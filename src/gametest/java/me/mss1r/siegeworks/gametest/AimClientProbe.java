package me.mss1r.siegeworks.gametest;

import com.mojang.logging.LogUtils;
import me.mss1r.siegeworks.entity.siege.AbstractBoltThrowerEntity;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
//? if forge {
/*import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.TickEvent;
*///?} else {
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
//?}

/** Isolated client diagnostic; never included in production jars. */
@EventBusSubscriber(modid = "siegeworks", value = Dist.CLIENT)
public final class AimClientProbe {
    private static boolean started;
    private static int ticks;
    private static long lastFrame;
    private static long maxFrame;
    private static int frames;
    private static float lastYaw;
    private static float lastPitch;
    private static double maxTurn;

    @SubscribeEvent
    //? if forge {
    /*public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
    *///?} else {
    public static void tick(ClientTickEvent.Post event) {
    //?}
        if (!Boolean.getBoolean("siegeworks.test.aimScene")) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null) return;
        var server = mc.getSingleplayerServer();
        if (server == null) return;
        if (!started) {
            started = true;
            server.execute(() -> {
                var player = server.getPlayerList().getPlayers().get(0);
                var level = player.serverLevel();
                level.getEntitiesOfClass(AbstractSiegeEntity.class, new AABB(-20, 90, -20, 20, 110, 20))
                        .forEach(net.minecraft.world.entity.Entity::discard);
                boolean pedestal = "pedestal".equals(System.getProperty("siegeworks.test.aimSceneFloor"));
                for (int x = -10; x <= 10; x++) for (int z = -10; z <= 10; z++) {
                    level.setBlockAndUpdate(new BlockPos(x, 99, z),
                            (pedestal && (x != 0 || z != 0) ? Blocks.AIR : Blocks.CALCITE).defaultBlockState());
                }
                player.teleportTo(0.5, 100, -3);
                var siege = SiegeworksEntities.TOWER_CROSSBOW_ENTITY.get().create(level);
                siege.setPos(0.5, 100, 0.5);
                level.addFreshEntity(siege);
                player.startRiding(siege, true);
                LogUtils.getLogger().info("AIM_CLIENT started calcite scene, pedestal={}, engine={}", pedestal, siege.getId());
            });
        }
        if (!(mc.player.getVehicle() instanceof AbstractBoltThrowerEntity siege)) return;
        ticks++;
        if (ticks > 40 && ticks <= 160) siege.applyClientAimInput(0, -4);
        if (ticks > 160 && ticks <= 300) siege.applyClientAimInput(4, 0);
        if (ticks > 300 && ticks <= 420) siege.applyClientAimInput(0, 4);
        if (ticks % 40 == 0) {
            var camera = mc.gameRenderer.getMainCamera();
            LogUtils.getLogger().info("AIM_CLIENT tick={} frames={} maxFrameMs={} maxTurn={} aim=({}, {}) camera=({}, {}) pos={} root={}",
                    ticks, frames, maxFrame / 1_000_000.0, maxTurn, siege.getRenderedAimYaw(), siege.getRenderedAimPitch(),
                    camera.getYRot(), camera.getXRot(), camera.getPosition(), siege.position());
            frames = 0; maxFrame = 0; maxTurn = 0;
        }
        if (ticks == 440) {
            LogUtils.getLogger().info("AIM_CLIENT completed");
            mc.stop();
        }
    }

    @SubscribeEvent
    //? if forge {
    /*public static void frame(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
    *///?} else {
    public static void frame(RenderFrameEvent.Post event) {
    //?}
        if (!Boolean.getBoolean("siegeworks.test.aimScene") || ticks == 0) return;
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        long now = System.nanoTime();
        if (lastFrame != 0) {
            maxFrame = Math.max(maxFrame, now - lastFrame);
            maxTurn = Math.max(maxTurn, Math.max(Math.abs(Mth.wrapDegrees(camera.getYRot() - lastYaw)),
                    Math.abs(camera.getXRot() - lastPitch)));
        }
        frames++;
        lastFrame = now; lastYaw = camera.getYRot(); lastPitch = camera.getXRot();
    }
}
