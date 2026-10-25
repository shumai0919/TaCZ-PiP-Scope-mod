package com.shumai.taczpip.client;

import java.lang.reflect.*;
import java.util.*;

/** Render-thread-only temporal state. No DH classes are linked when DH is absent. */
public final class DhTemporalState {
    private static final Map<Object, List<Runnable>> histories = new IdentityHashMap<>();
    private static final Set<Object> valid = Collections.newSetFromMap(new IdentityHashMap<>());
    private static List<Runnable> restores;
    private static boolean enabled;
    private static int frame;
    private static String context;
    private static long lastUpdate;

    private DhTemporalState() {}

    public static void prepare(boolean on, String key, long now) {
        if (on != enabled || !Objects.equals(context, key) || now - lastUpdate > 500_000_000L) {
            valid.clear();
            histories.clear();
            frame = 0;
        }
        enabled = on;
        context = key;
        lastUpdate = now;
    }

    public static void invalidate() {
        valid.clear();
        histories.clear();
        context = null;
        frame = 0;
    }

    public static void enter(List<Runnable> restore) {
        restores = restore;
        frame = (frame + 1) % 8;
        restore.add(() -> restores = null);
    }

    public static boolean enabled() { return enabled; }
    public static int jitter(int dhIndex) {
        if (dhIndex < 0) valid.clear(); // DH AA was disabled; do not reuse stale history on re-enable.
        return enabled && dhIndex >= 0 ? frame : -1;
    }

    // Register with the outer finally, so exceptions also restore the main counter.
    public static void restoreAfterPass(Runnable action) {
        if (restores == null) throw new IllegalStateException("Missing DH temporal scope");
        restores.add(action);
    }

    public static void beforeRender(Object renderer, Object params) {
        try {
            Class<?> shaderType = Class.forName(renderer.getClass().getName().replace("TaaRenderer", "TaaShader"));
            Object shader = shaderType.getField("INSTANCE").get(null);
            List<Runnable> main = capture(shader);
            restoreAfterPass(() -> {
                try { histories.put(shader, capture(shader)); }
                finally { apply(main); }
            });
            List<Runnable> scope = histories.get(shader);
            if (scope != null) apply(scope);
            if (!valid.contains(shader)) {
                // Seed camera/matrices with this frame, not the main view or origin.
                shaderType.getMethod("savePostRenderUniformObjects", params.getClass()).invoke(shader, params);
            }
        } catch (ReflectiveOperationException e) { throw new IllegalStateException("DH PiP TAA history", e); }
    }

    public static void seedHistory(Object shader) {
        if (valid.contains(shader)) return;
        try {
            String suffix = shader.getClass().getName().endsWith("_forge") ? "_forge" : "";
            Class<?> meta = Class.forName("com.seibel.distanthorizons.common.render.openGl.GlDhMetaRenderer" + suffix);
            int current = (Integer) meta.getMethod("getActiveColorTextureId").invoke(meta.getField("INSTANCE").get(null));
            field(shader.getClass(), "historyColorTextureId").setInt(shader, current);
        } catch (ReflectiveOperationException e) { throw new IllegalStateException("DH PiP TAA seed", e); }
    }

    public static void afterRender(Object renderer) {
        try {
            Class<?> type = Class.forName(renderer.getClass().getName().replace("TaaRenderer", "TaaShader"));
            valid.add(type.getField("INSTANCE").get(null));
        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }

    private static List<Runnable> capture(Object shader) {
        try {
            List<Runnable> result = new ArrayList<>();
            for (String name : new String[]{"frameBuffer", "outputColorTextureId", "historyColorTextureId"})
                save(field(shader.getClass(), name), shader, result);
            for (String name : new String[]{"previousDhProjMvmMatrix", "previousCameraPos"}) {
                Object value = field(shader.getClass(), name).get(shader);
                for (Class<?> c = value.getClass(); c != Object.class; c = c.getSuperclass())
                    for (Field f : c.getDeclaredFields())
                        if (!Modifier.isStatic(f.getModifiers()) && !Modifier.isFinal(f.getModifiers())) {
                            if (!f.getType().isPrimitive()) throw new IllegalStateException("Unknown DH temporal math field");
                            f.setAccessible(true);
                            save(f, value, result);
                        }
            }
            return result;
        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }

    private static Field field(Class<?> c, String name) throws NoSuchFieldException {
        Field f = c.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }
    private static void save(Field f, Object owner, List<Runnable> result) throws IllegalAccessException {
        Object value = f.get(owner);
        result.add(() -> {
            try { f.set(owner, value); }
            catch (IllegalAccessException e) { throw new IllegalStateException(e); }
        });
    }
    private static void apply(List<Runnable> values) { values.forEach(Runnable::run); }
}
