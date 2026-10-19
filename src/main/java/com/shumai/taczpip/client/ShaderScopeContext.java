package com.shumai.taczpip.client;

import java.lang.reflect.*;
import java.util.*;

/** Optional Oculus 1.8.0 bridge. No Oculus types occur in linked signatures. */
public final class ShaderScopeContext implements AutoCloseable {
    private static Field managerField;
    private static Object owner, scopeManager;
    private static boolean active, routing, releasing;
    private static final Map<Object, Object> programs = new IdentityHashMap<>();
    private static final Map<Class<?>, Field[]> snapshotFields = new HashMap<>();
    private final Object mainManager;
    private final List<Snapshot> snapshots = new ArrayList<>();
    private final Map<Field, Object> levelPipelineFields = new HashMap<>();

    private ShaderScopeContext(Object manager) { mainManager = manager; }
    public static boolean active() { return active; }
    public static boolean shouldRoutePrograms() { return active && !routing; }

    public static ShaderScopeContext enter() {
        if (active) throw new IllegalStateException("Nested shader scope");
        ShaderScopeContext context = null;
        try {
            Class<?> iris = Class.forName("net.irisshaders.iris.Iris");
            Object main = iris.getMethod("getPipelineManager").invoke(null);
            if (managerField == null) managerField = field(iris, "pipelineManager");
            if (owner != main) { release(); owner = main; }
            context = new ShaderScopeContext(main);
            Object levelRenderer = net.minecraft.client.Minecraft.getInstance().levelRenderer;
            // Oculus stores the active pipeline on LevelRenderer as well as on
            // the manager. Its nested RETURN hook clears that field.
            for (Field f : levelRenderer.getClass().getDeclaredFields()) {
                if (f.getType().getName().equals("net.irisshaders.iris.pipeline.WorldRenderingPipeline")) {
                    f.setAccessible(true);
                    context.levelPipelineFields.put(f, f.get(levelRenderer));
                }
            }
            context.capture("net.irisshaders.iris.uniforms.CapturedRenderingState", "INSTANCE");
            context.capture("net.irisshaders.iris.uniforms.SystemTimeUniforms", "COUNTER");
            context.capture("net.irisshaders.iris.uniforms.SystemTimeUniforms", "TIMER");
            context.capture("net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings", "INSTANCE");
            if (scopeManager == null) {
                Object factory = field(main.getClass(), "pipelineFactory").get(main);
                scopeManager = main.getClass().getConstructor(java.util.function.Function.class).newInstance(factory);
            }
            managerField.set(null, scopeManager);
            active = true;
            return context;
        } catch (ReflectiveOperationException error) {
            if (context != null) context.close();
            throw new IllegalStateException("Cannot enter Oculus scope context", error);
        }
    }

    private void capture(String type, String singleton) throws ReflectiveOperationException {
        Object object = Class.forName(type).getField(singleton).get(null);
        snapshots.add(new Snapshot(object));
    }

    @Override public void close() {
        try {
            managerField.set(null, mainManager);
            for (Snapshot snapshot : snapshots) snapshot.restore();
            for (var entry : levelPipelineFields.entrySet())
                entry.getKey().set(net.minecraft.client.Minecraft.getInstance().levelRenderer, entry.getValue());
            // The scope's shadow/terrain setup may have changed visibility. The
            // following main pass must rebuild it for the original wide view.
            Class<?> renderer = Class.forName("me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer");
            Object instance = renderer.getMethod("instanceNullable").invoke(null);
            if (instance != null) renderer.getMethod("scheduleTerrainUpdate").invoke(instance);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Cannot restore Oculus main context", error);
        } finally { active = false; }
    }

    public static Object program(Object original, Object pass, Object vertex) {
        try {
            Object separate = programs.get(original);
            if (separate == null) {
                separate = original.getClass().getConstructor().newInstance();
                programs.put(original, separate);
            }
            routing = true;
            for (Method method : original.getClass().getMethods()) {
                if (method.getName().equals("getProgramOverride")) return method.invoke(separate, pass, vertex);
            }
            throw new NoSuchMethodException("getProgramOverride");
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Oculus scope terrain program failure", error);
        } finally { routing = false; }
    }

    public static void discardPrograms(Object original) {
        Object separate = programs.remove(original);
        if (separate != null) deletePrograms(separate);
    }

    private static void deletePrograms(Object object) {
        try { object.getClass().getMethod("deleteShaders").invoke(object); }
        catch (ReflectiveOperationException error) { throw new IllegalStateException("Oculus scope cleanup", error); }
    }

    public static void onManagerDestroyed(Object manager) {
        if (manager == owner && !releasing) release();
    }

    public static void release() {
        ScopeFramebufferPrototype.invalidateFrame();
        if (releasing) return;
        ShaderLensLate.release();
        releasing = true;
        try {
            List<Object> old = new ArrayList<>(programs.values());
            programs.clear();
            for (Object program : old) deletePrograms(program);
            Object scope = scopeManager;
            scopeManager = null;
            owner = null;
            if (scope != null) scope.getClass().getMethod("destroyPipeline").invoke(scope);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Oculus scope pipeline cleanup", error);
        } finally { releasing = false; }
    }

    private static Field field(Class<?> type, String name) throws NoSuchFieldException {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static final class Snapshot {
        private final Object object;
        private final Field[] fields;
        private final Object[] values;
        Snapshot(Object object) throws IllegalAccessException {
            this.object = object;
            fields = snapshotFields.computeIfAbsent(object.getClass(), type -> {
                Field[] found = Arrays.stream(type.getDeclaredFields())
                        .filter(f -> !Modifier.isStatic(f.getModifiers()) && !Modifier.isFinal(f.getModifiers()))
                        .toArray(Field[]::new);
                for (Field f : found) f.setAccessible(true);
                return found;
            });
            values = new Object[fields.length];
            for (int i = 0; i < fields.length; i++) values[i] = fields[i].get(object);
        }
        void restore() throws IllegalAccessException {
            for (int i = 0; i < fields.length; i++) fields[i].set(object, values[i]);
        }
    }
}
