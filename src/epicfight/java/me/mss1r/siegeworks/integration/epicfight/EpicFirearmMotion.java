package me.mss1r.siegeworks.integration.epicfight;

import me.mss1r.siegeworks.client.firearm.FirearmAnimation.Part;
import me.mss1r.siegeworks.client.firearm.FirearmMesh;
import me.mss1r.siegeworks.gameplay.firearm.FirearmActions;
import me.mss1r.siegeworks.gameplay.firearm.FirearmLoad.Operation;
import me.mss1r.siegeworks.item.FirearmItem;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import me.mss1r.siegeworks.registry.SiegeworksItems;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** Poses authored for EF. Contacts are in a right-handed stance, in blocks, with +Z forward. */
final class EpicFirearmMotion {
    private static final Vector3f CANNON_GRIP = new Vector3f(-1.6F, .1F, 1.1F);
    private static final Vector3f ARQUEBUS_GRIP = new Vector3f(-1.9F, -1, -11.1F);
    private static final Vector3f ARQUEBUS_LOAD_GRIP = new Vector3f(-1.75F, -1.8F, 8.2F);
    private static final Vector3f CANNON_LOAD_GRIP = new Vector3f(-1.55F, -1.1F, 6.6F);
    private static final Vector3f ARQUEBUS_BUTT = new Vector3f(0, -1.8F, -20.15F);
    private static final Vector3f ARQUEBUS_SUPPORT = new Vector3f(1.7F, -2.65F, -4.6F);
    private static final Vector3f CANNON_SUPPORT = new Vector3f(1.0F, -1.9F, 4.8F);
    private static final Vector3f RELAXED_HAND = new Vector3f(.37F, .88F, .08F);
    private static final float ROD_SCALE = 1;
    static final float LOAD_VIEW_FORWARD = .24F;
    static final float LOAD_VIEW_DROP = .13F;
    private static final Map<LivingEntity, Playback> BODY_PLAYBACK = new WeakHashMap<>();
    private static final Map<LivingEntity, Playback> VIEW_PLAYBACK = new WeakHashMap<>();

    private EpicFirearmMotion() {}

    record Frame(Part gun, Vector3f mainGrip, Vector3f support, Part trigger, Prop prop,
                 float turn, float lean, float loading, float relaxedOffhand, boolean showOffhand) {
        Map<String, Part> equipment() {
            var parts = new HashMap<String, Part>();
            parts.put("weapon", gun);
            if (trigger != null) parts.put("trigger", trigger);
            if (prop != null) parts.put(prop.name, prop.part);
            return parts;
        }

        ItemStack material() { return prop == null ? ItemStack.EMPTY : prop.material; }
    }

    private record Prop(String name, Part part, ItemStack material) {
        Prop follow(Vector3f movement) {
            return new Prop(name, new Part(new Vector3f(part.position()).fma(16, movement),
                    part.rotation(), part.scale()), material);
        }
    }

    private static final class Playback {
        FirearmItem weapon;
        String action;
        long start;
        float changed;
        float lastSample;
        ItemStack material = ItemStack.EMPTY;
        Frame previous;
        Frame last;
    }

    static boolean reloadHandoff(LivingEntity entity, String action, boolean firstPerson) {
        if (!action.isEmpty() || !(entity.getMainHandItem().getItem() instanceof FirearmItem weapon)) return false;
        var playback = (firstPerson ? VIEW_PLAYBACK : BODY_PLAYBACK).get(entity);
        if (playback == null || playback.weapon != weapon || playback.last == null) return false;
        var operation = switch (playback.action) {
            case "POWDER" -> Operation.POWDER;
            case "BALL" -> Operation.BALL;
            default -> null;
        };
        if (operation == null) return false;
        long elapsed = entity.level().getGameTime() - playback.start;
        int end = FirearmActions.duration(operation, weapon.matchlock());
        return elapsed >= end - 1 && elapsed <= end + 8;
    }

