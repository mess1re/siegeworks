package me.mss1r.siegeworks.client.firearm;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import me.mss1r.siegeworks.item.FirearmItem;
import me.mss1r.siegeworks.item.RoundShotItem;
import me.mss1r.siegeworks.gameplay.ammunition.RoundShotSize;
import me.mss1r.siegeworks.gameplay.firearm.FirearmActions;
import me.mss1r.siegeworks.registry.SiegeworksItems;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.*;

/** Rigid transforms exported from the reviewed poses; player skin and armor remain vanilla. */
public final class FirearmAnimation implements ResourceManagerReloadListener {
    public static final FirearmAnimation INSTANCE = new FirearmAnimation();
    public record Part(Vector3f position, Quaternionf rotation, float scale) {
        Part blend(Part to, float weight) {
            return new Part(new Vector3f(position).lerp(to.position, weight),
                    new Quaternionf(rotation).slerp(to.rotation, weight), Mth.lerp(weight, scale, to.scale));
        }
    }
    private record Clip(float[] times, List<Map<String, Part>> frames) {
        float duration() {
            return times[times.length - 1];
        }

        Map<String, Part> sample(float seconds) {
            int index = Arrays.binarySearch(times, seconds);
            if (index >= 0) return frames.get(index);
            int next = -index - 1;
            if (next == 0) return frames.get(0);
            if (next >= times.length) return frames.get(frames.size()-1);
            return blend(frames.get(next-1), frames.get(next), (seconds-times[next-1])/(times[next]-times[next-1]));
        }
    }
    static final class Playback {
        Object weapon;
        String key = "";
        float changed, lastSample = Float.NEGATIVE_INFINITY;
        Map<String, Part> previous = Map.of(), current = Map.of();
        ItemStack material = ItemStack.EMPTY;
        float previousLoading, currentLoading;

        void select(Object weapon, String key, float age) {
            boolean continuous = this.weapon == weapon && age >= lastSample && age - lastSample <= 2;
            if (!continuous || !key.equals(this.key)) {
                previous = continuous ? current : Map.of();
                previousLoading = currentLoading;
                this.weapon = weapon;
                this.key = key;
                changed = age;
            }
            lastSample = age;
        }
    }
    private final Map<String, Map<String, Clip>> models = new HashMap<>();
    private final Map<LivingEntity, Playback> playing = new WeakHashMap<>();
    private final Map<LivingEntity, Playback> firstPersonPlaying = new WeakHashMap<>();
    private final FirearmFirstPerson firstPerson = new FirearmFirstPerson();
    private net.minecraft.client.model.PlayerModel<LivingEntity> wideModel, slimModel;

    @Override public void onResourceManagerReload(ResourceManager resources) {
        models.clear(); playing.clear(); firstPersonPlaying.clear(); wideModel=null; slimModel=null;
        firstPerson.reload(resources);
        for (String gun : List.of("hand_cannon", "arquebus")) {
            var id = MinecraftVersionCompat.id("siegeworks", "firearm/animations/"+gun+".json");
            try (var reader = resources.getResourceOrThrow(id).openAsReader()) {
                Map<String, Clip> clips = new HashMap<>();
                for (var entry : JsonParser.parseReader(reader).getAsJsonObject().entrySet()) {
                    var json = entry.getValue().getAsJsonObject();
                    float[] times = new float[json.getAsJsonArray("times").size()];
                    for (int i=0;i<times.length;i++) times[i]=json.getAsJsonArray("times").get(i).getAsFloat();
                    List<Map<String, Part>> frames = new ArrayList<>();
                    for (var frame : json.getAsJsonArray("frames")) {
                        Map<String, Part> parts = new HashMap<>();
                        for (var p : frame.getAsJsonObject().entrySet()) {
                            var a = p.getValue().getAsJsonArray();
                            parts.put(p.getKey(), new Part(new Vector3f(a.get(0).getAsFloat(),a.get(1).getAsFloat(),a.get(2).getAsFloat()),
                                    new Quaternionf(a.get(3).getAsFloat(),a.get(4).getAsFloat(),a.get(5).getAsFloat(),a.get(6).getAsFloat()).normalize(),a.get(7).getAsFloat()));
                        }
                        frames.add(Map.copyOf(parts));
                    }
                    clips.put(entry.getKey(), new Clip(times,List.copyOf(frames)));
                }
                models.put(gun,Map.copyOf(clips));
            } catch (Exception e) { me.mss1r.siegeworks.Siegeworks.LOG.error("Cannot load firearm animation {}",id,e); }
        }
    }

