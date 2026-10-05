package me.mss1r.siegeworks.gameplay.ladder;

import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import me.mss1r.siegeworks.entity.siege.SiegeLadderEntity;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
//? if forge {
/*import java.util.UUID;
*///?} else {
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.resources.ResourceLocation;
//?}

import java.util.HashMap;
import java.util.Map;

/**
 * Carrying a siege ladder with both hands. The ladder stays an entity and follows its carrier: ladders up to {@link
 * #OVERHEAD_SECTIONS} sections are held level overhead at the middle, longer ones two thirds up with the foot dragging.
 * While carrying, the player can't use items or sprint. Picked up at the foot, put down in front.
 */
public final class LadderCarry {
    private static final String TAG_CARRIED = "SiegeworksCarriedLadder";
    /** Hand position: height above the feet and distance in front of the body. */
    private static final double HANDS_HEIGHT = 1.9D;
    /** How much lower the hands are while crouching: the drop of a player's eyes from standing to crouching. */
    private static final double CROUCH_DROP = 1.62D - 1.27D;
    private static final double HANDS_FORWARD = 0.25D;
    /** Ladders with this many sections or fewer are carried level overhead; longer ones drag their foot. */
    public static final int OVERHEAD_SECTIONS = 2;
    /** Grip position on a long ladder, as a fraction of its length from the foot. */
    private static final double GRIP_SHARE = 2.0D / 3.0D;
    /** Max distance from the foot to pick a ladder up. */
    private static final double FOOT_REACH = 2.0D;
    private static final double FOOT_REACH_UP = 2.0D;
    /** Distance in front of the player where the ladder is put down. */
    private static final double PUT_DOWN_REACH = 1.0D;
    private static final double LADDER_HALF_WIDTH = 0.45D;
    /** Speed penalty per ladder part (the base and each section). */
    private static final double SLOWDOWN_PER_PART = 0.06D;
    //? if forge {
    /*private static final UUID SLOWDOWN_ID = UUID.fromString("5d0f4c8e-6c47-4b0b-9a3f-2f7f0a9f1c21");
    *///?} else {
    private static final ResourceLocation SLOWDOWN_ID = MinecraftVersionCompat.id("siegeworks", "carrying_ladder");
    //?}

    private static final Map<java.util.UUID, SiegeLadderEntity> CARRIED = new HashMap<>();
    private static final Map<Integer, SiegeLadderEntity> SEEN_CARRIED = new HashMap<>();

    private LadderCarry() {
    }

    /** Carried ladder pose: foot position, yaw, and lean from vertical in degrees. */
    public record Pose(Vec3 foot, float yaw, double leanDegrees) {
    }

    public static Pose pose(Entity carrier, int sections, double length, double levelDegrees) {
        float yaw = carrier instanceof LivingEntity living ? living.yBodyRot : carrier.getYRot();
        double yawRadians = Math.toRadians(yaw);
        Vec3 forward = new Vec3(-Math.sin(yawRadians), 0.0D, Math.cos(yawRadians));
        double handsHeight = HANDS_HEIGHT - (carrier.isCrouching() ? CROUCH_DROP : 0.0D);
        Vec3 hands = carrier.position().add(0.0D, handsHeight, 0.0D).add(forward.scale(HANDS_FORWARD));
        double grip;
        double lean;
        if (sections <= OVERHEAD_SECTIONS) {
            grip = length * 0.5D;
            lean = Math.toRadians(levelDegrees);
        } else {
            grip = length * GRIP_SHARE;
            // Tilt so the foot, below and behind the hands, touches the ground.
            lean = Math.acos(Mth.clamp(handsHeight / grip, 0.0D, 1.0D));
        }
        Vec3 foot = hands.subtract(forward.scale(grip * Math.sin(lean))).subtract(0.0D, grip * Math.cos(lean), 0.0D);
        return new Pose(foot, yaw, Math.toDegrees(lean));
    }

