package com.shumai.taczpip.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tacz.guns.client.model.bedrock.BedrockPolygon;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/** Only caches transformed positions/normals. UV, color, light and Oculus extensions stay live. */
public final class CubeVertexReuse {
    private final Matrix4f pose = new Matrix4f();
    private final Matrix3f normal = new Matrix3f();
    private final Vector4f positionScratch = new Vector4f();
    private final Vector3f normalScratch = new Vector3f();
    private float[] data = new float[0]; // raw position/normal + transformed position/normal per vertex
    private Object owner;
    private long frame = -1;

    public boolean render(BedrockPolygon[] polygons, PoseStack.Pose current, VertexConsumer consumer,
                          int light, int overlay, float r, float g, float b, float a) {
        var scope = VertexReuseContext.current();
        if (scope == null || scope.owner() == null) return false;
        int size = 0;
        for (var polygon : polygons) size += polygon.vertices.length * 12;
        if (scope.replay()) {
            if (frame != VertexReuseContext.frame() || owner != scope.owner() || data.length != size
                    || !pose.equals(current.pose()) || !normal.equals(current.normal()) || !sameGeometry(polygons)) {
                if (VertexReuseContext.PROFILING) VertexReuseContext.misses++;
                return false; // Let TaCZ/other mods execute the original compile method.
            }
            if (VertexReuseContext.PROFILING) VertexReuseContext.hits++;
        } else {
            frame = -1;
            if (data.length != size) data = new float[size];
            pose.set(current.pose()); normal.set(current.normal());
            int i = 0;
            for (var polygon : polygons) {
                normalScratch.set(polygon.normal).mul(normal);
                for (var vertex : polygon.vertices) {
                    data[i] = vertex.pos.x(); data[i+1] = vertex.pos.y(); data[i+2] = vertex.pos.z();
                    data[i+3] = polygon.normal.x(); data[i+4] = polygon.normal.y(); data[i+5] = polygon.normal.z();
                    positionScratch.set(data[i]/16f, data[i+1]/16f, data[i+2]/16f, 1f).mul(pose);
                    data[i+6] = positionScratch.x(); data[i+7] = positionScratch.y(); data[i+8] = positionScratch.z();
                    data[i+9] = normalScratch.x(); data[i+10] = normalScratch.y(); data[i+11] = normalScratch.z();
                    i += 12;
                }
            }
            owner = scope.owner(); frame = VertexReuseContext.frame();
            if (VertexReuseContext.PROFILING) VertexReuseContext.captures++;
        }
        int i = 0;
        for (var polygon : polygons) for (var vertex : polygon.vertices) {
            consumer.vertex(data[i+6], data[i+7], data[i+8], r, g, b, a, vertex.u, vertex.v,
                    overlay, light, data[i+9], data[i+10], data[i+11]);
            i += 12;
        }
        return true;
    }
    private boolean sameGeometry(BedrockPolygon[] polygons) {
        int i = 0;
        for (var polygon : polygons) for (var vertex : polygon.vertices) {
            if (data[i] != vertex.pos.x() || data[i+1] != vertex.pos.y() || data[i+2] != vertex.pos.z()
                    || data[i+3] != polygon.normal.x() || data[i+4] != polygon.normal.y() || data[i+5] != polygon.normal.z()) return false;
            i += 12;
        }
        return true;
    }
}
