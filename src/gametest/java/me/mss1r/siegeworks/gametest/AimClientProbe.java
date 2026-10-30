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
    private static int manualPitchChecks;
    private static int manualEngineIndex;
    private static volatile int nextManualEngineId = -1;
    private static boolean switchingManualEngine;

    @SubscribeEvent
    //? if forge {
    /*public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
    *///?} else {
    public static void tick(ClientTickEvent.Post event) {
    //?}
        if (Boolean.getBoolean("siegeworks.test.networkLadder")) { networkLadder(); return; }
        if (!Boolean.getBoolean("siegeworks.test.aimScene")) return;
        if ("construction_suite".equals(System.getProperty("siegeworks.test.aimSceneEngine"))) {
            constructionProbe();
            return;
        }
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
                String engine = System.getProperty("siegeworks.test.aimSceneEngine", "tower_crossbow");
                AbstractSiegeEntity siege = switch (engine) {
                    case "culverin", "manual_suite" -> SiegeworksEntities.CULVERIN_ENTITY.get().create(level);
                    case "serpentine" -> SiegeworksEntities.SERPENTINE_ENTITY.get().create(level);
                    case "mons_meg" -> SiegeworksEntities.MONS_MEG_ENTITY.get().create(level);
                    default -> SiegeworksEntities.TOWER_CROSSBOW_ENTITY.get().create(level);
                };
                siege.setPos(0.5, 100, 0.5);
                level.addFreshEntity(siege);
                player.startRiding(siege, true);
                LogUtils.getLogger().info("AIM_CLIENT started calcite scene, pedestal={}, engine={}", pedestal, siege.getId());
            });
        }
        if (mc.player.getVehicle() instanceof AbstractSiegeEntity manual && !manual.usesIndependentPitchAim()) {
            testManualPitch(mc, manual);
            return;
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

    private static int networkWaiting;
    private static boolean networkStarted;
    private static double networkLastZ;
    private static int networkTicks;
    private static void networkLadder() {
        var mc=Minecraft.getInstance();
        if(mc.player==null || mc.level==null) {
            if(++networkWaiting%100==0) LogUtils.getLogger().info("NETWORK_LADDER login screen={}",mc.screen==null?null:mc.screen.getClass().getName());
            if(mc.screen instanceof net.minecraft.client.gui.screens.DisconnectedScreen) {
                try {
                    var reason=net.minecraft.client.gui.screens.DisconnectedScreen.class.getDeclaredField("reason");
                    reason.setAccessible(true);
                    LogUtils.getLogger().error("NETWORK_LADDER FAIL login={}",((net.minecraft.network.chat.Component)reason.get(mc.screen)).getString());
                } catch(ReflectiveOperationException ex) { LogUtils.getLogger().error("NETWORK_LADDER FAIL login",ex); }
                net.minecraft.client.Screenshot.grab(mc.gameDirectory,"network-disconnect.png",mc.getMainRenderTarget(),m->{});
                mc.stop();return;
            }
            if(networkStarted) {LogUtils.getLogger().error("NETWORK_LADDER FAIL disconnected");mc.stop();}
            return;
        }
        if(!networkStarted && ++networkWaiting%100==0) {
            LogUtils.getLogger().info("NETWORK_LADDER waiting pos={} screen={}",mc.player.position(), mc.screen==null?null:mc.screen.getClass().getName());
            if(networkWaiting==100) net.minecraft.client.Screenshot.grab(mc.gameDirectory,"network-wait.png",mc.getMainRenderTarget(),m->{});
        }
        if(mc.screen!=null) return;
        if(!networkStarted) {
            if(Math.abs(mc.player.getX()-8.5)>.2 || Math.abs(mc.player.getY()-100)>.2
                    || Math.abs(mc.player.getZ()-3.9)>.3) return;
            networkStarted=true;networkLastZ=mc.player.getZ();
            LogUtils.getLogger().info("NETWORK_LADDER started at {}",mc.player.position());
        }
        networkTicks++;
        mc.player.setYRot(0);
        mc.options.keyUp.setDown(mc.player.getZ()<9.2);
        double back=networkLastZ-mc.player.getZ();
        networkLastZ=mc.player.getZ();
        if(back>.5) {mc.options.keyUp.setDown(false);LogUtils.getLogger().error("NETWORK_LADDER FAIL rollback={} pos={}",back,mc.player.position());mc.stop();}
        if(networkTicks%40==0) LogUtils.getLogger().info("NETWORK_LADDER tick={} pos={} ground={}",networkTicks,mc.player.position(),mc.player.onGround());
        if(networkTicks>=200 && mc.player.getZ()>8.6 && Math.abs(mc.player.getY()-104)<.15 && mc.player.onGround()) {
            mc.options.keyUp.setDown(false);LogUtils.getLogger().info("NETWORK_LADDER PASS ticks={} pos={}",networkTicks,mc.player.position());mc.stop();
        } else if(networkTicks==600) {
            mc.options.keyUp.setDown(false);LogUtils.getLogger().error("NETWORK_LADDER FAIL stalled at {}",mc.player.position());mc.stop();
        }
    }

    private static volatile int[] constructionIds;
    private static final String[] CONSTRUCTION_TYPES = {"tower_crossbow", "arcballista", "culverin", "onager"};
    private static int hiddenVehicleHealthFrames;

    @SubscribeEvent(priority =
            //? if forge {
            /*net.minecraftforge.eventbus.api.EventPriority.LOWEST,
            *///?} else {
            net.neoforged.bus.api.EventPriority.LOWEST,
            //?}
            receiveCanceled = true)
    //? if forge {
    /*public static void vehicleHealth(net.minecraftforge.client.event.RenderGuiOverlayEvent.Pre event) {
        boolean vehicleHealth = event.getOverlay().id().equals(
                net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.MOUNT_HEALTH.id());
    *///?} else {
    public static void vehicleHealth(net.neoforged.neoforge.client.event.RenderGuiLayerEvent.Pre event) {
        boolean vehicleHealth = event.getName().equals(
                net.neoforged.neoforge.client.gui.VanillaGuiLayers.VEHICLE_HEALTH);
    //?}
        if (!"construction_suite".equals(System.getProperty("siegeworks.test.aimSceneEngine"))) return;
        var player = Minecraft.getInstance().player;
        if (vehicleHealth && player != null && player.getVehicle() instanceof AbstractSiegeEntity) {
            if (!event.isCanceled()) {
                LogUtils.getLogger().error("VEHICLE_HEALTH_CLIENT FAIL uncanceled health layer");
                Minecraft.getInstance().stop();
            }
            hiddenVehicleHealthFrames++;
        }
    }

    private static void constructionProbe() {
        var mc = Minecraft.getInstance();
        var server = mc.getSingleplayerServer();
        if (mc.level == null || mc.player == null || mc.screen != null || server == null) return;
        if (!started) {
            started = true;
            server.execute(() -> {
                var player = server.getPlayerList().getPlayers().get(0);
                var level = player.serverLevel();
                level.getEntitiesOfClass(AbstractSiegeEntity.class, new AABB(-30, 90, -30, 30, 130, 30))
                        .forEach(net.minecraft.world.entity.Entity::discard);
                for (int x=-20; x<=20; x++) for(int z=-20; z<=20; z++)
                    level.setBlockAndUpdate(new BlockPos(x,99,z), Blocks.CALCITE.defaultBlockState());
                player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
                player.teleportTo(0,104,10);
                int[] ids = new int[CONSTRUCTION_TYPES.length];
                for (int i=0;i<ids.length;i++) {
                    var type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(
                            me.mss1r.siegeworks.platform.MinecraftVersionCompat.id("siegeworks", CONSTRUCTION_TYPES[i]));
                    var engine = (AbstractSiegeEntity)type.create(level);
                    engine.buildProgress().begin("siegeworks:"+CONSTRUCTION_TYPES[i], null);
                    engine.orientForPlacement(45+i*90);
                    engine.setPos(-12+i*8,100,0);
                    level.addFreshEntity(engine);
                    ids[i]=engine.getId();
                }
                constructionIds=ids;
            });
        }
        if (constructionIds == null) return;
        ticks++;
        if (ticks==80 || ticks==180) {
            for(int i=0;i<constructionIds.length;i++) {
                var engine=(AbstractSiegeEntity)mc.level.getEntity(constructionIds[i]);
                float yaw=45+i*90;
                if(engine==null || engine.isFullyBuilt() != (ticks==180)
                        || Math.abs(Mth.wrapDegrees(engine.getYRot()-yaw))>.01F
                        || Math.abs(Mth.wrapDegrees(engine.getRenderedAimYaw()-yaw))>.01F
                        || Math.abs(Mth.wrapDegrees(engine.getVisualRotationYInDegrees()-yaw))>.01F
                        || Math.abs(Mth.wrapDegrees(engine.collisionTransform().yawDegrees()-yaw))>.01F) {
                    LogUtils.getLogger().error("CONSTRUCTION_CLIENT FAIL phase={} type={} built={} root={} visual={} collision={}",
                            ticks,CONSTRUCTION_TYPES[i],engine==null?null:engine.isFullyBuilt(),
                            engine==null?null:engine.getYRot(),engine==null?null:engine.getVisualRotationYInDegrees(),
                            engine==null?null:engine.collisionTransform().yawDegrees());
                    mc.stop();return;
                }
            }
            LogUtils.getLogger().info("CONSTRUCTION_CLIENT PASS phase={} engines=4",ticks);
        }
        if(ticks==100) server.execute(() -> {
            for(int id:constructionIds) {
                var engine=(AbstractSiegeEntity)server.overworld().getEntity(id);
                engine.buildProgress().begin("",null);
                me.mss1r.axiomata.blueprint.internal.construction.ConstructionSync.changed(engine);
            }
        });
        if (ticks == 190) server.execute(() -> {
            var player = server.getPlayerList().getPlayers().get(0);
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.startRiding(server.overworld().getEntity(constructionIds[2]), true);
        });
        if (ticks == 230) {
            LogUtils.getLogger().info("VEHICLE_HEALTH_CLIENT {} hiddenFrames={}",
                    hiddenVehicleHealthFrames > 0 ? "PASS" : "FAIL", hiddenVehicleHealthFrames);
            mc.stop();
        }
    }

    private static void testManualPitch(Minecraft mc, AbstractSiegeEntity siege) {
        if (switchingManualEngine) {
            if (siege.getId() != nextManualEngineId) return;
            switchingManualEngine = false;
            ticks = 0;
            manualPitchChecks = 0;
        }
        ticks++;
        // Alternate fast mouse requests across the entire elevation range, then deliver
        // the previous request through the same entity-data callback as a delayed server echo.
        int phase = ticks % 40;
        if (ticks > 40 && phase == 1) mc.player.setXRot(siege.getMinAimPitch());
        if (ticks > 40 && phase == 21) mc.player.setXRot(siege.getMaxAimPitch());
        if (ticks > 40 && (phase == 5 || phase == 25)) {
            float expected = mc.player.getXRot();
            float delayed = phase == 5 ? siege.getMaxAimPitch() : siege.getMinAimPitch();
            siege.updateRenderedAim(0.75F);
            float beforeEcho = siege.getRenderedAimPitch();
            siege.setTrackedPitch(delayed);
            if (Math.abs(siege.getTrackedPitch() - expected) > 0.001F) {
                LogUtils.getLogger().error("AIM_MANUAL FAIL engine={} expected={} delayed={} actual={}",
                        siege.getType(), expected, delayed, siege.getTrackedPitch());
                mc.stop();
                return;
            }
            siege.updateRenderedAim(0.751F);
            if (Math.abs(siege.getRenderedAimPitch() - beforeEcho) > 0.01F) {
                LogUtils.getLogger().error("AIM_MANUAL FAIL rendered elevation jumped: {} -> {}",
                        beforeEcho, siege.getRenderedAimPitch());
                mc.stop();
                return;
            }
            manualPitchChecks++;
        }
        if (ticks == 240) {
            LogUtils.getLogger().info("AIM_MANUAL PASS engine={} checks={}", siege.getType(), manualPitchChecks);
            if ("manual_suite".equals(System.getProperty("siegeworks.test.aimSceneEngine"))
                    && manualEngineIndex < 2) {
                int next = ++manualEngineIndex;
                switchingManualEngine = true;
                mc.getSingleplayerServer().execute(() -> {
                    var player = mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
                    var previous = player.getVehicle();
                    player.stopRiding();
                    if (previous != null) previous.discard();
                    AbstractSiegeEntity replacement = next == 1
                            ? SiegeworksEntities.SERPENTINE_ENTITY.get().create(player.serverLevel())
                            : SiegeworksEntities.MONS_MEG_ENTITY.get().create(player.serverLevel());
                    replacement.setPos(0.5, 100, 0.5);
                    player.serverLevel().addFreshEntity(replacement);
                    nextManualEngineId = replacement.getId();
                    player.startRiding(replacement, true);
                });
                return;
            }
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
