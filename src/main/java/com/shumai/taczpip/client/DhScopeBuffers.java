package com.shumai.taczpip.client;

import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;

/** DH 2.2.1-a owns one mutable buffer set per LodRenderer. Keep a second set for PiP. */
public final class DhScopeBuffers {
    private static final String[] NAMES = {"cachedWidth", "cachedHeight", "framebuffer",
            "nullableColorTexture", "depthTexture", "usingMcFrameBuffer"};
    private static final Map<Object, Object[]> BANKS = new IdentityHashMap<>();
    private static Field[] fields;
    private DhScopeBuffers() {}

    public static Runnable swap(Object renderer) {
        try {
            if (fields == null) {
                fields = new Field[NAMES.length];
                for (int i = 0; i < NAMES.length; i++) {
                    fields[i] = renderer.getClass().getDeclaredField(NAMES[i]);
                    fields[i].setAccessible(true);
                }
            }
            Object[] main = read(renderer);
            Object[] scope = BANKS.get(renderer);
            var target = Minecraft.getInstance().getMainRenderTarget();
            if (scope != null && (!scope[0].equals(target.width) || !scope[1].equals(target.height))) {
                release(renderer);
                scope = null;
            }
            if (scope == null) {
                Object framebuffer = Class.forName("com.seibel.distanthorizons.core.render.glObject.texture.DhFramebuffer")
                        .getConstructor().newInstance();
                scope = new Object[]{0, 0, framebuffer, null, null, false};
                BANKS.put(renderer, scope);
            }
            try { write(renderer, scope); }
            catch (ReflectiveOperationException error) { write(renderer, main); throw error; }
            // DH's normal setup creates textures if the bank is new. Restore even on render failure.
            return () -> {
                try { BANKS.put(renderer, read(renderer)); }
                catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
                finally {
                    try { write(renderer, main); }
                    catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
                }
            };
        } catch (ReflectiveOperationException e) { throw new IllegalStateException("DH PiP buffer isolation failed", e); }
    }

    private static Object[] read(Object renderer) throws IllegalAccessException {
        Object[] values = new Object[fields.length];
        for (int i = 0; i < fields.length; i++) values[i] = fields[i].get(renderer);
        return values;
    }
    private static void write(Object renderer, Object[] values) throws IllegalAccessException {
        for (int i = 0; i < fields.length; i++) fields[i].set(renderer, values[i]);
    }
    public static void release(Object renderer) {
        Object[] bank = BANKS.remove(renderer);
        if (bank == null) return;
        // Only owned PiP resources; never delete the renderer's main-view objects.
        RuntimeException failure = null;
        for (int i = 2; i <= 4; i++) if (bank[i] != null) {
            try { bank[i].getClass().getMethod("destroy").invoke(bank[i]); }
            catch (ReflectiveOperationException e) { failure = new IllegalStateException("DH PiP buffer cleanup", e); }
        }
        if (failure != null) throw failure;
    }
    public static void releaseAll() {
        for (Object renderer : BANKS.keySet().toArray()) release(renderer);
    }
}
