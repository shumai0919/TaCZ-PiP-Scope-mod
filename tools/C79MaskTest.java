import com.shumai.taczpip.client.*;
import javax.imageio.ImageIO;
import java.io.File;
public class C79MaskTest {
 public static void main(String[] args)throws Exception {
  var png=ImageIO.read(new File(args[0]));int n=256;int[] p=new int[n*n];
  for(int y=0;y<n;y++)for(int x=0;x<n;x++){int c=png.getRGB(x,y+174);p[y*n+x]=(c&0xff00ff00)|((c>>>16)&255)|((c&255)<<16);}
  boolean[] removed=ReticleMaskRules.exterior(p,n,n);int left=0,changed=0;java.util.Map<String,Integer> colors=new java.util.TreeMap<>();
  for(int i=0;i<p.length;i++){if(removed[i])changed++;else if((p[i]>>>24)>0){left++;colors.merge(Integer.toHexString(p[i]),1,Integer::sum);}}
  System.out.println("C79 mask removed="+changed+" remaining="+left+" colors="+colors);
  if(left!=0||changed==0)throw new AssertionError("mask must be fully removed");
  if(args.length>1)try(var zip=new java.util.zip.ZipFile(args[1])) {
   try(var reader=new java.io.InputStreamReader(zip.getInputStream(zip.getEntry("assets/rfp/geo_models/attachment/elcan_c79_1.json")))) {
    var model=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
    for(var b:model.getAsJsonArray("bones"))if(b.getAsJsonObject().get("name").getAsString().equals("division")) {
     var cubes=b.getAsJsonObject().getAsJsonArray("cubes");var anchor=ReticlePanelsTest.quad(cubes.get(0).getAsJsonObject());int panels=0;
     for(int j=1;j<cubes.size();j++) {
      var cube=cubes.get(j).getAsJsonObject();
      if(ReticlePanelRules.outside(anchor,ReticlePanelsTest.quad(cube))) {
       panels++;
       var face=cube.getAsJsonObject("uv").getAsJsonObject("south");var uv=face.getAsJsonArray("uv");var size=face.getAsJsonArray("uv_size");
       int x0=Math.round(uv.get(0).getAsFloat()*4),y0=Math.round(uv.get(1).getAsFloat()*4),w=Math.round(size.get(0).getAsFloat()*4),h=Math.round(size.get(1).getAsFloat()*4);
       for(int y=y0;y<y0+h;y++)for(int x=x0;x<x0+w;x++)if(png.getRGB(x,y)!=0xff000000)throw new AssertionError("panel not black");
      }
     }
     if(panels!=4)throw new AssertionError("four outer panels; internal reticle retained: "+panels);
     System.out.println("C79 real model: four broad overlapping black panels recognized; two interior faces retained");
    }
   }
  }
 }
}
