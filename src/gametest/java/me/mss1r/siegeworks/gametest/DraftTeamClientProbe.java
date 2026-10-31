package me.mss1r.siegeworks.gametest;

import com.mojang.logging.LogUtils;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.HorseModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import java.util.HashMap;
import java.util.Map;
//? if forge {
/*import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
*///?} else {
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
//?}

/** Checks rendered leg poses and GeckoLib's rotation dispatch in an isolated test world. */
@EventBusSubscriber(modid = "siegeworks", value = Dist.CLIENT)
public final class DraftTeamClientProbe {
    private static final String[] MACHINES = {"onager", "culverin", "mons_meg", "hwacha", "battering_ram", "siege_tower"};
    private static final String[] ANIMALS = {"horse", "donkey", "mule", "camel", "horse", "horse"};
    private static final Map<Integer, double[]> LEGS = new HashMap<>();
    private static final Map<Integer, AnimationSample> ANIMATION = new HashMap<>();
    private record AnimationSample(int tick, float speed, float position) {}
    private static double maxSpeedGap, maxPhaseGap;
    private static int continuityChecks;
    private static int tailPoseChecks;
    private static float initialYaw, maxTurn;
    private static volatile int engineId = -1;
    private static int index, age, failures, poseChecks, forwardChecks, reverseChecks;
    private static boolean started, switching = true, complete;
    private static Vec3 previousHorse;

    private static boolean enabled() {
        return "draft_suite".equals(System.getProperty("siegeworks.test.aimSceneEngine"));
    }