    public static boolean holds(LivingEntity entity) {
        return entity.getMainHandItem().getItem() instanceof FirearmItem && !entity.isSwimming() && !entity.isFallFlying()
                && !me.mss1r.siegeworks.gameplay.ladder.LadderCarry.isCarrying(entity);
    }

    public void discardInactivePlayback() {
        playing.entrySet().removeIf(e -> !holds(e.getKey()) || e.getKey().getMainHandItem().getItem() != e.getValue().weapon);
        firstPersonPlaying.entrySet().removeIf(e -> !holds(e.getKey()) || e.getKey().getMainHandItem().getItem() != e.getValue().weapon);
    }

    private Map<String, Part> sample(LivingEntity entity, float age, boolean firstPerson) {
        float partial = age - entity.tickCount;
        float bodyYaw = FirearmClient.renderedBodyYaw(entity, partial,
                Mth.rotLerp(partial, entity.yBodyRotO, entity.yBodyRot));
        float headYaw = Mth.rotLerp(partial, entity.yHeadRotO, entity.yHeadRot);
        return sample(entity, age, firstPerson, entity.getViewXRot(partial), Mth.wrapDegrees(bodyYaw - headYaw));
    }

    private Map<String, Part> sample(LivingEntity entity, float age, boolean firstPerson, float pitch, float aimYaw) {
        FirearmItem gun = (FirearmItem) entity.getMainHandItem().getItem();
        var clips = models.get(gun.profile().getPath());
        if (clips == null) return Map.of();
        var tag = MinecraftVersionCompat.customData(entity.getMainHandItem());
        String action = entity.isUsingItem() ? tag.getString("FirearmAction") : "CARRY";
        if (!clips.containsKey(action)) action = "CARRY";
        long start = tag.getLong("FirearmActionStart");
        var playback = (firstPerson ? firstPersonPlaying : playing).computeIfAbsent(entity, ignored -> new Playback());
        String key = gun.profile()+"/"+action+"/"+(action.equals("CARRY") ? 0 : start);
        if (action.equals("CARRY")) key += "/" + entity.getOffhandItem().getItem();
        boolean actionChanged = !key.equals(playback.key);
        playback.select(gun, key, age);
        if (actionChanged) playback.material = entity.getOffhandItem().copy();
        float seconds = (entity.level().getGameTime()-start + (age-entity.tickCount))/20F;
        boolean handCannonShot = action.equals("FIRE") && !gun.matchlock();
        float clipSeconds = handCannonShot ? handCannonFireClipTime(seconds) : seconds;
        if (action.equals("FIRE")) {
            clipSeconds = recoilClipTime(clipSeconds, handCannonShot ? 1F : 0F, handCannonShot ? .10F : .125F);
        }
        Clip clip = clips.get(action);
        switch (action) {
            case "POWDER", "BALL", "RAM" -> {
                var operation = me.mss1r.siegeworks.gameplay.firearm.FirearmLoad.Operation.valueOf(action);
                clipSeconds *= clip.duration() * 20 / FirearmActions.duration(operation, gun.matchlock());
            }
        }
        Map<String, Part> result = clip.sample(clipSeconds);
        Map<String, Part> reference = clip.frames.get(0);
        if (handCannonShot) {
            result = anchorMainHand(result, clips.get("AIM").frames.get(0));
            result = ignite(result, seconds, gun.profile().getPath());
        }
        if (action.equals("CARRY")) result = attachHeldAccessory(result, entity.getOffhandItem());
        if (!firstPerson && action.equals("CARRY") && !entity.isPassenger()) {
            float partial = age - entity.tickCount;
            float phase = entity.walkAnimation.position(partial) * (entity.isBaby() ? 3 : 1);
            float speed = Math.min(1, entity.walkAnimation.speed(partial));
            float main = Mth.cos(phase * .6662F + (entity.getMainArm() == HumanoidArm.RIGHT ? Mth.PI : 0)) * speed;
            result = walk(result, main * .5F, -main * (entity.getOffhandItem().isEmpty() ? 1 : .5F));
        }
        if (firstPerson && action.equals("BALL")) result = alignBall(result, gun.profile().getPath());
        String view = "";
        if (firstPerson) {
            view = action.equals("AIM") || action.equals("FIRE") ? "aim"
                    : action.equals("CARRY") ? "ready" : action.equals("LIGHT_MATCH") ? "ignition" : "loading";
            result = this.firstPerson.place(gun.profile().getPath(), view, result, reference.get("weapon"));
        } else if (FirearmClient.aiming(entity)) {
            float support = handCannonShot ? 1 - ignitionReach(seconds) : 1;
            float yaw = entity.getMainArm() == HumanoidArm.LEFT ? -aimYaw : aimYaw;
            result = aim(result, pitch, 0, yaw, support);
        }
        // The matchlock fires immediately. A crossfade here would swallow the kick.
        float progress = action.equals("FIRE") && gun.matchlock() ? 1 : Mth.clamp((age-playback.changed)/6F,0,1);
        progress = progress*progress*(3-2*progress);
        if (!playback.previous.isEmpty() && progress<1) result=blend(playback.previous,result,progress);
        playback.current = result;
        if (firstPerson) {
            float loading = view.equals("loading") ? 1 : 0;
            playback.currentLoading = playback.previous.isEmpty() ? loading : Mth.lerp(progress, playback.previousLoading, loading);
            result = this.firstPerson.hands(result, !action.equals("CARRY"), playback.currentLoading);
        }
        else if (action.equals("BALL")) result = alignBall(result, gun.profile().getPath());
        return result;
    }

