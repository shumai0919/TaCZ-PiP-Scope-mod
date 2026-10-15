package com.shumai.taczpip.client;

import com.tacz.guns.client.model.bedrock.BedrockPart;
import java.util.ArrayList;
import java.util.List;

/** TaCZ packs can include a geometric eye-box background inside the reticle. */
public final class ReticleBackgroundFilter {
    private ReticleBackgroundFilter() {}
    public static void render(BedrockPart division, Runnable draw) {
        List<BedrockPart> hidden = new ArrayList<>();
        try {
            hideBackgroundChildren(division, hidden);
            draw.run();
        } finally {
            for (BedrockPart part : hidden) part.visible = true;
        }
    }
    private static void hideBackgroundChildren(BedrockPart parent, List<BedrockPart> hidden) {
        for (BedrockPart child : parent.children) {
            // Deliberately narrow: verified pack convention, not arbitrary black
            // geometry or illuminated-only filtering (which would erase reticles).
            if ("bg".equals(child.name)) {
                if (child.visible) {
                    hidden.add(child);
                    child.visible = false;
                }
            } else {
                hideBackgroundChildren(child, hidden);
            }
        }
    }
}
