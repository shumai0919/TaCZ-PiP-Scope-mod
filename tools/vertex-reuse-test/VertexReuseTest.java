import com.shumai.taczpip.client.*;
import com.mojang.blaze3d.vertex.*;
import com.tacz.guns.client.model.bedrock.*;
import org.joml.*;
import java.util.*;

public class VertexReuseTest {
    static void check(boolean ok) { if (!ok) throw new AssertionError(); }
    public static void main(String[] args) {
        var cache = new CubeVertexReuse();
        Object owner = new Object();
        var random = new java.util.Random(123);
        var vertices = new BedrockVertex[4];
        for (int i=0;i<4;i++) vertices[i] = new BedrockVertex(new Vector3f(i*3, i-1, i*2), i/4f, 1-i/4f);
        var polygons = new BedrockPolygon[]{new BedrockPolygon(vertices, new Vector3f(0,0,1))};
        for (int frame=0;frame<120;frame++) {
            VertexReuseContext.frame=frame;
            var pose = new PoseStack.Pose(new Matrix4f().translation(random.nextFloat(),2,3)
                    .rotateXYZ(random.nextFloat(),random.nextFloat(),random.nextFloat()).scale(1,2,3),
                    new Matrix3f().rotateXYZ(random.nextFloat(),random.nextFloat(),random.nextFloat()).scale(2,1,3));
            List<float[]> output = new ArrayList<>();
            VertexConsumer consumer=(x,y,z,r,g,b,a,u,v,o,l,nx,ny,nz)->output.add(new float[]{x,y,z,r,g,b,a,u,v,o,l,nx,ny,nz});
            VertexReuseContext.entry=new VertexReuseContext.Entry(owner,false);
            check(cache.render(polygons,pose,consumer,40,20,1,1,1,1));
            check(output.size()==4);
            for (int i=0;i<4;i++) {
                var p = new Vector4f(vertices[i].pos.x()/16f,vertices[i].pos.y()/16f,vertices[i].pos.z()/16f,1).mul(pose.pose());
                var n = new Vector3f(polygons[0].normal).mul(pose.normal());
                check(Arrays.equals(output.get(i),new float[]{p.x(),p.y(),p.z(),1,1,1,1,vertices[i].u,vertices[i].v,20,40,n.x(),n.y(),n.z()}));
            }
            var expected = output.toArray(float[][]::new);
            output.clear();
            VertexReuseContext.entry=new VertexReuseContext.Entry(owner,true);
            check(cache.render(polygons,pose,consumer,40,20,1,1,1,1));
            check(Arrays.deepEquals(expected,output.toArray(float[][]::new)));
            output.clear();
            check(cache.render(polygons,pose,consumer,80,10,.5f,1,1,1));
            check(output.get(0)[3]==.5f && output.get(0)[9]==10 && output.get(0)[10]==80);
            var shifted = new PoseStack.Pose(new Matrix4f(pose.pose()).translate(.1f,0,0),pose.normal());
            check(!cache.render(polygons,shifted,consumer,40,20,1,1,1,1));
            var rotated = new PoseStack.Pose(pose.pose(),new Matrix3f(pose.normal()).rotateX(.1f));
            check(!cache.render(polygons,rotated,consumer,40,20,1,1,1,1));
            vertices[0].pos.x += 1;
            check(!cache.render(polygons,pose,consumer,40,20,1,1,1,1));
            vertices[0].pos.x -= 1;
            VertexReuseContext.frame++;
            check(!cache.render(polygons,pose,consumer,40,20,1,1,1,1));
            VertexReuseContext.frame--;
            VertexReuseContext.entry=new VertexReuseContext.Entry(new Object(),true);
            check(!cache.render(polygons,pose,consumer,40,20,1,1,1,1));
            VertexReuseContext.entry=null;
            check(!cache.render(polygons,pose,consumer,40,20,1,1,1,1));
        }
        check(VertexReuseContext.hits==240);
        System.out.println("PASS: 120 frames; exact transforms/replay; live light/color; pose, normal, geometry, owner and frame invalidation. Stubbed integration, not a game test.");
    }
}