    static Map<String, Part> anchorMainHand(Map<String, Part> frame, Map<String, Part> aim) {
        Part restingArm = aim.get("right_arm"), restingGun = aim.get("weapon"), gun = frame.get("weapon");
        Vector3f grip = restingArm.rotation.transform(new Vector3f(-1,-9,0)).add(restingArm.position);
        Vector3f local = new Quaternionf(restingGun.rotation).conjugate()
                .transform(new Vector3f(grip).sub(restingGun.position)).div(restingGun.scale);
        Vector3f target = gun.rotation.transform(local.mul(gun.scale)).add(gun.position);
        Part arm = reach(restingArm, grip, target);
        Vector3f correction = arm.rotation.transform(new Vector3f(-1,-9,0)).add(arm.position).sub(target);
        var result = new HashMap<>(frame);
        result.put("right_arm", arm);
        // Keep the authored recoil rotation, but constrain the stock to the same
        // grip throughout the shot. The shoulder and arm length stay unchanged.
        for (String name : List.of("weapon", "trigger")) {
            Part part = frame.get(name);
            if (part != null) result.put(name, new Part(new Vector3f(part.position).add(correction), part.rotation, part.scale));
        }
        return result;
    }

    private static float handCannonFireClipTime(float seconds) {
        float strike = FirearmActions.HAND_CANNON_IGNITION_TICK / 20F;
        // The authored clip recoils at one second. Hold its pre-shot pose while the fuse burns.
        float extra = FirearmActions.HAND_CANNON_SHOT_TICK / 20F - 1F;
        return seconds <= strike ? seconds : Math.max(strike, seconds - extra);
    }

    private static float recoilClipTime(float seconds, float shot, float authoredPeak) {
        float elapsed = seconds - shot;
        if (elapsed <= 0) return seconds;
        float peak = .05F;
        if (elapsed <= peak) return shot + elapsed / peak * authoredPeak;
        // Retiming the attack preserves the end of the authored 1.6-second recovery.
        return shot + authoredPeak + (elapsed - peak) * ((1.6F - authoredPeak) / (1.6F - peak));
    }

    private static float ignitionReach(float seconds) {
        float strike = FirearmActions.HAND_CANNON_IGNITION_TICK / 20F;
        float withdrawal = .7F;
        float weight = seconds < strike ? Mth.clamp(seconds / strike, 0, 1)
                : Mth.clamp(1 - (seconds - strike) / withdrawal, 0, 1);
        return weight * weight * (3 - 2 * weight);
    }

