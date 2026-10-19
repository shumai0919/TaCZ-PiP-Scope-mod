package com.shumai.taczpip.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.*;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.model.bedrock.BedrockCube;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import java.util.*;

/** Texture copies used ONLY for optical reticles. Never changes the pack or gun-body texture. */
public final class ReticleTexture {
    private record Key(ResourceLocation texture,BedrockPart part) {}
    private static final Map<Key,Optional<ResourceLocation>> CACHE=new HashMap<>();
    private static final Map<Key,Map<BedrockPart,Set<BedrockCube>>> PANELS=new HashMap<>();
    private static final Map<Key,Map<List<Float>,float[]>> OPENINGS=new HashMap<>();
    private static List<Float> uvKey(float[] uv) { return List.of(uv[0],uv[1],uv[2],uv[3]); }
    public static float[] opening(ResourceLocation texture,BedrockPart part,float[] uv) {
        return OPENINGS.getOrDefault(new Key(texture,part),Map.of()).getOrDefault(uvKey(uv),new float[0]);
    }
    /** Shape only: deliberately does not inspect the division's position or depth. */
    public static float[] openingShape(ResourceLocation texture,BedrockPart part,ItemDisplayContext context) {
        get(texture,part,context);
        var shapes=OPENINGS.getOrDefault(new Key(texture,part),Map.of());
        // Ambiguous multiple openings must not be merged into a speculative shape.
        if(shapes.size()!=1) return new float[0];
        return NormalizedOpening.fit(shapes.values().iterator().next());
    }
    private ReticleTexture() {}
    public static void clear() {
        CACHE.values().forEach(v->v.ifPresent(id->Minecraft.getInstance().getTextureManager().release(id)));
        CACHE.clear();
        PANELS.clear();
        OPENINGS.clear();
    }
    public static void render(ResourceLocation original,BedrockPart part,Runnable draw) {
        var panels=PANELS.get(new Key(original,part));
        if(panels==null || panels.isEmpty()){draw.run();return;}
        var saved=new IdentityHashMap<BedrockPart,List<BedrockCube>>();
        try {
            panels.forEach((owner,cubes)->{
                saved.put(owner,new ArrayList<>(owner.cubes));
                owner.cubes.removeIf(cubes::contains);
            });
            draw.run();
        } finally {
            saved.forEach((owner,cubes)->{owner.cubes.clear();owner.cubes.addAll(cubes);});
        }
    }
    public static ResourceLocation get(ResourceLocation original,BedrockPart part,ItemDisplayContext context) {
        if(original==null) return null;
        return CACHE.computeIfAbsent(new Key(original,part), key->create(key,context)).orElse(null);
    }
    private static Optional<ResourceLocation> create(Key key,ItemDisplayContext context) {
        NativeImage image=null;
        try(var stream=Minecraft.getInstance().getResourceManager().open(key.texture)) {
            image=NativeImage.read(stream);
            if(image.getWidth()>4096 || image.getHeight()>4096) return Optional.empty();
            var collector=new Uvs();
            collector.collect(key.part);
            // Analyze all regions against the original, then apply changes once (overlapping UVs).
            var removals=new java.util.BitSet(image.getWidth()*image.getHeight());
            var anchors=new ArrayList<Face>();
            var openings=new HashMap<List<Float>,float[]>();
            for(var face:collector.faces) {
                var rect=face.uv;
                int x=Math.round(rect[0]*image.getWidth()),y=Math.round(rect[1]*image.getHeight());
                int w=Math.round((rect[2]-rect[0])*image.getWidth()),h=Math.round((rect[3]-rect[1])*image.getHeight());
                if(w<16||h<16||w>1024||h>1024||x<0||y<0||x+w>image.getWidth()||y+h>image.getHeight()) continue;
                int[] pixels=new int[w*h];
                for(int j=0;j<h;j++) for(int i=0;i<w;i++) pixels[j*w+i]=image.getPixelRGBA(x+i,y+j);
                boolean[] remove=ReticleMaskRules.exterior(pixels,w,h);
                boolean recognized=false;
                for(boolean value:remove) if(value){recognized=true;break;}
                if(recognized) {
                    anchors.add(face);
                    openings.put(uvKey(rect),ReticleOpening.trace(pixels,w,h));
                }
                for(int j=0;j<h;j++) for(int i=0;i<w;i++) if(remove[j*w+i]) removals.set((y+j)*image.getWidth()+x+i);
            }
            var panels=new IdentityHashMap<BedrockPart,Set<BedrockCube>>();
            for(var face:collector.faces) {
                // Only single rendered-face cubes can be pure mask panels.
                long count=collector.faces.stream().filter(other->other.cube==face.cube).count();
                if(count!=1 || !blackPatch(image,face.uv)) continue;
                for(var anchor:anchors) if(anchor.owner==face.owner &&
                        ReticlePanelRules.outside(anchor.xyz,face.xyz)) {
                    panels.computeIfAbsent(face.owner,p->Collections.newSetFromMap(new IdentityHashMap<>())).add(face.cube);
                    break;
                }
            }
            // Single-color UV patches may also draw geometric reticle strokes.
            // Never clear their shared texels merely because they lie in another face's mask.
            for(var face:collector.faces) {
                if(panels.getOrDefault(face.owner,Set.of()).contains(face.cube)) continue;
                var rect=face.uv;
                int x=Math.round(rect[0]*image.getWidth()),y=Math.round(rect[1]*image.getHeight());
                int w=Math.round((rect[2]-rect[0])*image.getWidth()),h=Math.round((rect[3]-rect[1])*image.getHeight());
                if(w<=0||h<=0||x<0||y<0||x+w>image.getWidth()||y+h>image.getHeight()||(w>=16&&h>=16)) continue;
                for(int j=0;j<h;j++) removals.clear((y+j)*image.getWidth()+x,(y+j)*image.getWidth()+x+w);
            }
            if(removals.isEmpty()) return Optional.empty();
            for(int bit=removals.nextSetBit(0);bit>=0;bit=removals.nextSetBit(bit+1))
                image.setPixelRGBA(bit%image.getWidth(),bit/image.getWidth(),0);
            var texture=new DynamicTexture(image);
            image=null; // owned by texture manager from here
            var id=Minecraft.getInstance().getTextureManager().register("tacz_pip_reticle",texture);
            PANELS.put(key,panels);
            OPENINGS.put(key,openings);
            return Optional.of(id);
        } catch(Exception ex) {
            com.mojang.logging.LogUtils.getLogger().warn("[TaCZ PiP Scope] Reticle separation skipped: {}",key.texture,ex);
            return Optional.empty();
        } finally {if(image!=null) image.close();}
    }
    private static boolean blackPatch(NativeImage image,float[] rect) {
        int x=Math.round(rect[0]*image.getWidth()),y=Math.round(rect[1]*image.getHeight());
        int w=Math.round((rect[2]-rect[0])*image.getWidth()),h=Math.round((rect[3]-rect[1])*image.getHeight());
        // Atlas resolution/patch size is not a geometric property of a mask panel.
        if(w<1||h<1||x<0||y<0||x+w>image.getWidth()||y+h>image.getHeight())return false;
        for(int j=0;j<h;j++)for(int i=0;i<w;i++){
            int p=image.getPixelRGBA(x+i,y+j);
            if((p>>>24)<250||(p&255)>24||((p>>>8)&255)>24||((p>>>16)&255)>24)return false;
        }
        return true;
    }
    private record Face(BedrockPart owner,BedrockCube cube,float[] uv,double[] xyz) {}
    private static final class Uvs implements VertexConsumer {
        final List<Face> faces=new ArrayList<>();
        BedrockPart owner; BedrockCube cube;
        double[] xyz=new double[12];
        void collect(BedrockPart part) {
            for(var c:part.cubes){
                owner=part;cube=c;count=0;u0=v0=Float.POSITIVE_INFINITY;u1=v1=Float.NEGATIVE_INFINITY;
                c.compile(new PoseStack().last(),this,0,0,1,1,1,1);
            }
            for(var child:part.children) collect(child);
        }
        float u0=Float.POSITIVE_INFINITY,v0=u0,u1=Float.NEGATIVE_INFINITY,v1=u1; int count;
        public VertexConsumer vertex(double x,double y,double z){xyz[count*3]=x;xyz[count*3+1]=y;xyz[count*3+2]=z;return this;}
        public VertexConsumer uv(float u,float v){u0=Math.min(u0,u);v0=Math.min(v0,v);u1=Math.max(u1,u);v1=Math.max(v1,v);return this;}
        public void endVertex(){if(++count==4){if(QuadGeometry.hasArea(xyz))faces.add(new Face(owner,cube,new float[]{u0,v0,u1,v1},xyz));xyz=new double[12];count=0;u0=v0=Float.POSITIVE_INFINITY;u1=v1=Float.NEGATIVE_INFINITY;}}
        public VertexConsumer color(int r,int g,int b,int a){return this;}
        public VertexConsumer overlayCoords(int u,int v){return this;}
        public VertexConsumer uv2(int u,int v){return this;}
        public VertexConsumer normal(float x,float y,float z){return this;}
        public void defaultColor(int r,int g,int b,int a){}
        public void unsetDefaultColor(){}
    }
}
