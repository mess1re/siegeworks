package me.mss1r.siegeworks.gametest;

import com.mojang.logging.LogUtils;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/** Full server/client fall with a connected operator, in the isolated diagnostic world. */
public final class SiegeGravityClientProbe {
    private static volatile int engineId = -1;
    private static volatile double serverLandingY = Double.NaN;
    private static boolean started;
    private static int age, landingTick;
    private static double previousY = Double.NaN, maximumFallStep;

    public static void tick() {
        var mc = Minecraft.getInstance();
        var server = mc.getSingleplayerServer();
        if (mc.player == null || mc.level == null || mc.screen != null || server == null) return;
        if (!started) {
            started = true;
            mc.options.pauseOnLostFocus = false;
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            server.execute(() -> {
                var player = server.getPlayerList().getPlayers().get(0);
                var level = player.serverLevel();
                player.stopRiding();
                level.getEntities((Entity) null, new AABB(-30, 90, -30, 30, 135, 30), e -> e != player)
                        .forEach(Entity::discard);
                for (var pos : BlockPos.betweenClosed(new BlockPos(-12, 99, -12), new BlockPos(12, 130, 12))) {
                    if (pos.getY() == 99) level.setBlockAndUpdate(pos, Blocks.CALCITE.defaultBlockState());
                    else if (!level.getBlockState(pos).isAir()) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                }
                level.setDayTime(6000);
                player.setGameMode(GameType.CREATIVE);
                player.teleportTo(.5, 112, -2);
                var siege = SiegeworksEntities.ARCBALLISTA_ENTITY.get().create(level);
                siege.setPos(.5, 112, .5);
                siege.applyYaw(0);
                level.addFreshEntity(siege);
                player.startRiding(siege, true);
                engineId = siege.getId();
            });
        }
        if (!(mc.level.getEntity(engineId) instanceof AbstractSiegeEntity siege)
                || mc.player.getVehicle() != siege) return;
        age++;
        mc.player.setXRot(0); mc.player.xRotO = 0;
        double y = siege.getY();
        if (Double.isFinite(previousY)) maximumFallStep = Math.max(maximumFallStep, previousY - y);
        previousY = y;
        if (landingTick == 0 && y < 101.5) landingTick = age;
        if (age % 5 == 0 && age <= 40) LogUtils.getLogger().info(
                "GRAVITY_CLIENT tick={} y={} riderY={} peakFallStep={}", age, y, mc.player.getY(), maximumFallStep);
        if (age == 40) server.execute(() -> {
            var engine = server.overworld().getEntity(engineId);
            serverLandingY = engine == null ? Double.NaN : engine.getY();
        });
        if (age == 60) {
            double seatError = Math.abs(mc.player.getY() - y - siege.getPassengerOffset(mc.player).y);
            boolean passed = landingTick > 0 && landingTick <= 40 && maximumFallStep > .3
                    && serverLandingY < 101.5 && y < 101.5 && seatError < .01;
            LogUtils.getLogger().info("GRAVITY_CLIENT {} landedAt={} peakFallStep={} serverY={} clientY={} seatError={}",
                    passed ? "PASS" : "FAIL", landingTick, maximumFallStep, serverLandingY, y, seatError);
            net.minecraft.client.Screenshot.grab(mc.gameDirectory, "siege-gravity-landed.png", mc.getMainRenderTarget(), m -> {});
        }
        if (age == 65) mc.stop();
    }
}