    private static Map<String, Part> ignite(Map<String, Part> frame, float seconds, String model) {
        float weight = ignitionReach(seconds);
        var socket = FirearmMesh.INSTANCE.ignitionSocket(model);
        if (socket == null || weight == 0) return frame;
        Part gun = frame.get("weapon"), arm = frame.get("left_arm");
        Vector3f grip = arm.rotation.transform(new Vector3f(1,-9,0)).add(arm.position);
        // Bring the igniter beside the touch hole, strike, then withdraw before recoil.
        Vector3f target = gun.rotation.transform(socket.add(4,-2,0).mul(gun.scale)).add(gun.position);
        var result = new HashMap<>(frame);
        result.put("left_arm", reach(arm, grip, new Vector3f(grip).lerp(target,weight)));
        return result;
    }

    /** Apply the same shoulder transform to the limb and everything held in that hand. */
    static Map<String, Part> walk(Map<String, Part> frame, float mainAngle, float offAngle) {
        var result = new HashMap<>(frame);
        swing(result, frame, "right_arm", mainAngle, "weapon", "trigger");
        swing(result, frame, "left_arm", offAngle, "ramrod", "component", "offhand");
        return result;
    }

    private static void swing(Map<String, Part> result, Map<String, Part> frame, String armName, float angle, String... held) {
        Part arm = frame.get(armName);
        Quaternionf rotation = new Quaternionf().rotationX(angle);
        result.put(armName, new Part(arm.position, new Quaternionf(rotation).mul(arm.rotation), arm.scale));
        for (String name : held) {
            Part part = frame.get(name);
            if (part != null) result.put(name, new Part(
                    rotation.transform(new Vector3f(part.position).sub(arm.position)).add(arm.position),
                    new Quaternionf(rotation).mul(part.rotation), part.scale));
        }
    }

    static Map<String,Part> aim(Map<String,Part> frame,float pitch,float headYaw,float bodyYaw,float support) {
        float yaw = Mth.wrapDegrees(bodyYaw - headYaw);
        Part gun=frame.get("weapon"), right=frame.get("right_arm"), left=frame.get("left_arm");
        Quaternionf look=new Quaternionf().rotationYXZ(yaw*Mth.DEG_TO_RAD,pitch*Mth.DEG_TO_RAD,0);
        Vector3f rightGrip=right.rotation.transform(new Vector3f(-1,-9,0)).add(right.position);
        Vector3f leftGrip=left.rotation.transform(new Vector3f(1,-9,0)).add(left.position);
        Vector3f rightOffset=look.transform(new Vector3f(rightGrip).sub(gun.position));
        Vector3f cradle = gun.rotation.transform(new Vector3f(1.6F, -1.1F, .8F).mul(support));
        Vector3f leftOffset=look.transform(new Vector3f(leftGrip).sub(gun.position).add(cradle));
        Vector3f shoulderMid=new Vector3f(right.position).add(left.position).mul(.5F);
        Vector3f desired=look.transform(new Vector3f(gun.position).add(2, 0, 0).sub(shoulderMid)).add(shoulderMid);
        // Square the shoulders around the waist before solving the two hand contacts.
        Part torso = frame.get("torso");
        Vector3f waist = torso.rotation.transform(new Vector3f(0, -12, 0)).add(torso.position);
        Quaternionf turn = new Quaternionf().rotationY(15 * Mth.DEG_TO_RAD);
        float rightReach = rightGrip.distance(right.position), leftReach = leftGrip.distance(left.position);
        right = turnAround(right, waist, turn);
        left = turnAround(left, waist, turn);
        rightGrip = right.rotation.transform(new Vector3f(-1, -9, 0)).add(right.position);
        leftGrip = left.rotation.transform(new Vector3f(1, -9, 0)).add(left.position);
        Vector3f position=FirearmAim.position(new Vector3f(right.position).sub(rightOffset),
                new Vector3f(left.position).sub(leftOffset),rightReach,leftReach,desired);
        Map<String,Part> result=new HashMap<>(frame);
        result.put("torso", turnAround(torso, waist, turn));
        result.put("head", turnAround(frame.get("head"), waist, turn));
        for(String name:List.of("weapon","trigger","component","ramrod")) {
            Part part=frame.get(name);
            if(part!=null)result.put(name,new Part(look.transform(new Vector3f(part.position).sub(gun.position)).add(position),
                    new Quaternionf(look).mul(part.rotation),part.scale));
        }
        result.put("right_arm",reach(right,rightGrip,new Vector3f(position).add(rightOffset)));
        result.put("left_arm",reach(left,leftGrip,new Vector3f(position).add(leftOffset)));
        return result;
    }

