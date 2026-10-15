package com.shumai.taczpip.client;

import net.minecraft.client.renderer.ShaderInstance;

final class PupilUniforms {
    private PupilUniforms() {}
    static void set(ShaderInstance shader, LensBounds bounds, float progress, int width, int height) {
        float[] planes=PupilShape.planes(bounds,progress,width,height);
        set(shader,planes);
    }
    static void set(ShaderInstance shader,float[] planes) {
        shader.safeGetUniform("PupilEdgeCount").set(planes.length/4f);
        for(int i=0; i<planes.length/4; i++) shader.safeGetUniform("PupilEdges["+i+"]")
                .set(planes[i*4],planes[i*4+1],planes[i*4+2],0f);
    }
}
