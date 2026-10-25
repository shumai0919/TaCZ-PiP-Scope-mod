package com.shumai.taczpip.compat;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

public final class PipMixinPlugin implements IMixinConfigPlugin {
    private static DhAbi.Kind dh = DhAbi.Kind.NONE;
    public static DhAbi.Kind dhKind() { return dh; }
    @Override public void onLoad(String mixinPackage) {
        dh = DhAbi.detect(name -> MixinService.getService().getBytecodeProvider().getClassNode(name));
    }
    @Override public boolean shouldApplyMixin(String target, String mixin) {
        if (mixin.endsWith(".DhScopeBuffersMixin")) return dh == DhAbi.Kind.LEGACY;
        if (mixin.endsWith(".DhModernTaaMixin") || mixin.endsWith(".DhModernTaaSeedMixin")
                || mixin.endsWith(".DhModernJitterMixin")) return dh == DhAbi.Kind.MODERN;
        return true;
    }
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> mine, Set<String> others) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String name, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String name, ClassNode node, String mixin, IMixinInfo info) {}
}