    private static Part turnAround(Part part, Vector3f pivot, Quaternionf rotation) {
        return new Part(rotation.transform(new Vector3f(part.position).sub(pivot)).add(pivot),
                new Quaternionf(rotation).mul(part.rotation), part.scale);
    }

    private static Part reach(Part arm,Vector3f oldGrip,Vector3f newGrip) {
        Quaternionf delta=new Quaternionf().rotationTo(new Vector3f(oldGrip).sub(arm.position),
                new Vector3f(newGrip).sub(arm.position));
        return new Part(arm.position,delta.mul(arm.rotation),arm.scale);
    }

    private static Map<String, Part> blend(Map<String, Part> from, Map<String, Part> to, float weight) {
        Map<String, Part> result = new HashMap<>();
        to.forEach((name, part) -> result.put(name,from.getOrDefault(name,part).blend(part,weight)));
        if (from.containsKey("trigger") || to.containsKey("trigger")) {
            Part fromGun = from.get("weapon"), toGun = to.get("weapon");
            Part fromTrigger = relativeTo(from.getOrDefault("trigger", fromGun), fromGun);
            Part toTrigger = relativeTo(to.getOrDefault("trigger", toGun), toGun);
            result.put("trigger", attach(fromTrigger.blend(toTrigger, weight), result.get("weapon")));
        }
        // Blend accessories in hand space: world-space interpolation lets a tool leave the grip.
        for (String name : List.of("ramrod", "component")) {
            if (from.containsKey(name) && to.containsKey(name) && result.containsKey("left_arm")) {
                Part local = relativeTo(from.get(name), from.get("left_arm"))
                        .blend(relativeTo(to.get(name), to.get("left_arm")), weight);
                result.put(name, attach(local, result.get("left_arm")));
            }
        }
        return result;
    }

    private static Part relativeTo(Part part, Part parent) {
        Quaternionf inverse = new Quaternionf(parent.rotation).conjugate();
        return new Part(inverse.transform(new Vector3f(part.position).sub(parent.position)),
                new Quaternionf(inverse).mul(part.rotation), part.scale);
    }

    private static Part attach(Part local, Part arm) {
        return new Part(arm.rotation.transform(new Vector3f(local.position)).add(arm.position),
                new Quaternionf(arm.rotation).mul(local.rotation), local.scale);
    }

    private static String accessoryAction(ItemStack held) {
        return held.is(SiegeworksItems.FIREARM_RAMROD.get()) ? "RAM"
                : held.getItem() instanceof RoundShotItem ? "BALL"
                : held.is(SiegeworksItems.BLACK_POWDER.get()) ? "POWDER" : null;
    }

    private static Map<String, Part> attachHeldAccessory(Map<String, Part> frame, ItemStack held) {
        if (frame.containsKey("ramrod") || frame.containsKey("component")) return frame;
        String action = accessoryAction(held);
        if (action == null) return frame;
        String part = action.equals("RAM") ? "ramrod" : "component";
        var result = new HashMap<>(frame);
        Part arm = frame.get("left_arm");
        // Resting tools pass through the fist, perpendicular to the forearm.
        // Loading clips own their separate tool path and blend from this hand attachment.
        Part local = new Part(new Vector3f(1, -9, action.equals("RAM") ? -6 : 0), new Quaternionf(), 1);
        result.put(part, attach(local, arm));
        return result;
    }

    private static Map<String, Part> alignBall(Map<String, Part> frame, String name) {
        var socket = FirearmMesh.INSTANCE.loadingSocket(name);
        Part gun = frame.get("weapon"), ball = frame.get("component");
        if (socket == null || ball == null || ball.scale <= 0) return frame;
        Vector3f muzzle = gun.rotation.transform(new Vector3f(socket.muzzle()).mul(gun.scale)).add(gun.position);
        Vector3f axis = gun.rotation.transform(new Vector3f(0,0,1));
        // The rigid Minecraft hand follows an arc. Let the ball roll to the bore axis
        // during the last short push, while the same hand continues the authored gesture.
        Vector3f position = FirearmLoadingFit.position(ball.position, muzzle, axis);
        var result = new HashMap<>(frame);
        result.put("component", new Part(position, ball.rotation, ball.scale));
        Part arm = frame.get("left_arm");
        result.put("left_arm", reach(arm, ball.position, position));
        return result;
    }