    public static void tick() {
        var mc = Minecraft.getInstance();
        if (complete || mc.player == null || mc.level == null || mc.screen != null || mc.getSingleplayerServer() == null) return;
        if (!started) {
            started = true;
            mc.options.framerateLimit().set(60);
            mc.options.pauseOnLostFocus = false;
            mc.options.fov().set(80);
            prepare();
        }
        if (!(mc.level.getEntity(engineId) instanceof AbstractSiegeEntity siege)) return;
        if (switching) {
            switching = false;
            age = failures = poseChecks = forwardChecks = reverseChecks = 0;
            LEGS.clear(); ANIMATION.clear(); previousHorse = null;
            maxSpeedGap = maxPhaseGap = 0;
            continuityChecks = 0;
            tailPoseChecks = 0;
            initialYaw = siege.getYRot();
            maxTurn = 0;
        }
        age++;
        if (age >= 65 && age <= 235) for (var horse : siege.getTowingMounts()) {
            var animation = horse.walkAnimation;
            var last = ANIMATION.put(horse.getId(),
                    new AnimationSample(horse.tickCount, animation.speed(1), animation.position(1)));
            if (last == null || last.tick() != horse.tickCount - 1) continue;
            double speedGap = Math.abs(animation.speed(0) - last.speed());
            double phaseGap = Math.abs(animation.position(0) - last.position());
            maxSpeedGap = Math.max(maxSpeedGap, speedGap);
            maxPhaseGap = Math.max(maxPhaseGap, phaseGap);
            continuityChecks++;
            if (speedGap > 1E-4 || phaseGap > 1E-4) {
                if (failures++ < 3) LogUtils.getLogger().error(
                        "DRAFT_CLIENT animation discontinuity machine={} age={} speedGap={} phaseGap={}",
                        MACHINES[index], age, speedGap, phaseGap);
            }
        }
        mc.options.keyUp.setDown(age >= 60 && age <= 120);
        mc.options.keyDown.setDown(age >= 195 && age <= 235);
        mc.options.keyLeft.setDown(age >= 90 && age <= 110);
        mc.options.keyRight.setDown(age >= 210 && age <= 225);
        maxTurn = Math.max(maxTurn, Math.abs(Mth.wrapDegrees(siege.getYRot() - initialYaw)));
        if (mc.player.getVehicle() instanceof AbstractHorse horse) {
            mc.options.setCameraType(index == 5 ? CameraType.THIRD_PERSON_FRONT : CameraType.THIRD_PERSON_BACK);
            mc.player.setYRot(horse.getYRot() + (index == 5 ? 0 : 45));
            mc.player.yRotO = mc.player.getYRot();
            mc.player.setXRot(25); mc.player.xRotO = 25;
            if (previousHorse != null) {
                Vec3 step = horse.position().subtract(previousHorse);
                double forward = step.x * -Math.sin(Math.toRadians(horse.yBodyRot))
                        + step.z * Math.cos(Math.toRadians(horse.yBodyRot));
                // A repeated network position is not reverse movement. Count the moving
                // samples and reject a step against the requested direction.
                if (age >= 85 && age <= 115) {
                    if (forward > .001) forwardChecks++;
                    if (forward < -.001 && failures++ < 3) LogUtils.getLogger().error(
                            "DRAFT_CLIENT wrong forward step machine={} age={} step={} heading={} speed={}",
                            MACHINES[index], age, step, horse.yBodyRot, siege.getCurrentDriveSpeed());
                }
                if (age >= 210 && age <= 230) {
                    if (forward < -.001) reverseChecks++;
                    if (forward > .001 && failures++ < 3) LogUtils.getLogger().error(
                            "DRAFT_CLIENT wrong reverse step machine={} age={} step={} heading={} speed={}",
                            MACHINES[index], age, step, horse.yBodyRot, siege.getCurrentDriveSpeed());
                }
            }
            previousHorse = horse.position();
        }
        var server = mc.getSingleplayerServer();
        if (age == 25) server.execute(() -> {
            var engine = (AbstractSiegeEntity) server.overworld().getEntity(engineId);
            for (int slot = 0; slot < engine.towingProfile().mountSlots().size(); slot++) {
                var horse = (AbstractHorse) BuiltInRegistries.ENTITY_TYPE.get(
                        MinecraftVersionCompat.id("minecraft", ANIMALS[index])).create(server.overworld());
                horse.setTamed(true);
                //? if forge {
                /*horse.equipSaddle(net.minecraft.sounds.SoundSource.NEUTRAL);
                *///?} else {
                horse.equipSaddle(new ItemStack(Items.SADDLE), null);
                //?}
                horse.setPos(engine.getX() + 8, engine.getY(), engine.getZ());
                server.overworld().addFreshEntity(horse);
                horse.startRiding(engine, true);
            }
        });
        if (age == 45) server.execute(() -> server.getPlayerList().getPlayers().get(0)
                .startRiding(((AbstractSiegeEntity) server.overworld().getEntity(engineId)).getTowingMount(), true));
        if (age == 245) server.execute(() -> server.getPlayerList().getPlayers().get(0).stopRiding());
        if (age == 188) for (var horse : siege.getTowingMounts()) {
            if (horse.walkAnimation.speed() > .01) failures++;
        }
        if (age == 265) server.execute(() -> {
            var engine = (AbstractSiegeEntity) server.overworld().getEntity(engineId);
            for (var horse : engine.getTowingMounts()) { horse.stopRiding(); horse.discard(); }
        });
        if (index == 0 && (age == 20 || age == 40 || age == 100 || age == 188 || age == 220 || age == 280))
            net.minecraft.client.Screenshot.grab(mc.gameDirectory, "draft-" + age + ".png", mc.getMainRenderTarget(), message -> {});
        if (age != 290) return;
        int mounts = siege.towingProfile().mountSlots().size();
        if (LEGS.size() != mounts || poseChecks < 20 || forwardChecks < 20 || reverseChecks < 15
                || continuityChecks < 100 * mounts || maxTurn < 1
                || index != 3 && tailPoseChecks < 100 * mounts) failures++;
        for (var sample : LEGS.values()) if (sample[1] - sample[0] < .1 || sample[2] < 10) failures++;
        LogUtils.getLogger().info("DRAFT_CLIENT {} machine={} animal={} failures={} modelFrames={} forward={} reverse={} renderedMounts={} legRanges={}",
                failures == 0 ? "PASS" : "FAIL", MACHINES[index], ANIMALS[index], failures, poseChecks,
                forwardChecks, reverseChecks, LEGS.size(), LEGS.values().stream().map(a -> a[1] - a[0]).toList());
        LogUtils.getLogger().info("DRAFT_CLIENT continuity machine={} samples={} maxSpeedGap={} maxPhaseGap={} tailFrames={} turn={}",
                MACHINES[index], continuityChecks, maxSpeedGap, maxPhaseGap, tailPoseChecks, maxTurn);
        if (++index < MACHINES.length) { prepare(); return; }
        complete = true;
        LogUtils.getLogger().info("DRAFT_CLIENT completed");
        mc.stop();
    }

