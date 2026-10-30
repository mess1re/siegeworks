package me.mss1r.siegeworks.gameplay.ladder;

import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.events.common.InteractionEvent;
import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.config.SiegeworksServerConfig;
import me.mss1r.siegeworks.entity.siege.SiegeLadderEntity;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Pushing a siege ladder off a wall: a defender on the wall braces a pole against its top and holds use until it goes.
 * The time it takes grows with the ladder's lean and with everyone on it.
 */
public final class LadderPush {
    public static final TagKey<Item> PUSHERS = TagKey.create(Registries.ITEM,
            MinecraftVersionCompat.id(Siegeworks.MOD_ID, "ladder_pushers"));
    /** Use repeats every four ticks while held; a longer gap means the brace was let go. */
    private static final int BRACE_GAP_TICKS = 8;
    /** How far below the ladder's top the feet may be and still brace against it from the wall. */
    private static final double BELOW_TOP = 2.5D;
    /** How far past the top, toward the wall's face, a brace may start: a defender leans over the parapet. */
    private static final double BEFORE_TOP = 0.75D;
    /** The least lean a braced ladder is eased to: it is still against the wall until the last push. */
    private static final double MIN_HELD_LEAN_DEGREES = 2.0D;
    /** How far off the line of sight a ladder's top may be and still count as looked at. */
    private static final double LOOK_TOLERANCE = 1.0D;
    /** How far around the player to look for ladders: a long one's foot is well away from its top. */
    private static final double LADDER_SEARCH = 24.0D;

    /** Client: the ladder each player was last seen bracing against, by player entity id. */
    private static final Map<Integer, SiegeLadderEntity> SEEN_BRACED = new HashMap<>();

    private final Map<UUID, Brace> braces = new HashMap<>();

    /**
     * Braces long weapons against the ladder top looked at, on each use repeated while the button is held. Their own
     * use is skipped meanwhile; the forked pole braces through its own use instead.
     */
    public static void register() {
        InteractionEvent.RIGHT_CLICK_ITEM.register((player, hand) -> {
            ItemStack held = player.getItemInHand(hand);
            if (hand != InteractionHand.MAIN_HAND || held.getItem() instanceof me.mss1r.siegeworks.item.ForkedPoleItem
                    || !isPusher(player, held)) {
                return CompoundEventResult.pass();
            }
            SiegeLadderEntity ladder = ladderLookedAt(player);
            if (ladder == null) {
                return CompoundEventResult.pass();
            }
            ladder.braceWith(player, hand);
            return CompoundEventResult.interruptFalse(held);
        });
    }

    /**
     * The ladder whose top {@code player} looks at within reach, if any. Its hitbox stays at its foot, so the top is
     * found by where it is rather than by aiming at the entity.
     */
    @Nullable
    public static SiegeLadderEntity ladderLookedAt(Player player) {
        double reach = MinecraftVersionCompat.entityInteractionRange(player) + 0.5D;
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        SiegeLadderEntity best = null;
        double bestOffset = LOOK_TOLERANCE;
        for (SiegeLadderEntity ladder : player.level().getEntitiesOfClass(SiegeLadderEntity.class,
                player.getBoundingBox().inflate(LADDER_SEARCH))) {
            if (ladder.isCarried()) {
                continue;
            }
            Vec3 toTop = ladder.topPoint().subtract(eye);
            double along = toTop.dot(look);
            double offset = toTop.subtract(look.scale(along)).length();
            if (along > 0.0D && toTop.length() <= reach && offset < bestOffset) {
                best = ladder;
                bestOffset = offset;
            }
        }
        return best;
    }