    static Frame sample(LivingEntity entity, String action, boolean firstPerson, float pitch, float yaw, float partialTick) {
        FirearmItem weapon = (FirearmItem) entity.getMainHandItem().getItem();
        var playback = (firstPerson ? VIEW_PLAYBACK : BODY_PLAYBACK).computeIfAbsent(entity, ignored -> new Playback());
        if (reloadHandoff(entity, action, firstPerson)) {
            var operation = Operation.valueOf(playback.action);
            playback.last = load(weapon, operation, FirearmActions.duration(operation, weapon.matchlock()),
                    firstPerson, playback.material);
            playback.lastSample = entity.tickCount + partialTick;
            return playback.last;
        }
        var tag = MinecraftVersionCompat.customData(entity.getMainHandItem());
        if (action.isEmpty()) action = "CARRY";
        long start = action.equals("CARRY") ? 0 : tag.getLong("FirearmActionStart");
        float now = entity.tickCount + partialTick;
        float age = Math.max(0, entity.level().getGameTime() - start + partialTick);
        float shot = tag.contains("FirearmShotAt")
                ? entity.level().getGameTime() - tag.getLong("FirearmShotAt") + partialTick : -1;

        ItemStack material = loadingMaterial(action, entity.getOffhandItem());
        boolean continuous = playback.weapon == weapon && Math.abs(now - playback.lastSample) <= 3;
        if (!continuous || !action.equals(playback.action) || start != playback.start) {
            playback.previous = continuous ? playback.last : null;
            playback.weapon = weapon;
            playback.action = action;
            playback.start = start;
            playback.changed = now;
            playback.material = material.copy();
        } else if (playback.material.isEmpty() && !material.isEmpty()) {
            playback.material = material.copy();
        }
        playback.lastSample = now;

        Frame target = switch (action) {
            case "CARRY" -> viewCarry(entity, weapon, partialTick);
            case "AIM", "FIRE" -> aim(weapon, firstPerson, pitch, yaw, action.equals("FIRE"), age, shot);
            case "POWDER", "BALL", "RAM" -> load(weapon, Operation.valueOf(action), age, firstPerson,
                    playback.material);
            case "INSERT_MATCH", "LIGHT_MATCH", "EXTINGUISH_MATCH" -> match(weapon, Operation.valueOf(action), age,
                    firstPerson);
            default -> throw new IllegalArgumentException("No Epic Fight firearm action: " + action);
        };
        // A shot from the matchlock is immediate. Keep the first recoil frame.
        boolean shoulderStance = !firstPerson && weapon.matchlock() && (action.equals("AIM") || action.equals("FIRE"));
        float blend = shoulderStance || action.equals("FIRE") && weapon.matchlock()
                ? 1 : ease(now - playback.changed, 0, 5);
        Frame result = playback.previous == null || blend == 1 ? target : blend(playback.previous, target, blend);
        playback.last = result;
        return result;
    }

    private static ItemStack loadingMaterial(String action, ItemStack offhand) {
        return switch (action) {
            case "POWDER" -> offhand.is(SiegeworksItems.BLACK_POWDER.get()) ? offhand : ItemStack.EMPTY;
            case "BALL" -> offhand.is(SiegeworksItems.CANNON_BALL.get()) ? offhand : ItemStack.EMPTY;
            default -> ItemStack.EMPTY;
        };
    }

    private static Frame viewCarry(LivingEntity entity, FirearmItem item, float partialTick) {
        float gait = Math.min(entity.walkAnimation.speed(partialTick), 1);
        float swing = Mth.cos(entity.walkAnimation.position(partialTick) * .6662F) * gait;
        Vector3f main = mainGrip(item);
        Vector3f grip = new Vector3f(-.39F, 1.12F - .018F * Math.abs(swing), .35F + .06F * swing);
        Part gun = place(grip, rotation(item.matchlock() ? 9 : 6, (item.matchlock() ? -9 : 3) + 7 * swing, -7),
                main, scale(item));
        return new Frame(gun, main, new Vector3f(.46F, .89F, .15F), null, null, 0, 0, 0, 1, true);
    }

    private static Frame aim(FirearmItem item, boolean firstPerson, float pitch, float yaw, boolean firing,
                             float age, float shotAge) {
        boolean matchlock = item.matchlock();
        float recoil = firing && shotAge >= 0 ? ease(shotAge, 0, 1) * (1 - ease(shotAge, 1, 13)) : 0;
        Quaternionf heading = rotation(yaw, 0, 0);
        Quaternionf orientation = rotation(yaw, pitch - recoil * (matchlock ? 7 : 10), recoil * 2);
        Vector3f main = mainGrip(item);
        Part gun;
        if (matchlock) {
            // The butt is the pivot, so raising the muzzle or firing keeps it on the shoulder.
            Vector3f shoulder = firstPerson ? new Vector3f(-.115F, 1.42F, -.045F)
                    : new Vector3f(-.29F, 1.415F, -.15F);
            shoulder.z -= .035F * recoil;
            gun = place(heading.transform(shoulder), orientation, ARQUEBUS_BUTT, scale(item));
        } else {
            Vector3f grip = new Vector3f(firstPerson ? -.21F : -.31F, firstPerson ? 1.40F : 1.39F, .34F);
            grip.z -= .065F * recoil;
            gun = place(heading.transform(grip), orientation, main, scale(item));
        }
        Vector3f support = point(gun, matchlock ? ARQUEBUS_SUPPORT : CANNON_SUPPORT);
        boolean ignite = !matchlock && firing && age < FirearmActions.HAND_CANNON_SHOT_TICK;
        if (ignite) {
            Vector3f socket = FirearmMesh.INSTANCE.ignitionSocket(item.profile().getPath());
            if (socket != null) {
                float strike = FirearmActions.HAND_CANNON_IGNITION_TICK;
                float reach = ease(age, 0, strike) * (1 - ease(age, strike, strike + 9));
                support.lerp(point(gun, socket).add(.13F, .07F, -.02F), reach);
            }
        }
        Part trigger = matchlock && firing ? trigger(gun, ease(age, 0, 1) * (1 - ease(age, 3, 9))) : null;
        return new Frame(gun, main, support, trigger, null, (matchlock ? 35 : 17) - yaw, 5 - 8 * recoil,
                0, 0, ignite);
    }

