package com.shumai.taczpip.mixin;

import java.util.Comparator;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.shumai.taczpip.client.ConfigDisplayOrder;

@Pseudo
@Mixin(targets="com.mrcrayfish.configured.client.screen.ConfigScreen",remap=false)
public abstract class ConfiguredOrderMixin {
    @Redirect(method={"constructEntries","getSearchResults"},
            at=@At(value="INVOKE",target="Ljava/util/List;sort(Ljava/util/Comparator;)V"),require=0,remap=false)
    private void taczpip$sortOwnSettings(List<Object> items,Comparator<Object> original) {
        ConfigDisplayOrder.sort(this,items,original);
    }
}
