package com.mojang.blaze3d.vertex;
import org.joml.*;
public class PoseStack {
    public record Pose(Matrix4f pose, Matrix3f normal) {}
}