    public boolean pose(HumanoidModel<?> model, LivingEntity entity, float age, float pitch) {
        if (!holds(entity)) return false;
        var frame=sample(entity,age,false);
        if (frame.isEmpty()) return false;
        applyFrame(model, entity, frame);
        return true;
    }

    /** Equipment in a Y-up armature frame, independent of vanilla player-model pivots. */
    public void renderArmatureHand(LivingEntity entity, HumanoidArm hand, PoseStack poses, MultiBufferSource buffers,
                                  int light, float partialTick, Map<String, Part> frame, Vector3f offset, ItemStack material) {
        poses.pushPose();
        poses.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
        if (entity.getMainArm() == HumanoidArm.LEFT) poses.scale(-1, 1, 1);
        poses.translate(entity.getMainArm() == HumanoidArm.LEFT ? offset.x : -offset.x, offset.y, -offset.z);
        renderEquipment(entity, hand, poses, buffers, light, partialTick, frame, material, true);
        poses.popPose();
    }

    private static void applyFrame(HumanoidModel<?> model, LivingEntity entity, Map<String,Part> frame) {
        boolean left=entity.getMainArm()==HumanoidArm.LEFT;
        float rightWalk=model.rightLeg.xRot, leftWalk=model.leftLeg.xRot;
        apply(model.body,frame.get("torso"),left);
        apply(left ? model.leftArm : model.rightArm,frame.get("right_arm"),left);
        apply(left ? model.rightArm : model.leftArm,frame.get("left_arm"),left);
        apply(left ? model.leftLeg : model.rightLeg,frame.get("right_leg"),left);
        apply(left ? model.rightLeg : model.leftLeg,frame.get("left_leg"),left);
        model.rightLeg.xRot += rightWalk;
        model.leftLeg.xRot += leftWalk;
        // Vanilla look remains free, but its neck must follow the posed torso.
        Part head=frame.get("head");
        model.head.setPos(left ? -head.position.x : head.position.x,24-head.position.y,-head.position.z);
        model.hat.copyFrom(model.head);
    }

    private static void apply(ModelPart target, Part part, boolean left) {
        Vector3f p=new Vector3f(part.position).sub(0,24,0);
        Quaternionf q=new Quaternionf(part.rotation);
        // Study coordinates: Y up, Z forward. Vanilla model coordinates: Y down, Z back.
        q.set(q.x,-q.y,-q.z,q.w);
        if (left) q.set(q.x,-q.y,-q.z,q.w);
        Vector3f angles=q.getEulerAnglesZYX(new Vector3f());
        target.setPos(left ? -p.x : p.x,-p.y,-p.z);
        target.setRotation(angles.x,angles.y,angles.z);
    }

    /** Called at the layer root, before vanilla applies the hand/item transforms. */
    public boolean renderHeld(LivingEntity entity, HumanoidArm hand, PoseStack pose, MultiBufferSource buffers, int light, float partialTick) {
        if (!holds(entity)) return false;
        if (hand != entity.getMainArm() && !entity.isUsingItem()
                && !entity.getOffhandItem().is(SiegeworksItems.FIREARM_RAMROD.get())) return false;
        var frame=sample(entity,entity.tickCount+partialTick,false);
        return renderHeld(entity, hand, pose, buffers, light, partialTick, frame);
    }

    private boolean renderHeld(LivingEntity entity, HumanoidArm hand, PoseStack pose, MultiBufferSource buffers, int light, float partialTick, Map<String,Part> frame) {
        if (frame.isEmpty()) return false;
        pose.pushPose();
        pose.translate(0,1.5,0); pose.scale(1,-1,-1);
        if (entity.getMainArm()==HumanoidArm.LEFT) pose.scale(-1,1,1);
        renderEquipment(entity,hand,pose,buffers,light,partialTick,frame,playing.get(entity));
        pose.popPose(); return true;
    }

    private void renderEquipment(LivingEntity entity, HumanoidArm hand, PoseStack pose, MultiBufferSource buffers,
                                 int light, float partialTick, Map<String,Part> frame, Playback playback) {
        renderEquipment(entity, hand, pose, buffers, light, partialTick, frame,
                playback == null ? null : playback.material, entity.isUsingItem());
    }

