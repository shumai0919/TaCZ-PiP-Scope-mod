package com.shumai.taczpip.client;

/** Implemented on TaCZ models; no pack IDs or names are required. */
public interface OpticalModel {
    boolean taczpip$hasOpticalLens();
    boolean taczpip$isOpticalView(int view);
    default boolean taczpip$nativeHud() { return false; }
}
