package com.shumai.taczpip.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import java.util.List;

/** Measures pack-provided ocular geometry without GPU readbacks or texture masks. */
public final class EyeBoxMeasure implements VertexConsumer {
    private final Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix())
            .mul(RenderSystem.getModelViewStack().last().pose());
    private double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
    private double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
    private boolean invalid;
    private final java.util.ArrayList<double[]> points = new java.util.ArrayList<>();
    private net.minecraft.resources.ResourceLocation texture;
    private final Vector4f[] quad = new Vector4f[4];
    private final double[] viewQuad=new double[12];
    private final float[] uv = new float[8];
    private int vertexIndex;
    private boolean alphaShaped;
    private BedrockPart division;
    private float[] shapeOverride=new float[0];
    private Matrix4f ocularToView,viewToOcular;
    private final java.util.ArrayList<double[]> localVertices=new java.util.ArrayList<>();
    public static LensBounds shaped(PoseStack pose,ItemDisplayContext context,List<BedrockPart> path,
                                    net.minecraft.resources.ResourceLocation texture,float[] shape) {
        return measure(pose,context,path,texture,false,shape);
    }
    public static LensBounds division(PoseStack pose,ItemDisplayContext context,List<BedrockPart> path,
                                      net.minecraft.resources.ResourceLocation texture) {
        if(path.isEmpty()||texture==null) return LensBounds.INVALID;
        ReticleTexture.get(texture,path.get(path.size()-1),context);
        return measure(pose,context,path,texture,true);
    }
    public static float radius(PoseStack pose, ItemDisplayContext context, List<BedrockPart> path,
                               float progress, int width, int height) {
        return measure(pose, context, path).pupilRadius(progress, width, height);
    }
    public static LensBounds measure(PoseStack pose, ItemDisplayContext context, List<BedrockPart> path) {
        return measure(pose,context,path,null);
    }
    public static LensBounds measure(PoseStack pose, ItemDisplayContext context, List<BedrockPart> path,
                                    net.minecraft.resources.ResourceLocation texture) {
        return measure(pose,context,path,texture,false);
    }
    private static LensBounds measure(PoseStack pose,ItemDisplayContext context,List<BedrockPart> path,
                                    net.minecraft.resources.ResourceLocation texture,boolean division) {
        return measure(pose,context,path,texture,division,new float[0]);
    }
    private static LensBounds measure(PoseStack pose,ItemDisplayContext context,List<BedrockPart> path,
                                    net.minecraft.resources.ResourceLocation texture,boolean division,float[] shape) {
        if (path.isEmpty()) return LensBounds.INVALID;
        EyeBoxMeasure measure = new EyeBoxMeasure();
        measure.texture=texture;
        measure.shapeOverride=shape;
        pose.pushPose();
        BedrockPart part = path.get(path.size() - 1);
        if(division) measure.division=part;
        boolean visible = part.visible;
        try {
            for (int i = 0; i < path.size() - 1; i++) path.get(i).translateAndRotateAndScale(pose);
            pose.pushPose();
            try {
                part.translateAndRotateAndScale(pose);
                measure.ocularToView=new Matrix4f(pose.last().pose());
                measure.viewToOcular=new Matrix4f(measure.ocularToView).invert();
            } finally {pose.popPose();}
            part.visible = true;
            part.render(pose, context, measure, 0, 0);
        } finally { part.visible = visible; pose.popPose(); }
        if (measure.invalid) return LensBounds.INVALID;
        if(shape.length>=6) {
            double[][] fitted=ApertureFrame.fit(measure.localVertices,shape);
            if(fitted.length>=3) {
                var projected=new java.util.ArrayList<double[]>();
                var matrix=new Matrix4f(measure.projection).mul(measure.ocularToView);
                for(var p:fitted) {
                    var clip=matrix.transform(new Vector4f((float)p[0],(float)p[1],(float)p[2],1));
                    if(!Float.isFinite(clip.w)||clip.w<=0) return LensBounds.INVALID;
                    projected.add(new double[]{clip.x/clip.w*.5+.5,clip.y/clip.w*.5+.5});
                }
                measure.points.clear();measure.points.addAll(projected);measure.alphaShaped=true;
            }
        }
        // Use one aggregate local plane, never a screen-area winner that can jump
        // between side faces as the gun sways. Preserve signed pitch/yaw as well.
        var normal=new org.joml.Vector3f(0,0,1);
        var plane=ApertureFrame.fit(measure.localVertices,new float[]{0,0,1,0,1,1,0,1});
        if(plane.length==4) {
            var a=new org.joml.Vector3f((float)(plane[1][0]-plane[0][0]),(float)(plane[1][1]-plane[0][1]),(float)(plane[1][2]-plane[0][2]));
            var b=new org.joml.Vector3f((float)(plane[2][0]-plane[0][0]),(float)(plane[2][1]-plane[0][1]),(float)(plane[2][2]-plane[0][2]));
            normal.set(a.cross(b));
        }
        normal.mul(new org.joml.Matrix3f(measure.ocularToView).invert().transpose())
                .mul(RenderSystem.getModelViewStack().last().normal()).normalize();
        if(normal.z<0) normal.negate();
        float facing=Math.max(0,Math.min(1,normal.z));
        float tiltX=Math.max(-2,Math.min(2,normal.x/Math.max(.25f,normal.z)));
        float tiltY=Math.max(-2,Math.min(2,normal.y/Math.max(.25f,normal.z)));
        return new LensBounds((float)((measure.minX + measure.maxX) / 4 + .5),
                (float)((measure.minY + measure.maxY) / 4 + .5),
                (float)((measure.maxX - measure.minX) / 4),
                (float)((measure.maxY - measure.minY) / 4), PupilShape.hull(measure.points), measure.alphaShaped,facing,tiltX,tiltY,
                measure.eyeProjection(plane,normal));
    }
    private EyeProjection eyeProjection(double[][] plane,org.joml.Vector3f normal) {
        if(localVertices.isEmpty()) return EyeProjection.CLOSED;
        double[] min={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY};
        double[] max={Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};
        for(var v:localVertices) for(int i=0;i<3;i++) {min[i]=Math.min(min[i],v[i]);max[i]=Math.max(max[i],v[i]);}
        var view=new Matrix4f(RenderSystem.getModelViewStack().last().pose()).mul(ocularToView);
        var center=view.transformPosition(new org.joml.Vector3f((float)((min[0]+max[0])*.5),
                (float)((min[1]+max[1])*.5),(float)((min[2]+max[2])*.5)));
        double radius;
        if(plane.length==4) {
            var a=view.transformPosition(new org.joml.Vector3f((float)plane[0][0],(float)plane[0][1],(float)plane[0][2]));
            var b=view.transformPosition(new org.joml.Vector3f((float)plane[1][0],(float)plane[1][1],(float)plane[1][2]));
            var c=view.transformPosition(new org.joml.Vector3f((float)plane[3][0],(float)plane[3][1],(float)plane[3][2]));
            radius=Math.min(a.distance(b),a.distance(c))*.5;
        } else {
            // Non-planar oculars retain the stable local Z axis used above.
            var x=view.transformDirection(new org.joml.Vector3f((float)((max[0]-min[0])*.5),0,0));
            var y=view.transformDirection(new org.joml.Vector3f(0,(float)((max[1]-min[1])*.5),0));
            radius=Math.min(x.length(),y.length());
        }
        var pupil=OpticEyeGeometry.pupil(center.x,center.y,center.z,normal.x,normal.y,normal.z,radius);
        if(!pupil.valid()) return EyeProjection.CLOSED;
        var projection=RenderSystem.getProjectionMatrix();
        var c=projection.transform(new Vector4f(center,1));
        var p=projection.transform(new Vector4f((float)pupil.x(),(float)pupil.y(),(float)pupil.z(),1));
        if(!Float.isFinite(c.w)||!Float.isFinite(p.w)||c.w<=0||p.w<=0) return EyeProjection.CLOSED;
        var result=new EyeProjection(c.x/c.w*.5f+.5f,c.y/c.w*.5f+.5f,p.x/p.w*.5f+.5f,p.y/p.w*.5f+.5f,true);
        return result.finite()?result:EyeProjection.CLOSED;
    }
    @Override public VertexConsumer vertex(double x, double y, double z) {
        viewQuad[vertexIndex*3]=x;viewQuad[vertexIndex*3+1]=y;viewQuad[vertexIndex*3+2]=z;
        Vector4f clip = projection.transform(new Vector4f((float)x, (float)y, (float)z, 1));
        quad[vertexIndex]=clip;
        return this;
    }
    @Override public VertexConsumer color(int r, int g, int b, int a) { return this; }
    @Override public VertexConsumer uv(float u, float v) { uv[vertexIndex*2]=u; uv[vertexIndex*2+1]=v; return this; }
    @Override public VertexConsumer overlayCoords(int u, int v) { return this; }
    @Override public VertexConsumer uv2(int u, int v) { return this; }
    @Override public VertexConsumer normal(float x, float y, float z) {
        return this;
    }
    @Override public void endVertex() {
        if(++vertexIndex<4) return;
        vertexIndex=0;
        if(!QuadGeometry.hasArea(viewQuad)) return;
        for(var p:quad) if(p==null || !Float.isFinite(p.w) || p.w<=0) { invalid=true; return; }
        for(int i=0;i<4;i++) {
            var local=viewToOcular.transform(new Vector4f((float)viewQuad[i*3],(float)viewQuad[i*3+1],(float)viewQuad[i*3+2],1));
            localVertices.add(new double[]{local.x/local.w,local.y/local.w,local.z/local.w});
            if(division==null) {
                double nx=quad[i].x/quad[i].w,ny=quad[i].y/quad[i].w;
                minX=Math.min(minX,nx);maxX=Math.max(maxX,nx);
                minY=Math.min(minY,ny);maxY=Math.max(maxY,ny);
            }
        }
        float u0=Float.POSITIVE_INFINITY,v0=u0,u1=Float.NEGATIVE_INFINITY,v1=u1;
        for(int i=0;i<4;i++) { u0=Math.min(u0,uv[i*2]); u1=Math.max(u1,uv[i*2]);
            v0=Math.min(v0,uv[i*2+1]); v1=Math.max(v1,uv[i*2+1]); }
        float[] contour=division==null ? ApertureAlpha.contour(texture,u0,v0,u1,v1)
                : ReticleTexture.opening(texture,division,new float[]{u0,v0,u1,v1});
        int origin=-1,right=-1,down=-1;
        for(int i=0;i<4;i++) {
            if(uv[i*2]==u0 && uv[i*2+1]==v0) origin=i;
            if(uv[i*2]==u1 && uv[i*2+1]==v0) right=i;
            if(uv[i*2]==u0 && uv[i*2+1]==v1) down=i;
        }
        if(contour.length>0 && origin>=0 && right>=0 && down>=0) {
            alphaShaped=true;
            for(int i=0;i<contour.length;i+=2) {
                var p=new Vector4f(quad[origin]);
                p.add(new Vector4f(quad[right]).sub(quad[origin]).mul(contour[i]));
                p.add(new Vector4f(quad[down]).sub(quad[origin]).mul(contour[i+1]));
                add(p);
            }
        } else if(division==null) for(var p:quad) add(p);
    }
    private void add(Vector4f p) {
        if(division!=null) {
            minX=Math.min(minX,p.x/p.w);maxX=Math.max(maxX,p.x/p.w);
            minY=Math.min(minY,p.y/p.w);maxY=Math.max(maxY,p.y/p.w);
        }
        points.add(new double[]{p.x/p.w*.5+.5,p.y/p.w*.5+.5});
    }
    @Override public void defaultColor(int r, int g, int b, int a) {}
    @Override public void unsetDefaultColor() {}
}
