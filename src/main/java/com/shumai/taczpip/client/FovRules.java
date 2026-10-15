package com.shumai.taczpip.client;

/** The bypass intentionally does not depend on held/rendered item identity. */
public final class FovRules {
    private FovRules() {}
    public static boolean bypass(boolean enabled, boolean worldPass, boolean firstPerson, boolean localCamera) {
        return enabled && worldPass && firstPerson && localCamera;
    }
}