    /** Picks the ladder up if the player is at its foot and not already carrying one. */
    public static boolean tryPickUp(Player player, SiegeLadderEntity ladder) {
        if (carried(player) != null) {
            player.displayClientMessage(Component.translatable("message.siegeworks.ladder.carrying_one"), true);
            return false;
        }
        double dx = player.getX() - ladder.getX();
        double dz = player.getZ() - ladder.getZ();
        double dy = player.getY() - ladder.getY();
        if (dx * dx + dz * dz > FOOT_REACH * FOOT_REACH || dy < -1.0D || dy > FOOT_REACH_UP) {
            player.displayClientMessage(Component.translatable("message.siegeworks.ladder.carry_from_foot"), true);
            return false;
        }
        take(player, ladder);
        player.level().playSound(null, ladder.blockPosition(), SoundEvents.WOOD_HIT, SoundSource.PLAYERS, 0.8F, 0.8F);
        player.displayClientMessage(Component.translatable("message.siegeworks.ladder.carrying"), true);
        return true;
    }

    private static void take(Player player, SiegeLadderEntity ladder) {
        ladder.beginCarry(player);
        CARRIED.put(player.getUUID(), ladder);
        slow(player, ladder.getSections());
    }

    /** Puts the carried ladder down upright in front of the player; it then leans onto whatever is ahead. */
    public static void putDown(Player player) {
        SiegeLadderEntity ladder = carried(player);
        if (ladder == null) {
            return;
        }
        double yawRadians = Math.toRadians(player.getYRot());
        Vec3 foot = player.position().add(-Math.sin(yawRadians) * PUT_DOWN_REACH, 0.0D,
                Math.cos(yawRadians) * PUT_DOWN_REACH);
        AABB upright = new AABB(foot.x - LADDER_HALF_WIDTH, foot.y + 0.01D, foot.z - LADDER_HALF_WIDTH,
                foot.x + LADDER_HALF_WIDTH, foot.y + ladder.getLadderLength(), foot.z + LADDER_HALF_WIDTH);
        if (!player.level().noCollision(upright)) {
            player.displayClientMessage(Component.translatable("message.siegeworks.ladder.carry_no_room"), true);
            return;
        }
        release(player, ladder);
        ladder.standAt(foot, player.getYRot());
        player.level().playSound(null, ladder.blockPosition(), SoundEvents.WOOD_PLACE, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    /** Drops the ladder where it is, e.g. when the carrier dies; it falls and tips over. */
    public static void drop(SiegeLadderEntity ladder) {
        java.util.UUID carrier = ladder.carrierUuid();
        if (carrier != null && CARRIED.get(carrier) == ladder) {
            CARRIED.remove(carrier);
            if (ladder.carrier() != null) {
                unslow(ladder.carrier());
            }
        }
        ladder.endCarry();
    }

    private static void release(Player player, SiegeLadderEntity ladder) {
        CARRIED.remove(player.getUUID());
        unslow(player);
        ladder.endCarry();
    }

    /** Removes the carry entry for a ladder that was removed. */
    public static void forget(SiegeLadderEntity ladder) {
        java.util.UUID carrier = ladder.carrierUuid();
        if (carrier != null && CARRIED.get(carrier) == ladder) {
            CARRIED.remove(carrier);
        }
    }

    /** Server: the ladder the player is carrying, or null. */
    @Nullable
    public static SiegeLadderEntity carried(Player player) {
        SiegeLadderEntity ladder = CARRIED.get(player.getUUID());
        if (ladder != null && (ladder.isRemoved() || !ladder.isCarried())) {
            CARRIED.remove(player.getUUID());
            return null;
        }
        return ladder;
    }

    /** True if the entity is carrying a ladder, on either side. */
    public static boolean isCarrying(Entity entity) {
        if (!entity.level().isClientSide) {
            return entity instanceof Player player && carried(player) != null;
        }
        SiegeLadderEntity ladder = SEEN_CARRIED.get(entity.getId());
        return ladder != null && !ladder.isRemoved() && ladder.carrierId() == entity.getId();
    }

    /** Client: records who carries this ladder when it syncs. */
    public static void seen(SiegeLadderEntity ladder) {
        SEEN_CARRIED.values().removeIf(known -> known == ladder || known.isRemoved());
        if (ladder.isCarried() && !ladder.isRemoved()) {
            SEEN_CARRIED.put(ladder.carrierId(), ladder);
        }
    }

    /** Client: clears recorded carriers when leaving a world. */
    public static void forgetSeen() {
        SEEN_CARRIED.clear();
    }

    /** On logout, stores the carried ladder in the player's data and removes it from the world. */
    public static void stash(Player player) {
        SiegeLadderEntity ladder = carried(player);
        if (ladder == null) {
            return;
        }
        release(player, ladder);
        CompoundTag tag = new CompoundTag();
        ladder.saveWithoutId(tag);
        player.getPersistentData().put(TAG_CARRIED, tag);
        ladder.discard();
    }

    /** On login or dimension change, respawns the stored ladder in the player's hands. */
    public static void restore(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(TAG_CARRIED)) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        CompoundTag tag = data.getCompound(TAG_CARRIED);
        data.remove(TAG_CARRIED);
        SiegeLadderEntity ladder = SiegeworksEntities.SIEGE_LADDER_ENTITY.get().create(level);
        if (ladder == null) {
            return;
        }
        ladder.load(tag);
        ladder.setPos(player.position());
        if (level.getEntity(ladder.getUUID()) != null) {
            ladder.setUUID(Mth.createInsecureUUID(player.getRandom()));
        }
        if (level.addFreshEntity(ladder)) {
            take(player, ladder);
        }
    }

    private static void slow(Player player, int sections) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        speed.removeModifier(SLOWDOWN_ID);
        double amount = -SLOWDOWN_PER_PART * (1 + sections);
        //? if forge {
        /*speed.addTransientModifier(new AttributeModifier(SLOWDOWN_ID, "Carrying a siege ladder", amount,
                AttributeModifier.Operation.MULTIPLY_TOTAL));
        *///?} else {
        speed.addTransientModifier(new AttributeModifier(SLOWDOWN_ID, amount,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        //?}
    }

    private static void unslow(Player player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(SLOWDOWN_ID);
        }
    }

    public static void register() {
        PlayerEvent.PLAYER_QUIT.register(LadderCarry::stash);
        PlayerEvent.PLAYER_JOIN.register(LadderCarry::restore);
        PlayerEvent.CHANGE_DIMENSION.register((player, from, to) -> {
            // The ladder is still in the old dimension; move it with the player.
            if (carried(player) != null) {
                stash(player);
                restore(player);
            }
        });
        EntityEvent.LIVING_DEATH.register((entity, source) -> {
            if (entity instanceof Player player) {
                SiegeLadderEntity ladder = carried(player);
                if (ladder != null) {
                    drop(ladder);
                }
            }
            return EventResult.pass();
        });
        TickEvent.PLAYER_POST.register(player -> {
            if (isCarrying(player)) {
                player.setSprinting(false);
            }
        });
        // Hands are busy: block interactions while carrying.
        InteractionEvent.RIGHT_CLICK_BLOCK.register((player, hand, pos, face) ->
                isCarrying(player) ? EventResult.interruptFalse() : EventResult.pass());
        InteractionEvent.RIGHT_CLICK_ITEM.register((player, hand) -> isCarrying(player)
                ? CompoundEventResult.interruptFalse(player.getItemInHand(hand))
                : CompoundEventResult.pass());
        InteractionEvent.LEFT_CLICK_BLOCK.register((player, hand, pos, face) ->
                isCarrying(player) ? EventResult.interruptFalse() : EventResult.pass());
        InteractionEvent.INTERACT_ENTITY.register((player, entity, hand) ->
                isCarrying(player) ? EventResult.interruptFalse() : EventResult.pass());
        PlayerEvent.ATTACK_ENTITY.register((player, level, target, hand, hit) ->
                isCarrying(player) ? EventResult.interruptFalse() : EventResult.pass());
        LifecycleEvent.SERVER_STOPPED.register(server -> CARRIED.clear());
    }
}
