package com.mojang.blaze3d.vertex;
public interface VertexConsumer {
    void vertex(float x, float y, float z, float r, float g, float b, float a,
                float u, float v, int overlay, int light, float nx, float ny, float nz);
}
