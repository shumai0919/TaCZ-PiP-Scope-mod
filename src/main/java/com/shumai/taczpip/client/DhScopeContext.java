package com.shumai.taczpip.client;

import net.minecraftforge.fml.ModList;
import java.lang.reflect.*;
import java.util.*;

/** DH 2.2.1-a: retain normal DH render hooks, isolate per-view shared state. */
public final class DhScopeContext implements AutoCloseable {
    private final List<Runnable> restore = new ArrayList<>();
    private static final Map<String, Field> fields = new HashMap<>();
    private static DhScopeContext active;
    private final Set<Object> swapped = Collections.newSetFromMap(new IdentityHashMap<>());
    public static void prepareBuffers(Object renderer) {
        if (active != null && active.swapped.add(renderer))
            active.restore.add(DhScopeBuffers.swap(renderer));
    }
    public static boolean loaded() { return ModList.get().isLoaded("distanthorizons"); }
    public static boolean supported() {
        return !loaded() || ModList.get().getModContainerById("distanthorizons")
                .map(m -> "2.2.1-a".equals(m.getModInfo().getVersion().toString())).orElse(false);
    }
    public static DhScopeContext enter() {
        if (!loaded()) return null;
        if (!supported()) throw new IllegalStateException("Unsupported DH version");
        DhScopeContext context = new DhScopeContext();
        try {
            String api = "com.seibel.distanthorizons.core.api.internal.ClientApi";
            context.capture(api, singleton(api), "renderingCancelledForThisFrame");
            String proxy = "com.seibel.distanthorizons.core.render.DhApiRenderProxy";
            context.capture(proxy, singleton(proxy), "deferTransparentRendering");
            String renderer = "com.seibel.distanthorizons.core.render.renderer.LodRenderer";
            for (String name : new String[]{"activeFramebufferId", "activeColorTextureId", "activeDepthTextureId"})
                context.capture(renderer, null, name);
            if (OculusCompat.shadersActive()) {
                context.capture("net.irisshaders.iris.compat.dh.LodRendererEvents", null, "atTranslucent");
                context.capture("net.irisshaders.iris.compat.dh.LodRendererEvents", null, "textureWidth");
                context.capture("net.irisshaders.iris.compat.dh.LodRendererEvents", null, "textureHeight");
                var target = net.minecraft.client.Minecraft.getInstance().getMainRenderTarget();
                fields.get("net.irisshaders.iris.compat.dh.LodRendererEvents:textureWidth").setInt(null, target.width);
                fields.get("net.irisshaders.iris.compat.dh.LodRendererEvents:textureHeight").setInt(null, target.height);
                // Oculus uses one global override registry. Temporarily remove
                // its main-view bindings so the scope pipeline can bind its own.
                Object injector = singleton("com.seibel.distanthorizons.coreapi.DependencyInjection.OverrideInjector");
                Class<?> override = Class.forName("com.seibel.distanthorizons.api.interfaces.override.IDhApiOverrideable");
                Method get = injector.getClass().getMethod("get", Class.class);
                Method bind = injector.getClass().getMethod("bind", Class.class, override);
                Method unbind = injector.getClass().getMethod("unbind", Class.class, override);
                for (String name : new String[]{"IDhApiFramebuffer", "IDhApiGenericObjectShaderProgram", "IDhApiShadowCullingFrustum"}) {
                    Class<?> type = Class.forName("com.seibel.distanthorizons.api.interfaces.override.rendering." + name);
                    Object previous = get.invoke(injector, type);
                    boolean detach = irisObject(previous);
                    if (detach) unbind.invoke(injector, type, previous);
                    context.restore.add(() -> {
                        try {
                            Object current = get.invoke(injector, type);
                            if (irisObject(current)) unbind.invoke(injector, type, current);
                            if (detach) bind.invoke(injector, type, previous);
                        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
                    });
                }
            }
            active = context;
            return context;
        } catch (ReflectiveOperationException | RuntimeException error) {
            try { context.close(); } catch (RuntimeException cleanup) { error.addSuppressed(cleanup); }
            throw new IllegalStateException("Cannot isolate DH scope state", error);
        }
    }
    private static boolean irisObject(Object value) {
        return value != null && value.getClass().getName().startsWith("net.irisshaders.");
    }
    private static Object singleton(String name) throws ReflectiveOperationException {
        return Class.forName(name).getField("INSTANCE").get(null);
    }
    private void capture(String type, Object object, String name) throws ReflectiveOperationException {
        String key = type + ":" + name;
        Field field = fields.get(key);
        if (field == null) {
            field = Class.forName(type).getDeclaredField(name);
            field.setAccessible(true); fields.put(key, field);
        }
        Field savedField = field;
        Object value = field.get(object);
        restore.add(() -> {
            try { savedField.set(object, value); }
            catch (IllegalAccessException e) { throw new IllegalStateException(e); }
        });
    }
    @Override public void close() {
        active = null;
        RuntimeException failure = null;
        for (int i = restore.size() - 1; i >= 0; i--) {
            try { restore.get(i).run(); }
            catch (RuntimeException e) { if (failure == null) failure = e; else failure.addSuppressed(e); }
        }
        restore.clear();
        if (failure != null) throw failure;
    }
}
