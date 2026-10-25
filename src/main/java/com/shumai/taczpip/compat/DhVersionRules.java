package com.shumai.taczpip.compat;

/** Release versions only. Future releases also need the structural ABI check. */
public final class DhVersionRules {
    private DhVersionRules() {}
    public static boolean legacy(String version) { return "2.2.1-a".equals(version); }
    public static boolean modern(String version) {
        if (version == null || !version.matches("\\d+\\.\\d+\\.\\d+")) return false;
        try {
            String[] parts = version.split("\\.");
            int major = Integer.parseInt(parts[0]), minor = Integer.parseInt(parts[1]);
            return major > 3 || major == 3 && minor >= 3;
        } catch (NumberFormatException invalid) { return false; }
    }
}
