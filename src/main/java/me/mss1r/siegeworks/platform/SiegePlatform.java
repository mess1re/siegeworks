package me.mss1r.siegeworks.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import java.nio.file.Path;
import java.util.function.Supplier;
import me.mss1r.siegeworks.item.SiegeDeploymentItem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

public final class SiegePlatform {
    private SiegePlatform() {}
    @ExpectPlatform public static net.minecraft.nbt.CompoundTag persistentData(net.minecraft.world.entity.Entity entity) { throw new AssertionError(); }
    public static double entityReach(Player player) { return me.mss1r.axiomata.platform.AxiomataPlatform.entityReach(player); }
    public static double blockReach(Player player) { return me.mss1r.axiomata.platform.AxiomataPlatform.blockReach(player); }
    @ExpectPlatform public static me.mss1r.siegeworks.block.IncendiaryPotBlock incendiaryPot(net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) { throw new AssertionError(); }
    @ExpectPlatform public static boolean isFakePlayer(Player player) { throw new AssertionError(); }
    @ExpectPlatform public static double entityReachBonus(Player player) { throw new AssertionError(); }
    @ExpectPlatform public static Path defaultConfigDirectory() { throw new AssertionError(); }
    @ExpectPlatform public static SiegeDeploymentItem deploymentItem(Supplier<? extends EntityType<?>> type, Item.Properties properties) { throw new AssertionError(); }
    //? if <1.21 {
    /*@ExpectPlatform public static net.minecraft.world.entity.ai.attributes.Attribute entityReachAttribute() { throw new AssertionError(); }
    *///?}
}
