package me.mss1r.siegeworks.gametest;

import com.mojang.logging.LogUtils;
import me.mss1r.siegeworks.entity.siege.AbstractBoltThrowerEntity;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
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
        if (Boolean.getBoolean("siegeworks.test.networkLadder")) { networkLadder(); return; }
        if (!Boolean.getBoolean("siegeworks.test.aimScene")) return;
        if ("draft_suite".equals(System.getProperty("siegeworks.test.aimSceneEngine"))) {
            DraftTeamClientProbe.tick(); return;
        }
        if ("gravity_scene".equals(System.getProperty("siegeworks.test.aimSceneEngine"))) {
            SiegeGravityClientProbe.tick(); return;
        }
        if ("arcballista_parking".equals(System.getProperty("siegeworks.test.aimSceneEngine"))) {
            arcballistaParking(); return;
        }
        if ("manual_frame_suite".equals(System.getProperty("siegeworks.test.aimSceneEngine"))
                || "manual_suite".equals(System.getProperty("siegeworks.test.aimSceneEngine"))) {
            ManualAimFrameProbe.tick(); return;
        }
        if ("sign_suite".equals(System.getProperty("siegeworks.test.aimSceneEngine"))) {
            signProbe(); return;
        }
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

    private static volatile int parkedArcballistaId = -1;
    private static int parkingChecks, parkingFailures;

    private static void arcballistaParking() {
        var mc = Minecraft.getInstance();
        var server = mc.getSingleplayerServer();
        if (mc.player == null || mc.level == null || mc.screen != null || server == null) return;
        if (!started) {
            started = true;
            mc.options.pauseOnLostFocus = false;
            server.execute(() -> {
                var player = server.getPlayerList().getPlayers().get(0);
                var level = player.serverLevel();
                player.stopRiding();
                level.getEntities((Entity) null, new AABB(-30, 90, -30, 30, 130, 30), e -> e != player)
                        .forEach(Entity::discard);
                for (var pos : BlockPos.betweenClosed(new BlockPos(-15, 99, -15), new BlockPos(15, 110, 15)))
                    level.setBlockAndUpdate(pos, (pos.getY() == 99 ? Blocks.CALCITE : Blocks.AIR).defaultBlockState());
                player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
                player.teleportTo(6.5, 100, .5); player.setYRot(90); player.setXRot(10);
                level.setDayTime(6000);
                var arc = SiegeworksEntities.ARCBALLISTA_ENTITY.get().create(level);
                arc.setPos(.5, 100, .5); arc.applyYaw(0); arc.setTrackedPitch(25);
                level.addFreshEntity(arc);
                parkedArcballistaId = arc.getId();
            });
        }
        if (!(mc.level.getEntity(parkedArcballistaId) instanceof me.mss1r.siegeworks.entity.siege.ArcballistaEntity arc)) return;
        mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
        mc.player.setYRot(90); mc.player.yRotO = 90;
        mc.player.setXRot(10); mc.player.xRotO = 10;
        ticks++;
        var renderer = mc.getEntityRenderDispatcher().getRenderer(arc);
        if (renderer instanceof software.bernie.geckolib.renderer.GeoEntityRenderer<?> geo && ticks > 10) {
            geo.getGeoModel().getBone("base").ifPresent(bone -> {
                float angle = (float) Math.toDegrees(bone.getRotX());
                parkingChecks++;
                if (angle < -25.01F || angle > 35.01F
                        || arc.getTrackedPitch() < -35.01F || arc.getTrackedPitch() > 25.01F) parkingFailures++;
            });
        }
        if (ticks == 200) {
            if (parkingChecks < 150) parkingFailures++;
            LogUtils.getLogger().info("ARCBALLISTA_PARK {} checks={} failures={} trackedPitch={} renderedPitch={}",
                    parkingFailures == 0 ? "PASS" : "FAIL", parkingChecks, parkingFailures,
                    arc.getTrackedPitch(), arc.getRenderedModelPitch());
            net.minecraft.client.Screenshot.grab(mc.gameDirectory, "arcballista-parked-limit.png", mc.getMainRenderTarget(), m -> {});
        }
        if (ticks == 210) mc.stop();
    }

    private static int networkWaiting;
    private static volatile int signEngineId;
    private static void signProbe() {
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
                level.setDayTime(6000);
                for (int x=-3; x<=3; x++) for (int y=100; y<=106; y++)
                    level.setBlockAndUpdate(new BlockPos(x,y,-2), Blocks.AIR.defaultBlockState());
                for (int x=-15; x<=15; x++) for (int z=-15; z<=15; z++)
                    level.setBlockAndUpdate(new BlockPos(x,99,z), Blocks.CALCITE.defaultBlockState());
                for (int x=-4; x<=4; x++) for (int y=100; y<=106; y++)
                    level.setBlockAndUpdate(new BlockPos(x,y,6),
                            ((x+y)%2==0 ? Blocks.RED_WOOL : Blocks.OAK_LOG).defaultBlockState());
                player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
                player.teleportTo(0.5,100,-6);
                player.setYRot(0); player.setXRot(-10);
                var hammer = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                        me.mss1r.siegeworks.platform.MinecraftVersionCompat.id("axiomata", "construction_hammer"));
                player.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(hammer));
                player.getInventory().selected=0;
                var engine = SiegeworksEntities.MONS_MEG_ENTITY.get().create(level);
                engine.buildProgress().begin("siegeworks:mons_meg", null);
                engine.orientForPlacement(0);
                engine.setPos(0.5,100,0.5);
                level.addFreshEntity(engine);
                signEngineId = engine.getId();
                // Finished machines behind the label reproduce interference from later entity draws.
                var rear = SiegeworksEntities.TREBUCHET_ENTITY.get().create(level);
                rear.setPos(0.5,100,18);
                level.addFreshEntity(rear);
                for (int side : new int[]{-1,1}) {
                    var gun = SiegeworksEntities.CULVERIN_ENTITY.get().create(level);
                    gun.setPos(side*5.0,100,5);
                    level.addFreshEntity(gun);
                }
            });
        }
        ticks++;
        mc.player.setYRot(ticks < 280 ? 0 : (float)Math.toDegrees(-Math.atan2(
                0.5-mc.player.getX(), 0.5-mc.player.getZ())));
        mc.player.setXRot(-10);
        if (ticks==100) {
            net.minecraft.client.Screenshot.grab(mc.gameDirectory,"stage-sign.png",mc.getMainRenderTarget(),m->{});
            LogUtils.getLogger().info("STAGE_SIGN screenshot hammer={} tag={}",mc.player.getMainHandItem(),
                    mc.player.getMainHandItem().is(me.mss1r.axiomata.blueprint.api.BlueprintTags.CONSTRUCTION_HAMMERS));
        }
        if (ticks==110) server.execute(() -> {
            var engine=(AbstractSiegeEntity)server.overworld().getEntity(signEngineId);
            var tag=new net.minecraft.nbt.CompoundTag();
            engine.buildProgress().save(tag);
            var recipe=me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinitions.get("siegeworks:mons_meg");
            tag.putInt("BuildStage", recipe.stageCount()-1);
            tag.putDouble("BuildHits",2);
            engine.buildProgress().load(tag);
            me.mss1r.axiomata.blueprint.internal.construction.ConstructionSync.changed(engine);
        });
        if (ticks==160) net.minecraft.client.Screenshot.grab(mc.gameDirectory,"stage-sign-late.png",mc.getMainRenderTarget(),m->{});
        if (ticks==170) server.execute(() -> server.getPlayerList().getPlayers().get(0)
                .setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY));
        if (ticks==210) net.minecraft.client.Screenshot.grab(mc.gameDirectory,"stage-sign-no-hammer.png",mc.getMainRenderTarget(),m->{});
        if (ticks==220) server.execute(() -> {
            var player=server.getPlayerList().getPlayers().get(0);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                    new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                            me.mss1r.siegeworks.platform.MinecraftVersionCompat.id("axiomata","construction_hammer"))));
            player.serverLevel().setDayTime(18000);
        });
        if (ticks==260) net.minecraft.client.Screenshot.grab(mc.gameDirectory,"stage-sign-night.png",mc.getMainRenderTarget(),m->{});
        if (ticks==280) server.execute(() -> {
            var player=server.getPlayerList().getPlayers().get(0);
            player.teleportTo(4.5,100,-6);
            player.serverLevel().setDayTime(6000);
        });
        if (ticks==330) net.minecraft.client.Screenshot.grab(mc.gameDirectory,"stage-sign-oblique.png",mc.getMainRenderTarget(),m->{});
        if (ticks==340) server.execute(() -> {
            var player=server.getPlayerList().getPlayers().get(0);
            player.teleportTo(0.5,100,-6);
            for (int x=-3; x<=-1; x++) for (int y=100; y<=106; y++)
                player.serverLevel().setBlockAndUpdate(new BlockPos(x,y,-2), Blocks.STONE.defaultBlockState());
        });
        if (ticks==390) net.minecraft.client.Screenshot.grab(mc.gameDirectory,"stage-sign-partial-wall.png",mc.getMainRenderTarget(),m->{});
        if (ticks==400) server.execute(() -> {
            for (int x=-3; x<=3; x++) for (int y=100; y<=106; y++)
                server.overworld().setBlockAndUpdate(new BlockPos(x,y,-2), Blocks.STONE.defaultBlockState());
        });
        if (ticks==450) net.minecraft.client.Screenshot.grab(mc.gameDirectory,"stage-sign-behind-wall.png",mc.getMainRenderTarget(),m->{});
        if (ticks==460) server.execute(() -> {
            for (int x=-3; x<=3; x++) for (int y=100; y<=106; y++)
                server.overworld().setBlockAndUpdate(new BlockPos(x,y,-2), Blocks.AIR.defaultBlockState());
        });
        if (ticks==510) net.minecraft.client.Screenshot.grab(mc.gameDirectory,"stage-sign-uncovered.png",mc.getMainRenderTarget(),m->{});
        if (ticks==530) mc.stop();
    }
    private static boolean networkStarted;
    private static double networkLastZ;
    private static int networkTicks;
    private static int networkHoldTicks;
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
        // Remain above the ground longer than vanilla's flight timeout before finishing the climb.
        boolean holding = mc.player.getY() >= 101.5 && networkHoldTicks < 140;
        if (holding) networkHoldTicks++;
        mc.options.keyUp.setDown(!holding && mc.player.getZ()<9.2);
        double back=networkLastZ-mc.player.getZ();
        networkLastZ=mc.player.getZ();
        if(back>.5) {mc.options.keyUp.setDown(false);LogUtils.getLogger().error("NETWORK_LADDER FAIL rollback={} pos={}",back,mc.player.position());mc.stop();}
        if(networkTicks%40==0) LogUtils.getLogger().info("NETWORK_LADDER tick={} pos={} ground={}",networkTicks,mc.player.position(),mc.player.onGround());
        if(networkTicks>=200 && networkHoldTicks==140 && mc.player.getZ()>8.6 && Math.abs(mc.player.getY()-104)<.15 && mc.player.onGround()) {
            mc.options.keyUp.setDown(false);LogUtils.getLogger().info("NETWORK_LADDER PASS ticks={} holdTicks={} pos={}",networkTicks,networkHoldTicks,mc.player.position());mc.stop();
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
