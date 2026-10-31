package me.mss1r.siegeworks.gameplay.ladder;

import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.events.common.InteractionEvent;
import me.mss1r.axiomata.collision.StructureCollisionResolver;
import me.mss1r.axiomata.collision.StructureGeometry;
import me.mss1r.axiomata.collision.system.StructureCollisionSystem;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.ClipContext;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pushing a siege ladder off a wall: a defender braces from an elevated footing and holds use until it goes.
 * Lean, climbers and the height of the contact determine how much effort it takes.
 */
public final class LadderPush {
    public static final TagKey<Item> PUSHERS = TagKey.create(Registries.ITEM,
            MinecraftVersionCompat.id(Siegeworks.MOD_ID, "ladder_pushers"));
    /** Use repeats every four ticks while held; a longer gap means the brace was let go. */
    private static final int BRACE_GAP_TICKS = 8;
    /** How far below the contact the feet may be when bracing from the wall. */
    private static final double BELOW_CONTACT = 2.5D;
    /** How far the defender may lean toward the foot past the contact before leaving the wall side. */
    private static final double WALL_SIDE_ALLOWANCE = 0.75D;
    /** The least lean a braced ladder is eased to: it is still against the wall until the last push. */
    private static final double MIN_HELD_LEAN_DEGREES = 2.0D;
    /** Half the ladder's working width plus the fork. */
    private static final double LOOK_TOLERANCE = 1.0D;
    /** How far around the player to look for ladders: a long one's foot is well away from its top. */
    private static final double LADDER_SEARCH = 24.0D;
    /** Probe actual footing, rather than relying on onGround or allowing a brace while airborne. */
    private static final double FOOTING_REACH = 0.05D;

    /** Client: the ladder each player was last seen bracing against, by player entity id. */
    private static final Map<Integer, SiegeLadderEntity> SEEN_BRACED = new HashMap<>();

    private final Map<UUID, Brace> braces = new HashMap<>();

    /**
     * Braces long weapons against a reachable ladder from the wall, on each use repeated while the button is held. Their own
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

    /** The nearest reachable rail or rung looked at from an elevated footing, using the same target as bracing. */
    @Nullable
    public static SiegeLadderEntity ladderLookedAt(Player player) {
        SiegeLadderEntity best = null;
        double closest = Double.POSITIVE_INFINITY;
        double footing = footingHeight(player);
        if (Double.isNaN(footing)) return null;
        for (SiegeLadderEntity ladder : player.level().getEntitiesOfClass(SiegeLadderEntity.class,
                player.getBoundingBox().inflate(LADDER_SEARCH))) {
            Vec3 contact = bracePoint(ladder, player, footing);
            if (contact != null && player.getEyePosition().distanceToSqr(contact) < closest) {
                best = ladder;
                closest = player.getEyePosition().distanceToSqr(contact);
            }
        }
        return best;
    }

