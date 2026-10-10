package me.mss1r.siegeworks.mixin;

import com.talhanation.recruits.entities.IStrategicFire;
import com.talhanation.recruits.entities.SiegeEngineerEntity;
import me.mss1r.siegeworks.integration.recruits.RecruitsFireZone;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.talhanation.recruits.CommandEvents", remap = false)
public abstract class RecruitsStrategicFireMixin {
    // A new point order replaces the RTS zone; restoring the saved point does not.
    @Redirect(method = "onStrategicFireCommand", at = @At(value = "INVOKE",
            target = "Lcom/talhanation/recruits/entities/IStrategicFire;setStrategicFirePos(Lnet/minecraft/core/BlockPos;)V"),
            remap = false)
    private static void siegeworks$replaceFireZoneWithPosition(IStrategicFire unit, BlockPos position) {
        if (unit instanceof SiegeEngineerEntity engineer && !engineer.level().isClientSide) {
            RecruitsFireZone.clear(engineer);
        }
        unit.setStrategicFirePos(position);
    }
}
