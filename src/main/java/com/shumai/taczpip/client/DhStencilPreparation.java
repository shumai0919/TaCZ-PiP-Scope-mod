package com.shumai.taczpip.client;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.ModList;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/** Allocate TaCZ's depth/stencil format before DH/Oculus start a world frame. */
public final class DhStencilPreparation {
    private DhStencilPreparation() {}

    public static void beforeFrame() {
        if (ModList.get() == null || !DhScopeContext.loaded() || !DhScopeContext.supported()) return;
        // Also run on the title screen: the first pipeline should be created
        // against the final format, even if a scoped gun is equipped on login.
        var target = Minecraft.getInstance().getMainRenderTarget();
        if (target == null || target.isStencilEnabled() || target.width <= 0 || target.height <= 0
                || target.getDepthTextureId() <= 0) return;
        int previousMain = target.frameBufferId;
        int read = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int draw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        try {
            // Forge recreates color/depth resources and notifies Oculus through
            // its RenderTarget version tracking. Never toggle DH or reload packs.
            target.enableStencil();
        } finally {
            // The old main FBO was deleted. Do not accidentally rebind that ID.
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,
                    read == previousMain ? target.frameBufferId : read);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,
                    draw == previousMain ? target.frameBufferId : draw);
        }
    }
}