    private void renderEquipment(LivingEntity entity, HumanoidArm hand, PoseStack pose, MultiBufferSource buffers,
                                 int light, float partialTick, Map<String,Part> frame, ItemStack material,
                                 boolean useComponentPose) {
        FirearmItem item=(FirearmItem) entity.getMainHandItem().getItem();
        if (hand != entity.getMainArm() && FirearmClient.aiming(entity)) {
            var tag = MinecraftVersionCompat.customData(entity.getUseItem());
            boolean lighting = !item.matchlock() && tag.getString("FirearmAction").equals("FIRE")
                    && entity.level().getGameTime() - tag.getLong("FirearmActionStart") < FirearmActions.HAND_CANNON_SHOT_TICK;
            if (!lighting) return;
        }
        pose.pushPose();
        if (hand==entity.getMainArm()) {
            pose.pushPose();
            transform(pose,frame.get("weapon"));
            Part trigger=frame.get("trigger");
            FirearmMesh.INSTANCE.render(item.profile().getPath(),pose,buffers,light,OverlayTexture.NO_OVERLAY,
                    name->trigger==null || !name.equals("trigger"));
            FirearmEffects.render(entity, item, pose, buffers, partialTick);
            pose.popPose();
            if(trigger!=null) {
                transform(pose,trigger);
                FirearmMesh.INSTANCE.render(item.profile().getPath(),pose,buffers,light,OverlayTexture.NO_OVERLAY,"trigger"::equals);
            }
        } else {
            var rod=frame.get("ramrod");var component=frame.get("component");
            if (rod!=null) {
                transform(pose,rod);
                FirearmMesh.INSTANCE.render("firearm_loading_ramrod",pose,buffers,light,OverlayTexture.NO_OVERLAY);
            } else {
                boolean usingComponent = useComponentPose && component != null;
                ItemStack held=usingComponent && material!=null ? material : entity.getOffhandItem();
                if (!held.isEmpty() && (component==null || component.scale>.0001F)) {
                    if (component != null && held.getItem() instanceof RoundShotItem) {
                        renderBall(held, component, frame.get("weapon"), item, pose, buffers, light, entity);
                    } else {
                    if (usingComponent) {
                        transform(pose,component); pose.scale(.5F,.5F,.5F);
                        Minecraft.getInstance().getItemRenderer().renderStatic(held,ItemDisplayContext.GROUND,light,
                                OverlayTexture.NO_OVERLAY,pose,buffers,entity.level(),entity.getId());
                    } else {
                        // Match vanilla's wrist attachment and use the item's hand display,
                        // rather than shrinking a FIXED/frame model into the fist.
                        transform(pose,frame.get("left_arm"));
                        pose.scale(1,-1,-1);
                        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90));
                        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
                        pose.translate(-1/16F,.125F,-.625F);
                        Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(
                                entity, held, ItemDisplayContext.THIRD_PERSON_LEFT_HAND,true,pose,buffers,light);
                    }
                    }
                }
            }
        }
        pose.popPose();
    }

    private static void renderBall(ItemStack held, Part ball, Part gun, FirearmItem item, PoseStack pose,
                                   MultiBufferSource buffers, int light, LivingEntity entity) {
        float diameter = (float) (10 * RoundShotSize.scale(RoundShotItem.caliber(held), 1));
        float displayDiameter = diameter * .4F;
        var socket = FirearmMesh.INSTANCE.loadingSocket(item.profile().getPath());
        if (socket != null && entity.isUsingItem()
                && MinecraftVersionCompat.customData(entity.getUseItem()).getString("FirearmAction").equals("BALL")) {
            Vector3f muzzle = gun.rotation.transform(new Vector3f(socket.muzzle()).mul(gun.scale)).add(gun.position);
            displayDiameter = FirearmLoadingFit.diameter(ball.position.distance(muzzle), displayDiameter, socket.diameter()*gun.scale);
        }
        transform(pose, new Part(ball.position, gun.rotation, ball.scale));
        float scale = displayDiameter / diameter;
        pose.scale(scale,scale,scale);
        // ItemRenderer centers its model at (8,8,8); round-shot geometry rests on Y=0.
        // NONE bypasses inventory/frame presentation transforms during physical insertion.
        pose.translate(0, .5F-diameter/32F, 0);
        Minecraft.getInstance().getItemRenderer().renderStatic(held,ItemDisplayContext.NONE,light,
                OverlayTexture.NO_OVERLAY,pose,buffers,entity.level(),entity.getId());
    }

    private static void transform(PoseStack pose, Part part) {
        pose.translate(part.position.x/16,part.position.y/16,part.position.z/16);
        pose.mulPose(part.rotation);pose.scale(part.scale,part.scale,part.scale);
    }

    public boolean renderFirstPerson(net.minecraft.client.player.LocalPlayer player, PoseStack poses,
                                     MultiBufferSource buffers, int light, float partialTick) {
        if (!holds(player) || !models.containsKey(((FirearmItem)player.getMainHandItem().getItem()).profile().getPath())) return false;
        var minecraft=Minecraft.getInstance();
        if (minecraft.getCameraEntity() != player || !minecraft.options.getCameraType().isFirstPerson()) return false;
        //? if <1.21 {
        /*boolean slim=player.getModelName().equals("slim");
        var skin=player.getSkinTextureLocation();
        *///?} else {
        boolean slim=player.getSkin().model()==net.minecraft.client.resources.PlayerSkin.Model.SLIM;
        var skin=player.getSkin().texture();
        //?}
        if (wideModel==null) wideModel=new net.minecraft.client.model.PlayerModel<>(minecraft.getEntityModels().bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER),false);
        if (slimModel==null) slimModel=new net.minecraft.client.model.PlayerModel<>(minecraft.getEntityModels().bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER_SLIM),true);
        var model=slim ? slimModel : wideModel;
        var frame=sample(player,player.tickCount+partialTick,true);
        if(frame.isEmpty())return false;
        poses.pushPose();
        // View-space rig: origin at the camera, Y up, Z forward. No player height,
        // body pose or world pitch is applied to the first-person model.
        poses.scale(player.getMainArm()==HumanoidArm.LEFT ? 1 : -1,1,-1);
        var buffer=buffers.getBuffer(net.minecraft.client.renderer.RenderType.entityCutoutNoCull(skin));
        if (!player.isInvisible()) {
            boolean left=player.getMainArm()==HumanoidArm.LEFT;
            renderViewArm(left ? model.leftArm : model.rightArm,left ? model.leftSleeve : model.rightSleeve,
                    frame.get("right_arm"),poses,buffer,light,left,slim,
                    player.isModelPartShown(left ? net.minecraft.world.entity.player.PlayerModelPart.LEFT_SLEEVE : net.minecraft.world.entity.player.PlayerModelPart.RIGHT_SLEEVE));
            if (player.isUsingItem() || player.getOffhandItem().is(SiegeworksItems.FIREARM_RAMROD.get())) renderViewArm(left ? model.rightArm : model.leftArm,left ? model.rightSleeve : model.leftSleeve,
                    frame.get("left_arm"),poses,buffer,light,!left,slim,
                    player.isModelPartShown(left ? net.minecraft.world.entity.player.PlayerModelPart.RIGHT_SLEEVE : net.minecraft.world.entity.player.PlayerModelPart.LEFT_SLEEVE));
        }
        renderEquipment(player,player.getMainArm(),poses,buffers,light,partialTick,frame,firstPersonPlaying.get(player));
        if (player.isUsingItem() || player.getOffhandItem().is(SiegeworksItems.FIREARM_RAMROD.get()))
            renderEquipment(player,player.getMainArm().getOpposite(),poses,buffers,light,partialTick,frame,firstPersonPlaying.get(player));
        poses.popPose();return true;
    }

    private static void renderViewArm(ModelPart arm, ModelPart sleeve, Part part, PoseStack pose,
                                      com.mojang.blaze3d.vertex.VertexConsumer buffer, int light,
                                      boolean leftMesh, boolean slim, boolean showSleeve) {
        pose.pushPose();
        transform(pose,part);
        pose.scale(1,-1,-1);
        // Grip coordinates use a centred fist; skin models have side-dependent pivots.
        pose.translate((leftMesh ? -1 : 1)*(slim ? .5F : 1F)/16,0,0);
        arm.setPos(0,0,0);arm.setRotation(0,0,0);
        sleeve.setPos(0,0,0);sleeve.setRotation(0,0,0);
        arm.render(pose,buffer,light,OverlayTexture.NO_OVERLAY);
        if(showSleeve)sleeve.render(pose,buffer,light,OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