    private static void prepare() {
        switching = true;
        var mc = Minecraft.getInstance();
        mc.options.keyUp.setDown(false); mc.options.keyDown.setDown(false);
        mc.options.keyLeft.setDown(false); mc.options.keyRight.setDown(false);
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        mc.getSingleplayerServer().execute(() -> {
            var server = mc.getSingleplayerServer();
            var player = server.getPlayerList().getPlayers().get(0);
            var level = player.serverLevel();
            player.stopRiding();
            level.getEntities((net.minecraft.world.entity.Entity) null, new AABB(-40, 90, -40, 40, 130, 40), e -> e != player)
                    .forEach(net.minecraft.world.entity.Entity::discard);
            if (index == 0) for (var pos : BlockPos.betweenClosed(new BlockPos(-32, 100, -32), new BlockPos(32, 125, 32))) {
                if (!level.getBlockState(pos).isAir()) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
            for (int x = -32; x <= 32; x++) for (int z = -32; z <= 32; z++)
                level.setBlockAndUpdate(new BlockPos(x, 99, z), Blocks.CALCITE.defaultBlockState());
            level.setDayTime(6000);
            player.setGameMode(GameType.CREATIVE);
            player.teleportTo(.5, 103, -7); player.setYRot(0); player.setXRot(15);
            var engine = (AbstractSiegeEntity) BuiltInRegistries.ENTITY_TYPE.get(
                    MinecraftVersionCompat.id("siegeworks", MACHINES[index])).create(level);
            engine.setPos(.5, 100, .5); engine.applyYaw(35);
            level.addFreshEntity(engine);
            engineId = engine.getId();
        });
    }

    @SubscribeEvent
    public static void rendered(RenderLivingEvent.Post<?, ?> event) {
        if (!enabled() || switching || complete || age < 65 || age > 235
                || !(event.getEntity() instanceof AbstractHorse horse)
                || !(horse.getVehicle() instanceof AbstractSiegeEntity siege) || siege.getId() != engineId) return;
        var model = event.getRenderer().getModel();
        try {
            ModelPart leg;
            if (model instanceof HierarchicalModel<?> hierarchical) {
                leg = hierarchical.getAnyDescendantWithName("left_front_leg").orElseThrow();
            } else {
                var field = HorseModel.class.getDeclaredField("leftFrontLeg");
                field.setAccessible(true); leg = (ModelPart) field.get(model);
                var tailField = HorseModel.class.getDeclaredField("tail");
                tailField.setAccessible(true);
                var tail = (ModelPart) tailField.get(model);
                float amount = horse.walkAnimation.speed(event.getPartialTick());
                if (Math.abs(tail.xRot - ((float) Math.PI / 6 + amount * .75F)) > 1E-5
                        || Math.abs(tail.y - (-5 + amount)) > 1E-5
                        || Math.abs(tail.z - (2 + amount * 2)) > 1E-5) failures++;
                tailPoseChecks++;
            }
            var sample = LEGS.computeIfAbsent(horse.getId(), ignored -> new double[]{Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0});
            sample[0] = Math.min(sample[0], leg.xRot); sample[1] = Math.max(sample[1], leg.xRot); sample[2]++;
            if (model.riding) failures++;
        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }

    @SubscribeEvent
    //? if forge {
    /*public static void frame(TickEvent.RenderTickEvent event) { inspect(event.phase == TickEvent.Phase.START); }
    *///?} else {
    public static void beforeFrame(RenderFrameEvent.Pre event) { inspect(true); }
    @SubscribeEvent
    public static void afterFrame(RenderFrameEvent.Post event) { inspect(false); }
    //?}

    private static void inspect(boolean before) {
        var mc = Minecraft.getInstance();
        if (!enabled() || switching || complete || mc.level == null || age < 5) return;
        if (!(mc.level.getEntity(engineId) instanceof AbstractSiegeEntity siege)) return;
        var renderer = mc.getEntityRenderDispatcher().getRenderer(siege);
        if (!(renderer instanceof GeoEntityRenderer<?> geo)) return;
        if (before) return;
        // Invoke the exact signature used by GeckoLib, through its base class so virtual
        // dispatch catches an override of an obsolete overload. Bone world matrices are
        // cached across render instances and cannot establish this contract reliably.
        var pose = new com.mojang.blaze3d.vertex.PoseStack();
        float bodyYaw = Mth.rotLerp(.5F, siege.yBodyRotO, siege.yBodyRot);
        try {
            //? if forge {
            /*var rotation = GeoEntityRenderer.class.getDeclaredMethod("applyRotations", net.minecraft.world.entity.Entity.class,
                    com.mojang.blaze3d.vertex.PoseStack.class, float.class, float.class, float.class);
            rotation.setAccessible(true);
            rotation.invoke(geo, siege, pose, (float) siege.tickCount, bodyYaw, .5F);
            *///?} else {
            var rotation = GeoEntityRenderer.class.getDeclaredMethod("applyRotations", net.minecraft.world.entity.Entity.class,
                    com.mojang.blaze3d.vertex.PoseStack.class, float.class, float.class, float.class, float.class);
            rotation.setAccessible(true);
            rotation.invoke(geo, siege, pose, (float) siege.tickCount, bodyYaw, .5F, siege.getScale());
            //?}
        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
        var right = pose.last().pose().transformDirection(new Vector3f(1, 0, 0)).normalize();
        var transform = siege.collisionTransform();
        var collision = new me.mss1r.axiomata.collision.StructureTransform(0, 0, 0,
                transform.yawDegrees() + Mth.wrapDegrees(bodyYaw - siege.getVisualRotationYInDegrees()))
                .directionToWorld(new Vec3(1, 0, 0));
        double alignment = right.x * collision.x + right.z * collision.z;
        // Gecko's authored X axis is the inverse of the collision X axis.
        if (alignment > -.99) {
            if (failures++ < 3) LogUtils.getLogger().error("DRAFT_CLIENT model turned machine={} age={} dot={} root={} towed={}",
                    MACHINES[index], age, alignment, siege.getYRot(), siege.isTowed());
        }
        poseChecks++;
    }
}
