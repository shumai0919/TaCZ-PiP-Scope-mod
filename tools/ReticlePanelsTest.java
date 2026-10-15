import com.shumai.taczpip.client.*;
import com.google.gson.*;
import java.util.zip.*;
import java.io.*;
import javax.imageio.ImageIO;

public class ReticlePanelsTest {
    static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
    static double[] quad(JsonObject cube){
        var o=cube.getAsJsonArray("origin");var s=cube.getAsJsonArray("size");
        double x=o.get(0).getAsDouble(),y=o.get(1).getAsDouble(),z=o.get(2).getAsDouble()+s.get(2).getAsDouble();
        double w=s.get(0).getAsDouble(),h=s.get(1).getAsDouble();
        return new double[]{x,y,z,x+w,y,z,x+w,y+h,z,x,y+h,z};
    }
    public static void main(String[] args)throws Exception {
        try(var zip=new ZipFile(args[0])){
            for(String name:new String[]{"sight_eotech_hhs8","sight_vortex_razorhdg3_1t10x24"}){
                JsonObject model;
                try(var reader=new InputStreamReader(zip.getInputStream(zip.getEntry("assets/atea/geo_models/attachment/"+name+"_geo.json")))){
                    model=JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
                }
                JsonArray cubes=null;
                for(var bone:model.getAsJsonArray("bones"))if(bone.getAsJsonObject().get("name").getAsString().equals("division"))
                    cubes=bone.getAsJsonObject().getAsJsonArray("cubes");
                check(cubes!=null,"division exists");
                double[] anchor=quad(cubes.get(0).getAsJsonObject());
                int panels=0;
                for(int i=1;i<cubes.size();i++)if(ReticlePanelRules.outside(anchor,quad(cubes.get(i).getAsJsonObject())))panels++;
                check(panels==4,"all four exterior panels: "+name);
                check(!ReticlePanelRules.outside(anchor,anchor),"main plane retained");
                java.awt.image.BufferedImage png;
                try(var stream=zip.getInputStream(zip.getEntry("assets/atea/textures/attachment/uv/"+name+".png"))){png=ImageIO.read(stream);}
                int size=png.getWidth()/2;int[] pixels=new int[size*size];
                for(int y=0;y<size;y++)for(int x=0;x<size;x++){
                    int c=png.getRGB(x,y);pixels[y*size+x]=(c&0xff00ff00)|((c>>>16)&255)|((c&255)<<16);
                }
                boolean[] removed=ReticleMaskRules.exterior(pixels,size,size);int changed=0,colored=0;
                if(args.length>1)try(var old=new java.net.URLClassLoader(new java.net.URL[]{new File(args[1]).toURI().toURL()},null)){
                    var method=old.loadClass("com.shumai.taczpip.client.ReticleMaskRules").getMethod("exterior",int[].class,int.class,int.class);
                    boolean[] before=(boolean[])method.invoke(null,pixels,size,size);int extra=0;
                    for(int i=0;i<before.length;i++)if(removed[i]&&!before[i])extra++;
                    System.out.println("Boundary pixels left by previous build and removed now: "+extra);
                }
                for(int i=0;i<pixels.length;i++) {
                    if(removed[i])changed++;
                    if((pixels[i]&0xffffff)!=0 && (pixels[i]>>>24)>0){colored++;check(!removed[i],"colored pixels retained");}
                }
                check(changed>100,"mask detected");
                float[] opening=ReticleOpening.trace(pixels,size,size);
                check(opening.length>8,"round mask must not become a four-corner quad: "+name);
                java.util.Map<String,Integer> residual=new java.util.TreeMap<>();
                for(int y=0;y<size;y++)for(int x=0;x<size;x++){
                    int i=y*size+x,c=pixels[i];
                    if(!removed[i] && (c>>>24)>0 && Math.hypot(x+.5-size*.5,y+.5-size*.5)>size*.35)
                        residual.merge(Integer.toHexString(c),1,Integer::sum);
                }
                System.out.println("Remaining outer colors: "+residual);
                System.out.println(name+": exterior panels="+panels+", mask pixels="+changed+", colored pixels retained="+colored);
            }
        }
        double[] a={0,0,0,1,0,0,1,1,0,0,1,0};
        check(!ReticlePanelRules.outside(a,new double[]{1,0,0,2,0,0,2,.02,0,1,.02,0}),"thin stroke retained");
        check(!ReticlePanelRules.outside(a,new double[]{0,0,1,1,0,1,1,1,1,0,1,1}),"noncoplanar retained");
        check(!ReticlePanelRules.outside(a,new double[]{.5,0,0,1.5,0,0,1.5,1,0,.5,1,0}),"overlap retained");
        // A staircase circle formerly left black pixels inside the transparent-pixel convex hull.
        int size=128;int[] pixels=new int[size*size];
        for(int y=0;y<size;y++)for(int x=0;x<size;x++)
            if(Math.hypot(x-63.5,y-63.5)>55)pixels[y*size+x]=0xff000000;
        boolean[] removed=ReticleMaskRules.exterior(pixels,size,size);
        for(int i=0;i<pixels.length;i++)check(pixels[i]==0||removed[i],"no circular rim residue");
        System.out.println("ReticlePanels: real pack panels, ring residue, narrow/overlapping/noncoplanar protection passed");
    }
}
