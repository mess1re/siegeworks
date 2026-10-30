package me.mss1r.siegeworks.fabric.mixin;
import me.mss1r.siegeworks.block.IncendiaryPotBlockEntity;
import me.mss1r.siegeworks.fabric.FabricPotState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=IncendiaryPotBlockEntity.class, remap=false)
public abstract class PotContentsMixin {
    @Inject(method={"setFilling", "read"}, at=@At("TAIL"), remap=false)
    private void siegeworks$syncFuelState(CallbackInfo ci) { FabricPotState.sync((IncendiaryPotBlockEntity)(Object)this); }
}
