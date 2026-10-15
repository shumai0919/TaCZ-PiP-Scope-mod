package net.minecraft.client;
public class Minecraft {
    private static final Minecraft INSTANCE = new Minecraft();
    public static Minecraft getInstance() { return INSTANCE; }
    public final Target target = new Target();
    public Target getMainRenderTarget() { return target; }
    public static class Target { public int width = 960, height = 540; }
}
