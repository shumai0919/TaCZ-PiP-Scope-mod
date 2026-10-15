package com.seibel.distanthorizons.core.render.glObject.texture;
public class DhFramebuffer {
    public boolean destroyed;
    public void destroy() { if (destroyed) throw new AssertionError("double delete"); destroyed = true; }
}
