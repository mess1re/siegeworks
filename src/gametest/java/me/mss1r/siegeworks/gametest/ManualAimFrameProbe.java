package me.mss1r.siegeworks.gametest;

import com.mojang.logging.LogUtils;
import me.mss1r.siegeworks.client.SiegeworksKeyMappings;
import me.mss1r.siegeworks.client.aim.SiegeAimController;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
//? if forge {
/*import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.TickEvent;
*///?} else {
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
//?}

/** Real frame/camera/model diagnostic in an isolated world; never packaged in the mod jar. */
@EventBusSubscriber(modid = "siegeworks", value = Dist.CLIENT)
public final class ManualAimFrameProbe {
    private static final String[] TYPES = {"culverin", "serpentine", "mons_meg", "hwacha"};
    private static final String[] BONES = {"cannon_aim", "cannon_aim", "aim_base", "base"};
    private static final List<String> CSV = new ArrayList<>(List.of("engine,frame,tick,expected,cameraExpected,camera,aim,model"));
    private static volatile int engineId = -1;
    private static boolean started, switching, complete;
    private static int engineIndex, age, frame, failures, totalChecks, engineChecks;
    private static float expected;
    private static float heldPitch;
    private static double maxCameraError, maxModelError;

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null || mc.getSingleplayerServer() == null) return;
        if (!started) {
            started = true;
            mc.options.enableVsync().set(false);
            mc.options.framerateLimit().set(120);
            prepareEngine();
        }
        if (mc.player.getVehicle() instanceof AbstractSiegeEntity siege && siege.getId() == engineId) {
            if (switching) {
                switching = false;
                age = frame = engineChecks = failures = 0;
                maxCameraError = maxModelError = 0;
            }
            age++;
        }
    }

    private static boolean enabled() {
        return Boolean.getBoolean("siegeworks.test.aimScene")
                && "manual_frame_suite".equals(System.getProperty("siegeworks.test.aimSceneEngine"));
    }

    private static void prepareEngine() {
        switching = true;
        Minecraft mc = Minecraft.getInstance();
        mc.getSingleplayerServer().execute(() -> {
            var server = mc.getSingleplayerServer();
            var player = server.getPlayerList().getPlayers().get(0);
            var level = player.serverLevel();
            player.stopRiding();
            level.getEntitiesOfClass(AbstractSiegeEntity.class, new AABB(-20, 90, -20, 20, 125, 20))
                    .forEach(net.minecraft.world.entity.Entity::discard);
            level.setDayTime(6000);
            for (int x = -10; x <= 10; x++) for (int z = -10; z <= 10; z++)
                level.setBlockAndUpdate(new BlockPos(x, 99, z), Blocks.CALCITE.defaultBlockState());
            player.setGameMode(GameType.CREATIVE);
            player.teleportTo(.5, 100, -3);
            var siege = (AbstractSiegeEntity) BuiltInRegistries.ENTITY_TYPE.get(
                    MinecraftVersionCompat.id("siegeworks", TYPES[engineIndex])).create(level);
            siege.setPos(.5, 100, .5);
            siege.applyYaw(0);
            level.addFreshEntity(siege);
            engineId = siege.getId();
            player.startRiding(siege, true);
        });
    }

    @SubscribeEvent
    //? if forge {
    /*public static void frame(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) input(); else verify();
    }
    *///?} else {
    public static void beforeFrame(RenderFrameEvent.Pre event) { input(); }

    @SubscribeEvent
    public static void afterFrame(RenderFrameEvent.Post event) { verify(); }
    //?}

    private static AbstractSiegeEntity current() {
        var mc = Minecraft.getInstance();
        return enabled() && !complete && !switching && age > 30 && mc.player != null && mc.screen == null
                && mc.player.getVehicle() instanceof AbstractSiegeEntity siege && siege.getId() == engineId ? siege : null;
    }

    private static void input() {
        var siege = current();
        if (siege == null) return;
        var mc = Minecraft.getInstance();
        // Each view mode gets slow sweeps, abrupt reversals, overshoot of both bounds and a held view.
        int phase = frame % 160;
        float low = siege.getMinAimPitch(), high = siege.getMaxAimPitch();
        float request = phase < 40 ? low + (high - low) * phase / 40
                : phase < 80 ? high - (high - low) * (phase - 40) / 40
                : phase < 100 ? high + 30 : phase < 120 ? low - 30 : (low + high) * .5F;
        int mode = frame / 160;
        mc.options.framerateLimit().set(mode == 0 ? 60 : mode == 1 ? 144 : 30);
        mc.options.setCameraType(mode == 0 ? CameraType.THIRD_PERSON_BACK
                : mode == 1 ? CameraType.FIRST_PERSON : CameraType.THIRD_PERSON_FRONT);
        SiegeworksKeyMappings.BARREL_SIGHT.setDown(mode == 1);
        boolean freeLook = phase >= 140;
        SiegeworksKeyMappings.FREE_LOOK.setDown(freeLook);
        if (phase == 140) heldPitch = mc.player.getXRot();
        double delta = freeLook ? .25 : request - mc.player.getXRot();
        mc.player.turn(0, delta / .15D);
        if (freeLook) {
            SiegeAimController.applyFreeLookInput(siege, 0, (float) delta);
            SiegeAimController.alignPlayerWithSiege(mc.player, siege, true);
        } else {
            SiegeAimController.applyManualAimInput(mc.player, siege);
        }
        expected = mc.player.getXRot();
        if (phase == 130) siege.setTrackedPitch(expected == low ? high : low);
    }

    private static void verify() {
        var siege = current();
        if (siege == null) return;
        var mc = Minecraft.getInstance();
        float camera = mc.gameRenderer.getMainCamera().getXRot();
        boolean freeLook = SiegeAimController.isFreeLookActive(mc.player, siege);
        float viewPitch = freeLook ? SiegeAimController.getFreeLookPitch(siege) : expected;
        float cameraExpected = mc.options.getCameraType() == CameraType.THIRD_PERSON_FRONT ? -viewPitch : viewPitch;
        var renderer = mc.getEntityRenderDispatcher().getRenderer(siege);
        double model = renderer instanceof GeoEntityRenderer<?> geo
                ? geo.getGeoModel().getBone(BONES[engineIndex]).map(bone -> -(double) bone.getRotX() * 180 / Math.PI)
                        .orElse(Double.NaN) : Double.NaN;
        double cameraError = Math.abs(camera - cameraExpected), modelError = Math.abs(model - expected);
        maxCameraError = Math.max(maxCameraError, cameraError);
        maxModelError = Math.max(maxModelError, modelError);
        if (cameraError > .01 || !Double.isFinite(model) || modelError > .01
                || freeLook && Math.abs(expected - heldPitch) > .01) {
            if (failures++ < 3) LogUtils.getLogger().error("AIM_FRAME mismatch engine={} frame={} tick={} input={} camera={} aim={} model={}",
                    TYPES[engineIndex], frame, siege.tickCount, expected, camera, siege.getRenderedAimPitch(), model);
        }
        CSV.add(TYPES[engineIndex] + "," + frame + "," + siege.tickCount + "," + expected + "," + cameraExpected + "," + camera + ","
                + siege.getRenderedAimPitch() + "," + model);
        engineChecks++;
        totalChecks++;
        if (++frame < 480) return;
        LogUtils.getLogger().info("AIM_FRAME {} engine={} frames={} failures={} maxCameraError={} maxModelError={}",
                failures == 0 ? "PASS" : "FAIL", TYPES[engineIndex], engineChecks, failures, maxCameraError, maxModelError);
        SiegeworksKeyMappings.BARREL_SIGHT.setDown(false);
        SiegeworksKeyMappings.FREE_LOOK.setDown(false);
        if (++engineIndex < TYPES.length) { prepareEngine(); return; }
        complete = true;
        try { Files.write(mc.gameDirectory.toPath().resolve("manual-aim-frames.csv"), CSV); }
        catch (java.io.IOException e) { throw new IllegalStateException(e); }
        LogUtils.getLogger().info("AIM_FRAME completed checks={}", totalChecks);
        mc.stop();
    }
}