    private static Frame load(FirearmItem item, Operation operation, float age, boolean firstPerson, ItemStack material) {
        boolean matchlock = item.matchlock();
        int duration = FirearmActions.duration(operation, matchlock);
        float t = Mth.clamp(age / duration, 0, 1);
        float commit = FirearmActions.commitTick(operation, matchlock) / (float) duration;
        Vector3f main = matchlock ? ARQUEBUS_LOAD_GRIP : CANNON_LOAD_GRIP;
        Vector3f right = new Vector3f(-.37F, 1.02F, .34F);
        if (firstPerson) right.add(0, -LOAD_VIEW_DROP, LOAD_VIEW_FORWARD);
        Part gun = place(right, rotation(firstPerson ? -14 : 16, -67, 7), main, scale(item));
        var socket = FirearmMesh.INSTANCE.loadingSocket(item.profile().getPath());
        if (socket == null) return new Frame(gun, main, null, null, null, 15, 4, 1, 0, true);
        Vector3f muzzle = point(gun, socket.muzzle());
        Vector3f axis = gun.rotation().transform(new Vector3f(0, 0, 1));
        Vector3f rest = new Vector3f(.35F, .96F, .16F);
        if (firstPerson) rest.add(0, -LOAD_VIEW_DROP, LOAD_VIEW_FORWARD);
        Prop prop;
        Vector3f support;

        switch (operation) {
            case POWDER -> {
                float approach = ease(t, .06F, .39F) * (1 - ease(t, .77F, 1));
                float pour = ease(t, .34F, .50F) * (1 - ease(t, .65F, .79F));
                float shake = Mth.sin(age * .8F) * pour;
                Vector3f pouringHand = new Vector3f(muzzle).add(.145F, .07F + .021F * shake, .025F);
                if (firstPerson) pouringHand.z += .08F;
                support = new Vector3f(rest).lerp(pouringHand, approach);
                Vector3f pouch = new Vector3f(support).add(-.115F, .04F, firstPerson ? -.10F : -.025F);
                float visible = ease(t, 0, .12F) * (1 - ease(t, .70F, .90F));
                prop = new Prop("component", at(pouch, rotation(0, -18, -65 * pour + 9 * shake), .46F * visible), material);
            }
            case BALL -> {
                float approach = ease(t, .08F, .39F);
                float insertion = ease(t, .40F, commit);
                Vector3f ballPosition = new Vector3f(rest).add(-.055F, .05F, 0)
                        .lerp(new Vector3f(muzzle).fma(.115F * (1 - insertion), axis), approach);
                support = new Vector3f(ballPosition).add(.055F + .05F * approach,
                        -.05F + .035F * approach, -.025F * approach).lerp(rest, ease(t, commit, .94F));
                float visible = ease(t, 0, .11F) * (1 - ease(t, commit - .035F, commit));
                prop = new Prop("component", at(ballPosition, gun.rotation(), visible), material);
            }
            case RAM -> {
                float align = ease(t, 0, .24F) * (1 - ease(t, .87F, 1));
                float strikeStart = commit - (matchlock ? 7F : 6F) / duration;
                float feed = ease(t, .24F, strikeStart);
                float thrust = ease(t, strikeStart, commit);
                float withdraw = ease(t, commit + .06F, .87F);
                float insertion = (.78F * feed + .22F * thrust) * (1 - withdraw);
                float depth = (matchlock ? .83F : .47F) * insertion;
                Vector3f head = new Vector3f(muzzle).fma(.035F - depth, axis);
                // Slide the grip along the shaft while feeding it into the bore.
                // There is one inward stroke and one withdrawal along the same axis.
                float grasp = 3.2F + (matchlock ? 6F : 3.5F) * feed * (1 - withdraw);
                float gripAlongRod = Mth.lerp(align, 10, grasp);
                Vector3f boreGrip = new Vector3f(head).fma(grasp / 16, axis);
                Vector3f rodGrip = new Vector3f(rest).lerp(boreGrip, align);
                Quaternionf rodAngle = rotation(-16, 46, 0).slerp(gun.rotation(), align);
                Part rod = rodAt(rodGrip, rodAngle, gripAlongRod);
                support = new Vector3f(rodGrip).add(.085F * align, -.015F * align,
                        (firstPerson ? .07F : .01F) * align);
                prop = new Prop("ramrod", rod, ItemStack.EMPTY);
            }
            default -> throw new IllegalArgumentException("Not a loading operation: " + operation);
        }
        return new Frame(gun, main, support, null, prop, 15, 4, 1, 0, true);
    }

