package com.shumai.taczpip.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Resource alpha only: never reads the GPU or changes texture filtering. Reload clears both caches. */
public final class ApertureAlpha {
    private static final Map<ResourceLocation, Optional<NativeImage>> IMAGES = new HashMap<>();
    private static final Map<Key, float[]> CONTOURS = new HashMap<>();
    private record Key(ResourceLocation texture, float u0, float v0, float u1, float v1) {}
    private ApertureAlpha() {}
    public static void clear() {
        IMAGES.values().forEach(image -> image.ifPresent(NativeImage::close));
        IMAGES.clear(); CONTOURS.clear();
    }
    public static float[] contour(ResourceLocation texture, float u0,float v0,float u1,float v1) {
        if(texture == null || u1<=u0 || v1<=v0) return new float[0];
        return CONTOURS.computeIfAbsent(new Key(texture,u0,v0,u1,v1), ApertureAlpha::read);
    }
    private static float[] read(Key k) {
        var image=IMAGES.computeIfAbsent(k.texture, location -> {
            try(var stream=Minecraft.getInstance().getResourceManager().open(location)) {
                return Optional.of(NativeImage.read(stream));
            } catch(Exception ignored) { return Optional.empty(); }
        }).orElse(null);
        if(image==null) return new float[0];
        int x0=Math.round(k.u0*image.getWidth()), y0=Math.round(k.v0*image.getHeight());
        int w=Math.round((k.u1-k.u0)*image.getWidth()), h=Math.round((k.v1-k.v0)*image.getHeight());
        if(w<2 || h<2 || w>512 || h>512 || x0<0 || y0<0 || x0+w>image.getWidth() || y0+h>image.getHeight())
            return new float[0];
        boolean[] opaque=new boolean[w*h];
        for(int y=0;y<h;y++) for(int x=0;x<w;x++)
            opaque[y*w+x]=((image.getPixelRGBA(x0+x,y0+y)>>>24)&255)>=128;
        return AlphaContour.trace(opaque,w,h);
    }
}
