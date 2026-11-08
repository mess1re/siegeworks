package me.mss1r.siegeworks.mixin;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import me.mss1r.siegeworks.integration.recruits.RecruitsFirearmGoal;
import me.mss1r.siegeworks.item.FirearmItem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.talhanation.recruits.entities.AbstractRecruitEntity", remap = false)
public abstract class RecruitsFirearmMixin extends PathfinderMob {
    protected RecruitsFirearmMixin(EntityType<? extends PathfinderMob> type, Level level) { super(type, level); }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void siegeworks$firearmGoal(CallbackInfo ci) {
        goalSelector.addGoal(2, new RecruitsFirearmGoal((AbstractRecruitEntity) (Object) this));
    }

    @Inject(method = "canBlock", at = @At("HEAD"), cancellable = true, remap = false)
    private void siegeworks$keepLoadingHandFree(CallbackInfoReturnable<Boolean> ci) {
        var recruit = (AbstractRecruitEntity) (Object) this;
        if (getMainHandItem().getItem() instanceof FirearmItem && recruit.getShouldRanged()
                && !recruit.getShouldBlock()) ci.setReturnValue(false);
    }
}