    /** Real footing, reach and visibility determine the contact, without a length-based working zone. */
    @Nullable
    private static Vec3 bracePoint(SiegeLadderEntity ladder, Player player, double footing) {
        if (ladder.isCarried() || !ladder.isFullyBuilt() || Math.abs(ladder.getLeanAngleDegrees()) < 3.0F) {
            return null;
        }
        Vec3 top = ladder.topPoint();
        if (top.y <= ladder.getY() || Double.isNaN(footing) || footing <= ladder.getY() + FOOTING_REACH) return null;
        Vec3 eye = player.getEyePosition();
        double reach = MinecraftVersionCompat.entityInteractionRange(player) + 0.5D;
        Vec3 look = player.getLookAngle();
        Vec3 contact = closestOnSpan(eye, look.scale(reach), ladder.position(), top);
        if (contact == null) return null;
        Vec3 toContact = contact.subtract(eye);
        double along = toContact.dot(look);
        if (along <= 0 || toContact.lengthSqr() > reach * reach + 1.0E-6D
                || toContact.subtract(look.scale(along)).lengthSqr() > LOOK_TOLERANCE * LOOK_TOLERANCE) {
            return null;
        }
        Vec3 offset = player.position().subtract(contact);
        Vec3 uphill = ladder.uphillVector();
        if (offset.x * uphill.x + offset.z * uphill.z < -WALL_SIDE_ALLOWANCE
                || player.getY() < contact.y - BELOW_CONTACT) return null;
        var obstruction = player.level().clip(new ClipContext(eye, contact,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (obstruction.getType() != HitResult.Type.MISS) return null;
        for (var structure : StructureCollisionSystem.structuresNear(player, new AABB(eye, contact).inflate(.001))) {
            if (structure != ladder && StructureCollisionResolver.clipSegment(structure.collisionTransform(),
                    StructureGeometry.solid(structure), eye, contact, 0).isPresent()) return null;
        }
        return contact;
    }

    /** World blocks and solid Axiomata geometry can both provide the defender's footing. */
    private static double footingHeight(Player player) {
        if (player.isPassenger() || player.getDeltaMovement().y > 1.0E-4D) return Double.NaN;
        Vec3 down = new Vec3(0, -FOOTING_REACH, 0);
        var box = player.getBoundingBox();
        double allowed = Entity.collideBoundingBox(player, down, box, player.level(), List.of()).y;
        if (allowed > down.y + 1.0E-7D) return player.getY() + allowed;
        double height = Double.NaN;
        for (var structure : StructureCollisionSystem.structuresNear(player, box.expandTowards(down))) {
            var support = StructureCollisionResolver.resolveMovement(structure, box, down, 0);
            if (support.isSupported()) {
                double surface = player.getY() + support.allowed().y;
                if (Double.isNaN(height) || surface > height) height = surface;
            }
        }
        return height;
    }

    /** Closest reachable point on the working span to the finite look segment. */
    @Nullable
    private static Vec3 closestOnSpan(Vec3 eye, Vec3 ray, Vec3 start, Vec3 end) {
        Vec3 span = end.subtract(start);
        Vec3 offset = eye.subtract(start);
        double rayLength = ray.lengthSqr();
        double spanLength = span.lengthSqr();
        // Clip to the reach sphere first. The closest point to the ray endpoint can be
        // just outside reach even though the next rail/rung along the same span is reachable.
        double projection = span.dot(offset);
        double discriminant = projection * projection - spanLength * (offset.lengthSqr() - rayLength);
        if (discriminant < 0 || spanLength < 1.0E-8D) return null;
        double root = Math.sqrt(discriminant);
        double near = Math.max(0, (projection - root) / spanLength);
        double far = Math.min(1, (projection + root) / spanLength);
        if (near > far) return null;
        start = start.add(span.scale(near));
        span = span.scale(far - near);
        spanLength = span.lengthSqr();
        if (spanLength < 1.0E-8D) return start;
        offset = eye.subtract(start);
        double raySpan = ray.dot(span);
        double rayOffset = ray.dot(offset);
        double spanOffset = span.dot(offset);
        double denominator = rayLength * spanLength - raySpan * raySpan;
        double alongRay = denominator > 1.0E-8D
                ? Mth.clamp((raySpan * spanOffset - spanLength * rayOffset) / denominator, 0, 1) : 0;
        double alongSpan = Mth.clamp((raySpan * alongRay + spanOffset) / spanLength, 0, 1);
        return start.add(span.scale(alongSpan));
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

    /** Whether the player can brace against the rail or rung currently under their crosshair from the wall. */
    public static boolean canBrace(SiegeLadderEntity ladder, Player player) {
        return bracePoint(ladder, player, footingHeight(player)) != null;
    }

    /** Ticks of bracing at the tip, from the server config, the initial lean and the climbers on the ladder. */
    public static int requiredTicks(float leanDegrees, int climbers) {
        double seconds = SiegeworksServerConfig.getLadderPushSeconds()
                * (1.0D + SiegeworksServerConfig.getLadderPushPer45Degrees() * Math.abs(leanDegrees) / 45.0D)
                * (1.0D + SiegeworksServerConfig.getLadderPushPerClimber() * climbers);
        return Math.max(1, Mth.ceil(seconds * 20.0D));
    }

    /** One held use of a pole against the ladder; pushes it off once braced long enough. */
    public void brace(SiegeLadderEntity ladder, Player player, InteractionHand hand, long gameTime) {
        Vec3 contact = bracePoint(ladder, player, footingHeight(player));
        if (contact == null) {
            braces.remove(player.getUUID());
            player.displayClientMessage(Component.translatable("message.siegeworks.ladder.push_from_top"), true);
            return;
        }
        Brace brace = braces.get(player.getUUID());
        long gap = brace == null ? Long.MAX_VALUE : gameTime - brace.lastTick;
        boolean reset = gap > BRACE_GAP_TICKS || gap < 0;
        float initialLean = reset ? ladder.getLeanAngleDegrees() : brace.initialLean;
        int required = requiredTicks(initialLean, ladder.climberCount());
        // A horizontal push has a moment arm equal to its height above the foot. Relative
        // to pushing at the tip, a lower contact contributes proportionally less effort.
        double leverage = Mth.clamp((contact.y - ladder.getY()) / (ladder.topPoint().y - ladder.getY()), 0, 1);
        double progress = reset ? 0 : brace.progress + gap * leverage / required;
        if (progress >= 1) {
            braces.clear();
            ladder.pushOffWall();
            MinecraftVersionCompat.damageHeldItem(player.getItemInHand(hand), 1, player, hand);
            ladder.level().playSound(null, ladder.topPoint().x, ladder.topPoint().y, ladder.topPoint().z,
                    SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0F, 0.7F);
            player.displayClientMessage(Component.translatable("message.siegeworks.ladder.pushed"), true);
            return;
        }
        braces.put(player.getUUID(), new Brace(progress, initialLean, gameTime, player.getId()));
        player.displayClientMessage(Component.translatable("message.siegeworks.ladder.pushing",
                Math.min(99, (int) (progress * 100))), true);
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
        return braces.values().stream().mapToDouble(brace -> Math.min(1.0D, brace.progress))
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

    private record Brace(double progress, float initialLean, long lastTick, int entityId) {
    }
}