    /** Whether the stack braces against ladders: a pole from the tag, or a weapon reaching far enough. */
    public static boolean isPusher(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.is(PUSHERS)) {
            return true;
        }
        double minimum = SiegeworksServerConfig.getLadderPushMinReachBonus();
        return minimum > 0.0D && MinecraftVersionCompat.entityReachBonus(player) >= minimum;
    }

    /** Whether {@code player} stands where the ladder's top can be pushed: on the wall side of it, near its top. */
    public static boolean canBrace(SiegeLadderEntity ladder, Player player) {
        Vec3 top = ladder.topPoint();
        Vec3 uphill = ladder.uphillVector();
        Vec3 offset = player.position().subtract(top);
        return Math.abs(ladder.getLeanAngleDegrees()) >= 3.0F
                && offset.x * uphill.x + offset.z * uphill.z >= -BEFORE_TOP
                && player.getY() >= top.y - BELOW_TOP
                && player.getEyePosition().distanceTo(top) <= MinecraftVersionCompat.entityInteractionRange(player) + 0.5D;
    }

    /** Ticks of bracing that push the ladder off, from the server config, its lean and the climbers on it. */
    public static int requiredTicks(float leanDegrees, int climbers) {
        double seconds = SiegeworksServerConfig.getLadderPushSeconds()
                * (1.0D + SiegeworksServerConfig.getLadderPushPer45Degrees() * Math.abs(leanDegrees) / 45.0D)
                * (1.0D + SiegeworksServerConfig.getLadderPushPerClimber() * climbers);
        return Math.max(1, Mth.ceil(seconds * 20.0D));
    }

    /** One held use of a pole against the ladder; pushes it off once braced long enough. */
    public void brace(SiegeLadderEntity ladder, Player player, InteractionHand hand, long gameTime) {
        if (!canBrace(ladder, player)) {
            braces.remove(player.getUUID());
            player.displayClientMessage(Component.translatable("message.siegeworks.ladder.push_from_top"), true);
            return;
        }
        Brace brace = braces.get(player.getUUID());
        long gap = brace == null ? Long.MAX_VALUE : gameTime - brace.lastTick;
        int braced = gap > BRACE_GAP_TICKS ? 0 : brace.ticks + (int) gap;
        int required = requiredTicks(ladder.getLeanAngleDegrees(), ladder.climberCount());
        if (braced >= required) {
            braces.clear();
            ladder.pushOffWall();
            MinecraftVersionCompat.damageHeldItem(player.getItemInHand(hand), 1, player, hand);
            ladder.level().playSound(null, ladder.topPoint().x, ladder.topPoint().y, ladder.topPoint().z,
                    SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0F, 0.7F);
            player.displayClientMessage(Component.translatable("message.siegeworks.ladder.pushed"), true);
            return;
        }
        braces.put(player.getUUID(), new Brace(braced, required, gameTime, player.getId()));
        player.displayClientMessage(Component.translatable("message.siegeworks.ladder.pushing",
                Math.min(99, braced * 100 / required)), true);
    }

    /** The entity id of someone bracing against the ladder now, or -1; forgets braces let go. */
    public int activeBracer(long gameTime) {
        braces.values().removeIf(brace -> gameTime - brace.lastTick > BRACE_GAP_TICKS);
        return braces.values().stream().mapToInt(Brace::entityId).findFirst().orElse(-1);
    }

    /** Client: whether {@code entity} stands braced against a ladder, for its pose. */
    public static boolean isBracing(LivingEntity entity) {
        SiegeLadderEntity ladder = SEEN_BRACED.get(entity.getId());
        return ladder != null && !ladder.isRemoved() && ladder.bracerId() == entity.getId();
    }

    /** Client: records who braces against this ladder when it syncs. */
    public static void seen(SiegeLadderEntity ladder) {
        SEEN_BRACED.values().removeIf(known -> known == ladder || known.isRemoved());
        if (ladder.bracerId() >= 0 && !ladder.isRemoved()) {
            SEEN_BRACED.put(ladder.bracerId(), ladder);
        }
    }

    /** Client: forgets every ladder seen braced, on leaving a world. */
    public static void forgetSeen() {
        SEEN_BRACED.clear();
    }

    /** The hand a bracing {@code entity} holds its pole in. */
    public static InteractionHand bracingHand(LivingEntity entity) {
        return entity.isUsingItem() ? entity.getUsedItemHand() : InteractionHand.MAIN_HAND;
    }

    /** How far along the furthest brace on the ladder is, from 0 to 1; 0 when nobody braces it. */
    public double progress() {
        return braces.values().stream().mapToDouble(brace -> Math.min(1.0D, (double) brace.ticks / brace.required))
                .max().orElse(0.0D);
    }

    /**
     * The lean, in degrees, a ladder braced from {@code fromDegrees} is held at {@code progress} of the way: its top
     * eased off the wall by up to the configured distance, though never past upright.
     */
    public static double heldLean(double fromDegrees, double length, double progress) {
        double from = Math.toRadians(Math.abs(fromDegrees));
        double minimum = Math.toRadians(MIN_HELD_LEAN_DEGREES);
        if (from <= minimum || length <= 0.0D) {
            return fromDegrees;
        }
        double offset = Math.max(length * Math.sin(from) - progress * SiegeworksServerConfig.getLadderPushLiftBlocks(),
                length * Math.sin(minimum));
        return Math.copySign(Math.toDegrees(Math.asin(offset / length)), fromDegrees);
    }

    private record Brace(int ticks, int required, long lastTick, int entityId) {
    }
}
