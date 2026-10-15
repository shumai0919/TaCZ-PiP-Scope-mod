package com.tacz.guns.client.model.bedrock;
import org.joml.Vector3f;
public class BedrockPolygon {
    public final BedrockVertex[] vertices;
    public final Vector3f normal;
    public BedrockPolygon(BedrockVertex[] vertices, Vector3f normal) { this.vertices=vertices; this.normal=normal; }
}
