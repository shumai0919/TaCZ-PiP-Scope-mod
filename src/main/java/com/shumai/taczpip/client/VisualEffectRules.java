package com.shumai.taczpip.client;

/** Pure transition rules shared by normal and late shader composites. */
public final class VisualEffectRules {
    private VisualEffectRules() {}
    public static float idle(float ads,boolean enabled,float strength) {
        if(!enabled||!Float.isFinite(ads)||!Float.isFinite(strength)||strength<=0)return 0;
        float t=Math.max(0,Math.min(1,ads/.2f));
        return Math.min(1,strength)*(1-t*t*(3-2*t));
    }
}
