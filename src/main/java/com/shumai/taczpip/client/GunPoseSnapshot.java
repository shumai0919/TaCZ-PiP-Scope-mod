package com.shumai.taczpip.client;

import com.tacz.guns.client.model.bedrock.BedrockPart;
import org.joml.Quaternionf;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

/** Frame-local bone state for the shader late pass, not an animation update. */
public final class GunPoseSnapshot {
    private final List<State> states = new ArrayList<>();

    public GunPoseSnapshot(Iterable<BedrockPart> parts) {
        var seen = new IdentityHashMap<BedrockPart, Boolean>();
        for (var part : parts) capture(part, seen);
    }
    private void capture(BedrockPart part, IdentityHashMap<BedrockPart, Boolean> seen) {
        if (part == null || seen.put(part, Boolean.TRUE) != null) return;
        states.add(new State(part));
        for (var child : part.children) capture(child, seen);
    }
    public void run(Runnable draw) {
        // The shared TaCZ model may already have been reset or used by another pass.
        // Restore that current state afterwards, including when rendering throws.
        var previous = new ArrayList<State>(states.size());
        for (var state : states) previous.add(new State(state.part));
        try {
            for (var state : states) state.apply();
            draw.run();
        } finally {
            for (var state : previous) state.apply();
        }
    }
    private static final class State {
        private final BedrockPart part;
        private final float x, y, z, xr, yr, zr, ox, oy, oz, sx, sy, sz;
        private final Quaternionf rotation;
        private final boolean visible;
        State(BedrockPart p) {
            part = p;
            x=p.x; y=p.y; z=p.z; xr=p.xRot; yr=p.yRot; zr=p.zRot;
            ox=p.offsetX; oy=p.offsetY; oz=p.offsetZ;
            sx=p.xScale; sy=p.yScale; sz=p.zScale;
            rotation = new Quaternionf(p.additionalQuaternion);
            visible=p.visible;
        }
        void apply() {
            part.x=x; part.y=y; part.z=z; part.xRot=xr; part.yRot=yr; part.zRot=zr;
            part.offsetX=ox; part.offsetY=oy; part.offsetZ=oz;
            part.xScale=sx; part.yScale=sy; part.zScale=sz;
            part.additionalQuaternion.set(rotation);
            part.visible=visible;
        }
    }
}
