package com.shumai.taczpip.client;
public class VertexReuseContext {
    public static final boolean PROFILING = true;
    public record Entry(Object owner, boolean replay) {}
    public static Entry entry;
    public static long frame, captures, hits, misses;
    static Entry current() { return entry; }
    static long frame() { return frame; }
}
