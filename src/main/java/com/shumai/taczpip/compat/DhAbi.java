package com.shumai.taczpip.compat;

import java.util.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/** Read bytecode without initializing DH or creating GL resources. */
public final class DhAbi {
    public enum Kind { NONE, LEGACY, MODERN }
    @FunctionalInterface public interface Classes { ClassNode read(String name) throws Exception; }
    public static final String ROOT = "com.seibel.distanthorizons.";
    public static final String META = ROOT + "common.render.openGl.GlDhMetaRenderer";
    public static final String POST = ROOT + "common.render.openGl.postProcessing.";
    public static final String PARAM = "Lcom/seibel/distanthorizons/core/render/RenderParams;";
    private DhAbi() {}

    public static Kind detect(Classes classes) {
        try {
            ClassNode lod = classes.read(ROOT + "core.render.renderer.LodRenderer");
            if (hasMethod(lod, "setupGLStateAndRenderObjects", null)
                    && hasMethod(lod, "cleanup", "()V")
                    && hasField(lod, "cachedWidth", "I") && hasLegacyHook(lod)) return Kind.LEGACY;
            for (String suffix : new String[]{"", "_forge"}) {
                ClassNode meta = classes.read(META + suffix);
                requireMethod(meta, "runRenderPassSetup", "(" + PARAM + ")V");
                requireMethod(meta, "<init>", "()V");
                requireField(meta, "framebuffer", "Lcom/seibel/distanthorizons/api/interfaces/override/rendering/IDhApiFramebuffer;");
                requireField(meta, "nullableColorTexture", "Lcom/seibel/distanthorizons/common/render/openGl/glObject/texture/GlDhColorTexture;");
                requireField(meta, "depthTexture", "Lcom/seibel/distanthorizons/common/render/openGl/glObject/texture/GlDhDepthTexture;");
                requireField(meta, "shaderProgramForThisFrame", "Lcom/seibel/distanthorizons/api/interfaces/override/rendering/IDhApiShaderProgram;");
                requireField(classes.read(ROOT + "common.wrappers.minecraft.MinecraftRenderWrapper" + suffix),
                        "finalLevelFrameBufferId", "I");
                requireFields(meta, "activeFramebufferId", "activeColorTextureId", "activeDepthTextureId",
                        "textureWidth", "textureHeight", "framebuffer", "nullableColorTexture",
                        "depthTexture", "usingMcFramebuffer", "renderObjectsCreated", "shaderProgramForThisFrame");
                for (String[] post : new String[][] {
                        {"ssao.GlDhSSAORenderer", "ssaoFramebuffer", "ssaoTexture"},
                        {"fog.GlDhFogRenderer", "fogFramebuffer", "fogTexture"},
                        {"fade.GlDhFarFadeRenderer", "fadeFramebuffer", "fadeTexture"},
                        {"fade.GlVanillaFadeRenderer", "fadeFramebuffer", "fadeTexture"}}) {
                    ClassNode renderer = classes.read(POST + post[0] + suffix);
                    requireFields(renderer, "init", "width", "height", post[1], post[2]);
                    requireMethod(renderer, "<init>", "()V");
                }
                requireMethod(classes.read(POST + "antialiasing.GlDhTaaRenderer" + suffix),
                        "render", "(" + PARAM + ")V");
                ClassNode taa = classes.read(POST + "antialiasing.GlDhTaaRenderer" + suffix);
                requireFields(taa, "init", "width", "height", "framebufferA", "framebufferB",
                        "colorTextureA", "colorTextureB", "textureAIsHistory");
                requireMethod(taa, "<init>", "()V");
                ClassNode history = classes.read(POST + "antialiasing.GlDhTaaShader" + suffix);
                requireMethod(history, "renderPrep", "(III)V");
                requireMethod(history, "savePostRenderUniformObjects", "(" + PARAM + ")V");
                for (String name : new String[]{"frameBuffer", "outputColorTextureId", "historyColorTextureId"})
                    requireField(history, name, "I");
                requireField(history, "previousDhProjMvmMatrix", "Lcom/seibel/distanthorizons/core/util/math/DhMat4f;");
                requireField(history, "previousCameraPos", "Lcom/seibel/distanthorizons/core/util/math/DhVec3d;");
                ClassNode terrain = classes.read(ROOT + "common.render.openGl.terrain.GlDhTerrainShaderProgram" + suffix);
                requireField(terrain, "frameIndexMod8", "I");
                requireField(terrain, "uFrameMod8", "I");
                requireMethod(terrain, "fillUniformData", "(Lcom/seibel/distanthorizons/api/methods/events/sharedParameterObjects/DhApiRenderParam;)V");
            }
            ClassNode api = classes.read(ROOT + "core.api.internal.ClientApi");
            requireField(api, "RENDER_STATE", "Lcom/seibel/distanthorizons/core/api/internal/rendering/DhRenderState;");
            requireField(api, "RENDER_PARAMS", PARAM);
            ClassNode proxy = classes.read(ROOT + "core.render.DhApiRenderProxy");
            requireField(proxy, "deferTransparentRendering", "Z");
            requireField(proxy, "activeOpenGlDhColorTextureId", "I");
            requireField(proxy, "activeOpenGlDhDepthTextureId", "I");
            for (String type : new String[]{"GlDhFramebuffer", "texture.GlDhColorTexture", "texture.GlDhDepthTexture"})
                requireMethod(classes.read(ROOT + "common.render.openGl.glObject." + type), "destroy", "()V");
            return Kind.MODERN;
        } catch (Exception | LinkageError unsupported) { return Kind.NONE; }
    }
    private static boolean hasLegacyHook(ClassNode node) {
        for (MethodNode method : node.methods) {
            if (!method.name.equals("setupGLStateAndRenderObjects")) continue;
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call
                        && call.owner.equals("com/seibel/distanthorizons/core/wrapperInterfaces/minecraft/IMinecraftRenderWrapper")
                        && call.name.equals("getTargetFrameBufferViewportWidth") && call.desc.equals("()I")) return true;
            }
        }
        return false;
    }
    private static void requireFields(ClassNode node, String... names) {
        Set<String> actual = new HashSet<>();
        for (FieldNode field : node.fields)
            if ((field.access & Opcodes.ACC_STATIC) == 0) actual.add(field.name);
        if (!actual.equals(new HashSet<>(Arrays.asList(names))))
            throw new IllegalArgumentException("Unknown DH state layout: " + node.name);
        for (FieldNode field : node.fields) {
            if ((field.access & Opcodes.ACC_STATIC) != 0) continue;
            if ((field.access & Opcodes.ACC_FINAL) != 0)
                throw new IllegalArgumentException("Immutable DH bank field: " + field.name);
            String name = field.name;
            if (name.equals("init") || name.equals("usingMcFramebuffer") || name.equals("renderObjectsCreated") || name.equals("textureAIsHistory"))
                requireField(node, name, "Z");
            else if (!name.equals("framebuffer") && !name.equals("nullableColorTexture")
                    && !name.equals("depthTexture") && !name.equals("shaderProgramForThisFrame"))
                requireField(node, name, "I");
        }
    }
    private static boolean hasField(ClassNode node, String name, String descriptor) {
        return node.fields.stream().anyMatch(f -> f.name.equals(name) && f.desc.equals(descriptor));
    }
    private static void requireField(ClassNode node, String name, String descriptor) {
        if (!hasField(node, name, descriptor)) throw new IllegalArgumentException(name);
    }
    private static boolean hasMethod(ClassNode node, String name, String descriptor) {
        return node.methods.stream().anyMatch(m -> m.name.equals(name) && (descriptor == null || m.desc.equals(descriptor)));
    }
    private static void requireMethod(ClassNode node, String name, String descriptor) {
        if (!hasMethod(node, name, descriptor)) throw new IllegalArgumentException(name);
    }
}
