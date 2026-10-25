import java.lang.reflect.*;
import java.util.*;

/** No Minecraft or GL context: checks the actual bank and snapshot implementation. */
public class DhModernStateTest {
    public static class Resource {
        boolean destroyed;
        public void destroy() { destroyed = true; }
    }
    public static class Renderer {
        int textureWidth, textureHeight;
        Resource framebuffer, nullableColorTexture, depthTexture;
        boolean renderObjectsCreated;
        public Renderer() {}
    }
    public static class Params {
        final Matrix apiCopy = new Matrix();
        int pass = 1;
    }
    public static class Matrix { float m00 = 1; }
    public static class TaaShader {
        public static final TaaShader INSTANCE = new TaaShader();
        int frameBuffer = 10, outputColorTextureId = 20, historyColorTextureId = 30;
        final Matrix previousDhProjMvmMatrix = new Matrix();
        final Camera previousCameraPos = new Camera();
        public void savePostRenderUniformObjects(Params params) {
            previousDhProjMvmMatrix.m00 = params.apiCopy.m00;
            previousCameraPos.x = params.pass;
        }
    }
    public static class Camera { double x = 4, y = 5, z = 6; }
    public static class TaaRenderer {
        boolean init, textureAIsHistory = true;
        int width = -1, height = -1, framebufferA = -1, framebufferB = -1, colorTextureA = -1, colorTextureB = -1;
    }
    static void check(boolean value) { if (!value) throw new AssertionError(); }
    public static void main(String[] args) throws Exception {
        Class<?> state = Class.forName("com.shumai.taczpip.client.DhModernState");
        Class<?> bankType = Class.forName(state.getName() + "$Bank");
        Constructor<?> constructor = bankType.getDeclaredConstructor(Object.class);
        constructor.setAccessible(true);
        Method enter = bankType.getDeclaredMethod("enter"), release = bankType.getDeclaredMethod("release");
        enter.setAccessible(true); release.setAccessible(true);
        Renderer renderer = new Renderer();
        renderer.textureWidth = 1920; renderer.textureHeight = 1080;
        renderer.framebuffer = new Resource(); renderer.depthTexture = new Resource();
        Resource mainF = renderer.framebuffer, mainD = renderer.depthTexture;
        Object bank = constructor.newInstance(renderer);
        Resource scopeF = null, scopeD = null;
        for (int i = 0; i < 120; i++) {
            Runnable restore = (Runnable) enter.invoke(bank);
            try {
                if (i == 0) {
                    check(renderer.framebuffer == null && renderer.textureWidth == 0);
                    renderer.framebuffer = scopeF = new Resource();
                    renderer.depthTexture = scopeD = new Resource();
                    renderer.textureWidth = 960;
                } else check(renderer.framebuffer == scopeF && renderer.depthTexture == scopeD && renderer.textureWidth == 960);
                if (i == 119) throw new IllegalStateException("simulated render failure");
            } catch (IllegalStateException expected) {
                check(i == 119);
            } finally { restore.run(); }
            check(renderer.framebuffer == mainF && renderer.depthTexture == mainD && renderer.textureWidth == 1920);
        }
        release.invoke(bank);
        check(scopeF.destroyed && scopeD.destroyed && !mainF.destroyed && !mainD.destroyed);
        Method snapshot = state.getDeclaredMethod("snapshot", Object.class, List.class, IdentityHashMap.class);
        snapshot.setAccessible(true);
        Params params = new Params();
        List<Runnable> restore = new ArrayList<>();
        snapshot.invoke(null, params, restore, new IdentityHashMap<>());
        params.pass = 2; params.apiCopy.m00 = 77;
        for (int i = restore.size() - 1; i >= 0; i--) restore.get(i).run();
        check(params.pass == 1 && params.apiCopy.m00 == 1);
        TaaRenderer taa = new TaaRenderer();
        taa.width = 1920;
        Object taaBank = constructor.newInstance(taa);
        for (int i = 0; i < 120; i++) {
            Runnable back = (Runnable) enter.invoke(taaBank);
            check(taa.textureAIsHistory == (i % 2 == 0));
            taa.textureAIsHistory = !taa.textureAIsHistory;
            taa.width = 960;
            back.run();
            check(taa.width == 1920 && taa.textureAIsHistory);
        }
        Class<?> temporal = Class.forName("com.shumai.taczpip.client.DhTemporalState");
        Method capture = temporal.getDeclaredMethod("capture", Object.class);
        capture.setAccessible(true);
        TaaShader shader = new TaaShader();
        @SuppressWarnings("unchecked") List<Runnable> saved = (List<Runnable>) capture.invoke(null, shader);
        shader.previousCameraPos.x = 99;
        shader.previousDhProjMvmMatrix.m00 = 9;
        shader.frameBuffer = 100;
        saved.forEach(Runnable::run);
        check(shader.previousCameraPos.x == 4 && shader.previousDhProjMvmMatrix.m00 == 1 && shader.frameBuffer == 10);
        Method prepare = temporal.getMethod("prepare", boolean.class, String.class, long.class);
        Method temporalEnter = temporal.getMethod("enter", List.class);
        Method jitter = temporal.getMethod("jitter", int.class);
        for (int i = 0; i < 16; i++) {
            prepare.invoke(null, true, "scope:75:4x", i * 33_000_000L);
            List<Runnable> cleanup = new ArrayList<>();
            temporalEnter.invoke(null, cleanup);
            check((Integer) jitter.invoke(null, 7) == (i + 1) % 8);
            check((Integer) jitter.invoke(null, 7) == (i + 1) % 8); // Multiple draws share one jitter.
            for (int j = cleanup.size() - 1; j >= 0; j--) cleanup.get(j).run();
        }
        prepare.invoke(null, false, "scope:75:4x", 600_000_000L);
        check((Integer) jitter.invoke(null, 3) == -1);
        prepare.invoke(null, true, "scope:75:8x", 630_000_000L);
        List<Runnable> cleanup = new ArrayList<>();
        temporalEnter.invoke(null, cleanup);
        check((Integer) jitter.invoke(null, 3) == 1);
        check((Integer) jitter.invoke(null, -1) == -1);
        cleanup.forEach(Runnable::run);
        Method before = temporal.getMethod("beforeRender", Object.class, Object.class);
        Method after = temporal.getMethod("afterRender", Object.class);
        temporal.getMethod("invalidate").invoke(null);
        Params first = new Params();
        first.apiCopy.m00 = 4; first.pass = 42;
        for (int i = 0; i < 3; i++) {
            prepare.invoke(null, true, i == 2 ? "new-scope" : "scope", 1_000_000_000L + i * 33_000_000L);
            List<Runnable> back = new ArrayList<>();
            temporalEnter.invoke(null, back);
            before.invoke(null, taa, first);
            // First/reset frames seed current matrices; subsequent frames recover PiP history.
            check(TaaShader.INSTANCE.previousDhProjMvmMatrix.m00 == (i == 1 ? 8 : 4));
            check(TaaShader.INSTANCE.previousCameraPos.x == (i == 1 ? 80 : 42));
            TaaShader.INSTANCE.previousDhProjMvmMatrix.m00 = 8;
            TaaShader.INSTANCE.previousCameraPos.x = 80;
            after.invoke(null, taa);
            for (int j = back.size() - 1; j >= 0; j--) back.get(j).run();
            check(TaaShader.INSTANCE.previousDhProjMvmMatrix.m00 == 1);
            check(TaaShader.INSTANCE.previousCameraPos.x == 4);
        }
        System.out.println("PASS: TAA ping-pong isolation, matrix/camera restoration, per-update jitter, OFF and zoom reset");
        System.out.println("PASS: modern 120-frame bank reuse, failure restoration, scope-only cleanup, final mutable parameter restoration");
    }
}
