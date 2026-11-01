package me.mss1r.siegeworks.mixin;

import me.mss1r.axiomata.collision.system.StructurePathfindingSystem;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//? if <1.21 {
/*import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
*///?}

/** Recruits 1.20.1 replaces vanilla classification; 1.21.1 inherits Axiomata's vanilla hook. */
@Pseudo
@Mixin(targets = "com.talhanation.recruits.entities.ai.navigation.RecruitsPathNodeEvaluator")
public abstract class RecruitsPathNodeEvaluatorMixin {
    //? if <1.21 {
    /*@Inject(method = "getBlockPathType(Lnet/minecraft/world/level/BlockGetter;IIILnet/minecraft/world/entity/Mob;)Lnet/minecraft/world/level/pathfinder/BlockPathTypes;",
            at = @At("RETURN"), cancellable = true)
    private void siegeworks$blockStructureNodes(BlockGetter level, int x, int y, int z, Mob mob,
                                                CallbackInfoReturnable<BlockPathTypes> cir) {
        if (cir.getReturnValue() != BlockPathTypes.BLOCKED
                && StructurePathfindingSystem.blocked(mob, x, y, z)) {
            cir.setReturnValue(BlockPathTypes.BLOCKED);
        }
    }
    *///?}
}