    private static Frame match(FirearmItem item, Operation operation, float age, boolean firstPerson) {
        int duration = FirearmActions.duration(operation, true);
        float reach = ease(age, 0, Math.min(9, duration * .35F)) * (1 - ease(age, duration * .72F, duration));
        Vector3f main = mainGrip(item);
        Vector3f right = new Vector3f(-.29F, 1.10F, firstPerson ? .36F : .23F);
        Part gun = place(right, rotation(35, -23, -20), main, scale(item));
        Vector3f vent = FirearmMesh.INSTANCE.ignitionSocket(item.profile().getPath());
        Vector3f support = vent == null ? null : new Vector3f(RELAXED_HAND).lerp(
                point(gun, vent).add(.11F, .055F, -.03F), reach);
        return new Frame(gun, main, support, null, null, 28, 1, 0, 0, true);
    }

    private static Frame blend(Frame from, Frame to, float weight) {
        Vector3f before = from.support == null ? RELAXED_HAND : from.support;
        Vector3f after = to.support == null ? RELAXED_HAND : to.support;
        Vector3f support = from.support == null && to.support == null ? null : new Vector3f(before).lerp(after, weight);
        Prop prop = to.prop;
        if (prop != null && support != null) prop = prop.follow(new Vector3f(support).sub(after));
        return new Frame(mix(from.gun, to.gun, weight), new Vector3f(from.mainGrip).lerp(to.mainGrip, weight),
                support, to.trigger, prop, Mth.lerp(weight, from.turn, to.turn), Mth.lerp(weight, from.lean, to.lean),
                Mth.lerp(weight, from.loading, to.loading),
                Mth.lerp(weight, from.relaxedOffhand, to.relaxedOffhand), to.showOffhand);
    }

    static Vector3f point(Part part, Vector3f local) {
        return part.rotation().transform(new Vector3f(local).mul(part.scale())).add(part.position()).div(16);
    }

    private static Part trigger(Part gun, float press) {
        Vector3f pivot = new Vector3f(0, -1.36F, -7.24F);
        Quaternionf moved = new Quaternionf(gun.rotation()).rotateX(-18 * press * Mth.DEG_TO_RAD);
        return place(point(gun, pivot), moved, pivot, gun.scale());
    }

    private static Part rodAt(Vector3f grip, Quaternionf angle, float along) {
        return place(grip, angle, new Vector3f(0, 0, along), ROD_SCALE);
    }

    private static Part place(Vector3f contact, Quaternionf angle, Vector3f local, float scale) {
        Vector3f origin = new Vector3f(contact).mul(16).sub(angle.transform(new Vector3f(local).mul(scale)));
        return new Part(origin, angle, scale);
    }

    private static Part at(Vector3f position, Quaternionf angle, float scale) {
        return new Part(new Vector3f(position).mul(16), angle, scale);
    }

    private static Part mix(Part from, Part to, float weight) {
        return new Part(new Vector3f(from.position()).lerp(to.position(), weight),
                new Quaternionf(from.rotation()).slerp(to.rotation(), weight), Mth.lerp(weight, from.scale(), to.scale()));
    }

    private static float ease(float value, float from, float to) {
        float progress = Mth.clamp((value - from) / (to - from), 0, 1);
        return progress * progress * (3 - 2 * progress);
    }

    private static Quaternionf rotation(float yaw, float pitch, float roll) {
        return new Quaternionf().rotationYXZ(yaw * Mth.DEG_TO_RAD, pitch * Mth.DEG_TO_RAD, roll * Mth.DEG_TO_RAD);
    }

    static Vector3f mainGrip(FirearmItem item) { return item.matchlock() ? ARQUEBUS_GRIP : CANNON_GRIP; }
    static float scale(FirearmItem item) { return item.matchlock() ? .7F : 1; }
}
