import com.shumai.taczpip.client.ApertureFrame;
import com.google.gson.*;
import java.util.*;
import java.nio.file.*;
import java.util.zip.*;
import java.io.*;

public final class ApertureFrameTest {
    static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);}
    static double at(JsonArray a,int i){return a==null?0:a.get(i).getAsDouble();}
    static void test(String name,String json) {
        var model=JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        var vertices=new ArrayList<double[]>();int count=0;
        for(var entry:model.getAsJsonArray("bones")) {
            var bone=entry.getAsJsonObject();if(!bone.get("name").getAsString().equals("ocular"))continue;
            for(var cube:bone.getAsJsonArray("cubes")) {
                count++;var c=cube.getAsJsonObject();var o=c.getAsJsonArray("origin");var s=c.getAsJsonArray("size");
                var p=c.getAsJsonArray("pivot");var r=c.getAsJsonArray("rotation");
                check(at(r,0)==0&&at(r,1)==0,"fixture only rotates Z");
                double angle=Math.toRadians(at(r,2));
                check(Math.min(at(s,0),at(s,1))/Math.max(at(s,0),at(s,1))<.21,"old selected face is narrow");
                for(int i=0;i<4;i++) {
                    double x=at(o,0)+((i==1||i==2)?at(s,0):0)-at(p,0);
                    double y=at(o,1)+(i>=2?at(s,1):0)-at(p,1);
                    vertices.add(new double[]{x*Math.cos(angle)-y*Math.sin(angle)+at(p,0),
                        x*Math.sin(angle)+y*Math.cos(angle)+at(p,1),at(o,2)});
                }
            }
        }
        check(count==8,"eight-strip fixture");
        float[] circle=new float[32];
        for(int i=0;i<16;i++){circle[i*2]=(float)(.5+.5*Math.cos(i*Math.PI/8));circle[i*2+1]=(float)(.5+.5*Math.sin(i*Math.PI/8));}
        var a=ApertureFrame.fit(vertices,circle);
        check(a.length==16,"whole opening shape");
        double rx=Math.abs(a[0][0]-a[8][0]),ry=Math.abs(a[4][1]-a[12][1]);
        check(rx/ry>.98&&rx/ry<1.02,"must not squash circle into strip: "+rx/ry);
        Collections.reverse(vertices);
        var b=ApertureFrame.fit(vertices,circle);
        for(int i=0;i<a.length;i++)for(int j=0;j<3;j++)check(a[i][j]==b[i][j],"face order independence");
        System.out.println(name+": old strip aspect <0.21; aggregate aspect="+rx/ry+"; ordering stable");
    }
    public static void main(String[] args)throws Exception {
        test("Vuduu",Files.readString(Path.of(args[0])));
        try(var zip=new ZipFile(args[1])) {
            for(String n:new String[]{"hmw_20_geo","hmw_20_2_geo"})try(var in=zip.getInputStream(zip.getEntry("assets/mw3/geo_models/attachment/"+n+".json"))) {
                test(n,new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));
            }
        }
        float[] square={0,0,1,0,1,1,0,1};
        var q=ApertureFrame.fit(List.of(new double[]{-2,-1,0},new double[]{2,1,0}),square);
        check(q.length==4&&q[0][0]==-2&&q[2][0]==2,"square stays square");
        check(ApertureFrame.fit(List.of(new double[]{0,0,0},new double[]{1,1,1}),square).length==0,"volume rejected");
    }
}
