package me.mss1r.siegeworks.mixin;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import me.mss1r.siegeworks.integration.recruits.RecruitsFirearmGoal;
import me.mss1r.siegeworks.item.FirearmItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.talhanation.recruits.entities.ai.RecruitMeleeAttackGoal", remap = false)
public abstract class RecruitsFirearmMeleeMixin {
    @Shadow @Final protected AbstractRecruitEntity recruit;
    @Unique private ItemStack siegeworks$rangedFirearm = ItemStack.EMPTY;

    @Inject(method = {"canUse", "canContinueToUse"}, at = @At("HEAD"), cancellable = true, remap = true)
    private void siegeworks$useFirearmAtRange(CallbackInfoReturnable<Boolean> ci) {
        if ((recruit.getMainHandItem().getItem() instanceof FirearmItem || !siegeworks$rangedFirearm.isEmpty()) && recruit.getShouldRanged()
                && recruit.getTarget() != null
                && recruit.distanceToSqr(recruit.getTarget()) >= RecruitsFirearmGoal.MELEE_RANGE_SQR) ci.setReturnValue(false);
    }

    @Inject(method = "start", at = @At("HEAD"), remap = true)
    private void siegeworks$rememberFirearm(CallbackInfo ci) {
        ItemStack held = recruit.getMainHandItem();
        siegeworks$rangedFirearm = held.getItem() instanceof FirearmItem ? held : ItemStack.EMPTY;
    }

    @Inject(method = "stop", at = @At("TAIL"), remap = true)
    private void siegeworks$returnToFirearm(CallbackInfo ci) {
        if (recruit.getShouldRanged() && !siegeworks$rangedFirearm.isEmpty()) {
            recruit.switchMainHandItem(stack -> stack == siegeworks$rangedFirearm);
        }
        siegeworks$rangedFirearm = ItemStack.EMPTY;
    }
}
