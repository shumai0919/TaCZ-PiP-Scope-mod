package com.shumai.taczpip.client;

import com.shumai.taczpip.compat.DhAbi;
import java.lang.reflect.*;
import java.util.*;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/** DH 3.3 OpenGL renderers: retain independent owned buffers, not main-view resources. */
public final class DhModernState {
    private static final Map<Object, Bank> banks = new IdentityHashMap<>();
    private DhModernState() {}

    public static void enter(List<Runnable> restore) throws ReflectiveOperationException {
        DhTemporalState.enter(restore);
        Class<?> api = Class.forName(DhAbi.ROOT + "core.api.internal.ClientApi");
        snapshot(field(api, "RENDER_STATE").get(null), restore, new IdentityHashMap<>());
        snapshot(field(api, "RENDER_PARAMS").get(null), restore, new IdentityHashMap<>());
        Class<?> proxy = Class.forName(DhAbi.ROOT + "core.render.DhApiRenderProxy");
        for (Field f : proxy.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) && !Modifier.isFinal(f.getModifiers())
                    && (f.getName().startsWith("active") || f.getName().equals("getDhBlockRatioAtlasTextureGlId"))) {
                f.setAccessible(true);
                save(f, null, restore);
            }
        }
        Object injector = Class.forName(DhAbi.ROOT + "core.dependencyInjection.SingletonInjector")
                .getField("INSTANCE").get(null);
        Method get = injector.getClass().getMethod("get", Class.class);
        Object wrapper = get.invoke(injector, Class.forName(DhAbi.ROOT + "core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper"));
        save(field(wrapper.getClass(), "finalLevelFrameBufferId"), wrapper, restore);
        // Use the actually registered renderer (Forge transforms/aliases class names).
        for (String name : new String[]{"IDhMetaRenderer", "IDhSsaoRenderer", "IDhFogRenderer",
                "IDhFarFadeRenderer", "IDhVanillaFadeRenderer", "IDhAntiAliasRenderer"}) {
            Object renderer = get.invoke(injector,
                    Class.forName(DhAbi.ROOT + "core.wrapperInterfaces.render.renderPass." + name));
            if (renderer == null || !renderer.getClass().getName().startsWith(DhAbi.ROOT + "common.render.openGl."))
                throw new IllegalStateException("Unsupported DH renderer: " + name);
            Bank bank = banks.get(renderer);
            if (bank == null) {
                bank = new Bank(renderer);
                banks.put(renderer, bank);
            }
            restore.add(bank.enter());
        }
    }

    private static Field field(Class<?> type, String name) throws NoSuchFieldException {
        Field f = type.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    private static void save(Field field, Object owner, List<Runnable> restore) throws IllegalAccessException {
        Object value = field.get(owner);
        restore.add(() -> {
            try { field.set(owner, value); }
            catch (IllegalAccessException e) { throw new IllegalStateException(e); }
        });
    }

    // Matrices in RenderParams are final mutable objects; shallow field restoration
    // would leave the main view with the scope's projection after the nested render.
    private static void snapshot(Object owner, List<Runnable> restore, IdentityHashMap<Object, Boolean> seen)
            throws IllegalAccessException {
        if (owner == null || seen.put(owner, true) != null) return;
        for (Class<?> type = owner.getClass(); type != Object.class; type = type.getSuperclass()) {
            for (Field f : type.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                f.setAccessible(true);
                Object value = f.get(owner);
                if (!Modifier.isFinal(f.getModifiers())) save(f, owner, restore);
                if (value != null && (value.getClass().getName().startsWith(DhAbi.ROOT + "api.objects.math.")
                        || value.getClass().getName().startsWith(DhAbi.ROOT + "core.util.math.")
                        || f.getName().equals("apiCopy"))) snapshot(value, restore, seen);
            }
        }
    }

    private static final class Bank {
        final Object renderer;
        final Field[] fields;
        Object[] scope;
        boolean active;
        Bank(Object renderer) throws ReflectiveOperationException {
            this.renderer = renderer;
            fields = Arrays.stream(renderer.getClass().getDeclaredFields())
                    .filter(f -> !Modifier.isStatic(f.getModifiers())).toArray(Field[]::new);
            for (Field f : fields) {
                if (Modifier.isFinal(f.getModifiers())) throw new IllegalStateException("Unknown immutable DH state");
                f.setAccessible(true);
            }
            Constructor<?> constructor = renderer.getClass().getDeclaredConstructor();
            constructor.setAccessible(true);
            // Constructors only initialize fields; GL allocation happens on first render.
            scope = read(constructor.newInstance());
        }
        Object[] read(Object object) throws IllegalAccessException {
            Object[] values = new Object[fields.length];
            for (int i = 0; i < fields.length; i++) values[i] = fields[i].get(object);
            return values;
        }
        void write(Object[] values) throws IllegalAccessException {
            for (int i = 0; i < fields.length; i++) fields[i].set(renderer, values[i]);
        }
        Runnable enter() throws IllegalAccessException {
            if (active) throw new IllegalStateException("Nested DH buffer bank");
            Object[] main = read(renderer);
            try { write(scope); }
            catch (IllegalAccessException | RuntimeException e) { write(main); throw e; }
            active = true;
            return () -> {
                try { scope = read(renderer); }
                catch (IllegalAccessException e) { throw new IllegalStateException(e); }
                finally {
                    try { write(main); }
                    catch (IllegalAccessException e) { throw new IllegalStateException(e); }
                    finally { active = false; }
                }
            };
        }
        void release() {
            if (active) throw new IllegalStateException("Cannot release active DH scope bank");
            RuntimeException failure = null;
            for (int i = 0; i < fields.length; i++) {
                String name = fields[i].getName();
                Object value = scope[i];
                try {
                    if (name.equals("framebuffer") || name.equals("nullableColorTexture") || name.equals("depthTexture")) {
                        if (value != null) value.getClass().getMethod("destroy").invoke(value);
                    } else if ((name.endsWith("Framebuffer") || name.equals("framebufferA") || name.equals("framebufferB")) && value instanceof Integer id && id > 0) {
                        GL30.glDeleteFramebuffers(id);
                    } else if ((name.endsWith("Texture") || name.equals("colorTextureA") || name.equals("colorTextureB")) && value instanceof Integer id && id > 0) {
                        GL11.glDeleteTextures(id);
                    }
                } catch (ReflectiveOperationException | RuntimeException e) {
                    if (failure == null) failure = new IllegalStateException("DH scope resource cleanup", e);
                    else failure.addSuppressed(e);
                }
            }
            // Do not call renderer.free(): it also frees shared singleton shaders.
            if (failure != null) throw failure;
        }
    }

    public static void releaseAll() {
        DhTemporalState.invalidate();
        RuntimeException failure = null;
        for (Bank bank : banks.values()) {
            try { bank.release(); }
            catch (RuntimeException e) {
                if (failure == null) failure = e; else failure.addSuppressed(e);
            }
        }
        banks.clear();
        if (failure != null) throw failure;
    }
}
