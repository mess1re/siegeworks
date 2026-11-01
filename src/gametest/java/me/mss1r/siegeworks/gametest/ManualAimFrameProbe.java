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

/** Real frame/camera/model diagnostic in an isolated world; never packaged in the mod jar. */
public final class ManualAimFrameProbe {
    private static final String[] TYPES = {"culverin", "serpentine", "mons_meg", "hwacha", "arcballista", "tower_crossbow"};
    private static final String[] BONES = {"cannon_aim", "cannon_aim", "aim_base", "base", "base", "engine"};
    private static final List<String> CSV = new ArrayList<>(List.of("engine,frame,tick,expected,cameraExpected,camera,aim,model,mechanism,target"));
    private static volatile int engineId = -1;
    private static boolean started, switching, complete;
    private static int engineIndex, age, frame, failures, totalChecks, engineChecks, modelChecks;
    private static double sampledModelPitch = Double.NaN;
    private static float expected;
    private static float heldPitch;
    private static float lastAim, lastMechanism, lastMechanismStep;
    private static int lastTick, movingSubtickFrames, backwards;
    private static double maxCameraError, maxModelError;

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) {
            mc.setScreen(null);
            mc.mouseHandler.grabMouse();
        }
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
                age = frame = engineChecks = failures = modelChecks = 0;
                movingSubtickFrames = backwards = 0;
                maxCameraError = maxModelError = 0;
            }
            age++;
        }
    }

    private static boolean enabled() {
        return Boolean.getBoolean("siegeworks.test.aimScene")
                && ("manual_frame_suite".equals(System.getProperty("siegeworks.test.aimSceneEngine"))
                    || "manual_suite".equals(System.getProperty("siegeworks.test.aimSceneEngine")));
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
            siege.setTrackedPitch(siege.getMinAimPitch());
            level.addFreshEntity(siege);
            engineId = siege.getId();
            player.startRiding(siege, true);
        });
    }

    public static void beforeRenderFrame() { sampledModelPitch = Double.NaN; input(); }

    public static void afterRenderFrame() { verify(); }

    /** Sample the actual world draw before inventory icons reuse GeckoLib's baked bones. */
    public static void recordModel(net.minecraft.world.entity.Entity rendered) {
        var siege = current();
        if (siege == null || rendered != siege) return;
        var renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(siege);
        sampledModelPitch = renderer instanceof GeoEntityRenderer<?> geo
                ? geo.getGeoModel().getBone(BONES[engineIndex]).map(bone -> -(double) bone.getRotX() * 180 / Math.PI)
                        .orElse(Double.NaN) : Double.NaN;
    }

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
        if (phase == 140) heldPitch = siege.getRenderedAimPitch();
        double delta = freeLook ? .25 : request - mc.player.getXRot();
        mc.player.turn(0, delta / .15D);
        SiegeAimController.applyAimInput(mc.player, siege, 0, (float) delta);
        if (phase == 130) siege.setTrackedPitch(siege.getClientAimTargetPitch() == low ? high : low);
    }

    private static void verify() {
        var siege = current();
        if (siege == null) return;
        var mc = Minecraft.getInstance();
        expected = siege.getRenderedAimPitch();
        float target = siege.getClientAimTargetPitch();
        float mechanism = siege.getClientPredictedAimPitch();
        if (frame > 0) {
            double movement = expected - lastAim;
            float step = mechanism - lastMechanism;
            if (siege.tickCount == lastTick) {
                if (movement * (mechanism - lastAim) < -1.0E-4 || Math.abs(step) > .001) backwards++;
                if (Math.abs(movement) > .001) movingSubtickFrames++;
            } else {
                if (step * lastMechanismStep > 1.0E-5 && movement * step < -1.0E-4) backwards++;
                float speed = engineIndex == 2 ? 1 : engineIndex == 5 ? 2 : 2.5F;
                if (Math.abs(step) > speed * (siege.tickCount - lastTick) + .001) failures++;
                lastMechanismStep = step;
            }
        }
        lastAim = expected;
        lastMechanism = mechanism;
        lastTick = siege.tickCount;
        float camera = mc.gameRenderer.getMainCamera().getXRot();
        boolean freeLook = SiegeAimController.isFreeLookActive(mc.player, siege);
        float viewPitch = freeLook ? SiegeAimController.getFreeLookPitch(siege) : expected;
        float cameraExpected = mc.options.getCameraType() == CameraType.THIRD_PERSON_FRONT ? -viewPitch : viewPitch;
        double model = sampledModelPitch;
        boolean modelDrawn = Double.isFinite(model);
        if (modelDrawn) modelChecks++;
        double cameraError = Math.abs(camera - cameraExpected), modelError = modelDrawn ? Math.abs(model - expected) : 0;
        maxCameraError = Math.max(maxCameraError, cameraError);
        maxModelError = Math.max(maxModelError, modelError);
        if (cameraError > .01 || modelError > .01
                || expected < siege.getMinAimPitch() - .01 || expected > siege.getMaxAimPitch() + .01
                || freeLook && Math.abs(expected - heldPitch) > 5.01) {
            if (failures++ < 3) LogUtils.getLogger().error("AIM_FRAME mismatch engine={} frame={} tick={} input={} camera={} aim={} model={}",
                    TYPES[engineIndex], frame, siege.tickCount, expected, camera, siege.getRenderedAimPitch(), model);
        }
        CSV.add(TYPES[engineIndex] + "," + frame + "," + siege.tickCount + "," + expected + "," + cameraExpected + "," + camera + ","
                + siege.getRenderedAimPitch() + "," + model + "," + mechanism + "," + target);
        engineChecks++;
        totalChecks++;
        if (++frame < 480) return;
        if (movingSubtickFrames < 10 || backwards > 0 || modelChecks < 100) failures++;
        LogUtils.getLogger().info("AIM_FRAME {} engine={} frames={} modelFrames={} failures={} backwards={} movingSubtickFrames={} maxCameraError={} maxModelError={}",
                failures == 0 ? "PASS" : "FAIL", TYPES[engineIndex], engineChecks, modelChecks, failures, backwards,
                movingSubtickFrames, maxCameraError, maxModelError);
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
