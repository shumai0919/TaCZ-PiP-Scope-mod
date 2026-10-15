package com.tacz.guns.client.model.bedrock;
import org.joml.Vector3f;
public class BedrockVertex {
    public final Vector3f pos;
    public final float u, v;
    public BedrockVertex(Vector3f pos, float u, float v) { this.pos=pos; this.u=u; this.v=v; }
}
