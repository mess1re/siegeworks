package me.mss1r.siegeworks.gameplay.firearm;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import me.mss1r.siegeworks.data.profile.SiegeProfileCatalogs;
import me.mss1r.siegeworks.gameplay.audio.ArtilleryLoadingSounds;
import me.mss1r.siegeworks.entity.projectile.FirearmProjectile;
import me.mss1r.siegeworks.item.FirearmItem;
import me.mss1r.siegeworks.item.RoundShotItem;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.registry.SiegeworksItems;
import me.mss1r.siegeworks.registry.SiegeworksSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.Map;
import java.util.WeakHashMap;
import static me.mss1r.siegeworks.gameplay.firearm.FirearmLoad.Operation.*;

/** One server-owned operation per wielder. The material is checked again at the point of consumption. */
public final class FirearmActions {
    public static final int HAND_CANNON_IGNITION_TICK = 12;
    public static final int HAND_CANNON_FUSE_TICKS = 20;
    public static final int HAND_CANNON_SHOT_TICK = HAND_CANNON_IGNITION_TICK + HAND_CANNON_FUSE_TICKS;

    // Stroke starts measured from the ramrod's motion relative to the barrel.
    // Fractions follow the animation when its playback duration changes.

    private static final Map<LivingEntity, Session> SESSIONS = new WeakHashMap<>();
    private static final class Session {
        final ItemStack weapon, material;
        final FirearmLoad.Operation operation;
        final long start;
        final HotbarSwap swap;
        boolean committed, ignited;
        int soundTick = -1;
        Session(ItemStack weapon, ItemStack material, FirearmLoad.Operation operation, long start, HotbarSwap swap) {
            this.weapon = weapon; this.material = material.copy(); this.operation = operation; this.start = start;
            this.swap = swap;
        }
    }

    private record Selection(FirearmLoad.Operation operation, int hotbarSlot) {}

    private record HotbarSwap(int slot, ItemStack borrowed, ItemStack displaced) {
        static HotbarSwap take(Player player, int slot) {
            if (slot < 0) return null;
            Inventory inventory = player.getInventory();
            var swap = new HotbarSwap(slot, inventory.getItem(slot), player.getOffhandItem());
            inventory.setItem(slot, swap.displaced);
            player.setItemInHand(InteractionHand.OFF_HAND, swap.borrowed);
            inventory.setChanged();
            return swap;
        }

        boolean intact(Player player) {
            return sameStack(player.getInventory().getItem(slot), displaced)
                    && sameStack(player.getOffhandItem(), borrowed);
        }

        void restore(Player player) {
            if (!intact(player)) return;
            player.getInventory().setItem(slot, player.getOffhandItem());
            player.setItemInHand(InteractionHand.OFF_HAND, displaced);
            player.getInventory().setChanged();
        }

        private static boolean sameStack(ItemStack current, ItemStack expected) {
            return current == expected || current.isEmpty() && expected.isEmpty();
        }
    }

    private FirearmActions() {}

    public static void register() {
        TickEvent.PLAYER_POST.register(player -> {
            if (player.level().isClientSide) return;
            var session = SESSIONS.get(player);
            if (session != null && (!player.isUsingItem() || player.getUseItem() != session.weapon
                    || player.getMainHandItem() != session.weapon)) cancelActive(player);
        });
        PlayerEvent.PLAYER_QUIT.register(FirearmActions::cancelActive);
        EntityEvent.LIVING_DEATH.register((entity, source) -> {
            if (!entity.level().isClientSide) cancelActive(entity);
            return EventResult.pass();
        });
    }

    public static void cancelActive(LivingEntity player) {
        var session = SESSIONS.get(player);
        if (session != null) cancel(player, session.weapon);
    }

    public static int caliber(FirearmItem item) {
        return (int) Math.round(SiegeProfileCatalogs.PROJECTILES.get(item.profile()).diameter().orElse(0.0) * 1000);
    }

    public static boolean begin(Player player, ItemStack stack, FirearmItem item) {
        if (player.isSpectator() || player.isUsingItem() || me.mss1r.siegeworks.gameplay.ladder.LadderCarry.isCarrying(player)) return false;
        cancelActive(player);
        var load = FirearmStack.load(stack);
        var selection = select(player, load, item);
        if (selection == null && load.charge() != FirearmLoad.Charge.READY) {
            player.displayClientMessage(nextActionHint(player, item), true);
            return false;
        }
        start(player, stack, selection == null ? null : selection.operation(),
                selection == null ? null : HotbarSwap.take(player, selection.hotbarSlot()));
        return true;
    }

    public static Component nextActionHint(Player player, FirearmItem item) {
        var load = FirearmStack.load(player.getMainHandItem());
        if (load.charge() == FirearmLoad.Charge.READY) {
            var requirement = fireRequirement(player, item);
            if (requirement != FirearmLoad.FireRequirement.NONE) {
                return Component.translatable("firearm.siegeworks.require." + requirement.name().toLowerCase(java.util.Locale.ROOT));
            }
        }
        if (load.charge() == FirearmLoad.Charge.POWDER) {
            return Component.translatable("firearm.siegeworks.ball_caliber", caliber(item));
        }
        return Component.translatable("firearm.siegeworks.charge." + load.charge().name().toLowerCase(java.util.Locale.ROOT));
    }

    public static boolean canBegin(Player player, ItemStack stack, FirearmItem item) {
        var load = FirearmStack.load(stack);
        return !player.isSpectator() && !me.mss1r.siegeworks.gameplay.ladder.LadderCarry.isCarrying(player)
                && (load.charge() == FirearmLoad.Charge.READY || select(player, load, item) != null);
    }

    private static Selection select(Player player, FirearmLoad load, FirearmItem item) {
        ItemStack offhand = player.getOffhandItem();
        var manual = operation(load, item, offhand);
        if (manual != null) return new Selection(manual, -1);
        if (player.isShiftKeyDown() && offhand.isEmpty() && load.accepts(EXTINGUISH_MATCH, item.matchlock())) {
            return new Selection(EXTINGUISH_MATCH, -1);
        }
        Inventory inventory = player.getInventory();
        Selection best = null;
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            if (slot == inventory.selected) continue;
            var candidate = operation(load, item, inventory.getItem(slot));
            if (candidate == null || player.isShiftKeyDown() && candidate == LIGHT_MATCH) continue;
            // Charge stages precede optional match preparation in the operation order.
            if (best == null || candidate.compareTo(best.operation()) < 0) best = new Selection(candidate, slot);
        }
        return best;
    }

    public static FirearmLoad.Operation operation(FirearmLoad load, FirearmItem item, ItemStack material) {
        if (material.isEmpty()) return null;
        boolean matchlock = item.matchlock();
        if (load.accepts(RAM, matchlock) && material.is(SiegeworksItems.FIREARM_RAMROD.get())) return RAM;
        if (load.accepts(POWDER, matchlock) && material.is(SiegeworksItems.BLACK_POWDER.get())) return POWDER;
        if (load.accepts(BALL, matchlock) && matchesBall(material, item)) return BALL;
        if (load.accepts(INSERT_MATCH, matchlock) && material.is(Items.STRING)) return INSERT_MATCH;
        if (load.accepts(LIGHT_MATCH, matchlock) && material.is(Items.FLINT_AND_STEEL)) return LIGHT_MATCH;
        return null;
    }

    private static boolean matchesBall(ItemStack material, FirearmItem item) {
        return material.is(SiegeworksItems.CANNON_BALL.get()) && RoundShotItem.caliber(material) == caliber(item);
    }

    private static int hotbarIgniter(Player player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            if (slot != inventory.selected && inventory.getItem(slot).is(Items.FLINT_AND_STEEL)) return slot;
        }
        return -1;
    }

    public static FirearmLoad.FireRequirement fireRequirement(LivingEntity player, FirearmItem item) {
        boolean igniter = item.matchlock() || player.getOffhandItem().is(Items.FLINT_AND_STEEL)
                || player instanceof Player human && hotbarIgniter(human) >= 0;
        return FirearmStack.load(player.getMainHandItem()).fireRequirement(item.matchlock(), igniter);
    }

    private static void start(LivingEntity player, ItemStack stack, FirearmLoad.Operation operation, HotbarSwap swap) {
        long time = player.level().getGameTime();
        SESSIONS.put(player, new Session(stack, player.getOffhandItem(), operation, time, swap));
        MinecraftVersionCompat.editCustomData(stack, tag -> {
            tag.putString("FirearmAction", operation == null ? "AIM" : operation.name());
            tag.putLong("FirearmActionStart", time);
        });
    }

    /** Begins an AI-controlled loading operation, or aiming when operation is null. */
    public static boolean beginOperation(LivingEntity user, FirearmLoad.Operation operation) {
        if (user.level().isClientSide || user.isUsingItem()
                || !(user.getMainHandItem().getItem() instanceof FirearmItem item)) return false;
        ItemStack stack = user.getMainHandItem();
        var load = FirearmStack.load(stack);
        if (operation == null ? load.charge() != FirearmLoad.Charge.READY
                : operation == FIRE || operation(load, item, user.getOffhandItem()) != operation) return false;
        cancelActive(user);
        start(user, stack, operation, null);
        user.startUsingItem(InteractionHand.MAIN_HAND);
        return true;
    }

    public static void requestFire(LivingEntity player) {
        var session = SESSIONS.get(player);
        if (session == null || session.operation != null || !player.isUsingItem()
                || player.getUseItem() != session.weapon || player.getMainHandItem() != session.weapon
                || !(session.weapon.getItem() instanceof FirearmItem item)) return;
        if (fireRequirement(player, item) != FirearmLoad.FireRequirement.NONE) {
            if (player instanceof Player human) human.displayClientMessage(nextActionHint(human, item), true);
            return;
        }
        int slot = item.matchlock() || player.getOffhandItem().is(Items.FLINT_AND_STEEL) ? -1
                : player instanceof Player human ? hotbarIgniter(human) : -1;
        start(player, session.weapon, FIRE, player instanceof Player human ? HotbarSwap.take(human, slot) : null);
        // A lit match is already an ignition source; commit the trigger press now.
        if (item.matchlock()) tick(player, session.weapon, item);
    }

    public static int duration(FirearmLoad.Operation operation, boolean matchlock) {
        return switch (operation) {
            case POWDER -> 80;
            case BALL -> 70;
            case RAM -> matchlock ? 190 : 150;
            case LIGHT_MATCH -> 60;
            case INSERT_MATCH -> 24;
            case EXTINGUISH_MATCH -> 12;
            case FIRE -> commitTick(operation, matchlock) + 32;
        };
    }

    public static int commitTick(FirearmLoad.Operation operation, boolean matchlock) {
        return switch (operation) {
            case FIRE -> matchlock ? 0 : HAND_CANNON_SHOT_TICK;
            case LIGHT_MATCH -> 20;
            // Commit at the pour, the ball's insertion and the end of the ramrod push.
            case POWDER -> 45;
            case BALL -> 39;
            case RAM -> matchlock ? 104 : 90;
            default -> duration(operation, matchlock);
        };
    }

    public static void tick(LivingEntity player, ItemStack stack, FirearmItem item) {
        var session = SESSIONS.get(player);
        if (session == null || session.weapon != stack || player.getMainHandItem() != stack) {
            player.stopUsingItem(); return;
        }
        if (session.operation == null) return;
        int age = (int) (player.level().getGameTime() - session.start);
        if (age % 4 == 0 && player instanceof Player human) {
            human.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "firearm.siegeworks.action_progress",
                    net.minecraft.network.chat.Component.translatable("firearm.siegeworks.action."
                            + session.operation.name().toLowerCase(java.util.Locale.ROOT)),
                    me.mss1r.axiomata.loading.LoadingProgress.bar(age, duration(session.operation, item.matchlock()))), true);
        }
        if (!item.matchlock() && session.operation == FIRE && age >= HAND_CANNON_IGNITION_TICK && !session.ignited) {
            if (!player.getOffhandItem().is(Items.FLINT_AND_STEEL)) {
                player.stopUsingItem(); cancel(player, stack); return;
            }
            player.level().playSound(null, player.blockPosition(), SoundEvents.FLINTANDSTEEL_USE,
                    SoundSource.PLAYERS, .7F, 1);
            session.ignited = true;
        }
        if (!session.committed) {
            boolean needsOffhand = session.operation != FIRE || !item.matchlock();
            ItemStack material = player.getOffhandItem();
            if (session.swap != null && (!(player instanceof Player human) || !session.swap.intact(human))
                    || needsOffhand && (!MinecraftVersionCompat.isSameItemAndData(material, session.material)
                    || material.getCount() < session.material.getCount())) {
                player.stopUsingItem(); cancel(player, stack); return;
            }
            if (age >= commitTick(session.operation, item.matchlock())) {
                var load = FirearmStack.load(stack);
                if (!load.accepts(session.operation, item.matchlock()) || (session.operation == BALL && !matchesBall(material, item))) {
                    player.stopUsingItem(); cancel(player, stack); return;
                }
                if (session.operation == FIRE && !shoot(player, item)) {
                    player.stopUsingItem(); cancel(player, stack); return;
                }
                FirearmStack.save(stack, load.complete(session.operation, item.matchlock()));
                if (!(player instanceof Player human) || !human.getAbilities().instabuild) {
                    switch (session.operation) {
                        case POWDER, BALL, INSERT_MATCH -> material.shrink(1);
                        case RAM, LIGHT_MATCH -> MinecraftVersionCompat.damageHeldItem(material, 1, player, InteractionHand.OFF_HAND);
                        case FIRE -> {
                            if (!item.matchlock()) {
                                MinecraftVersionCompat.damageHeldItem(material, 1, player, InteractionHand.OFF_HAND);
                            }
                            MinecraftVersionCompat.damageHeldItem(stack, 1, player, InteractionHand.MAIN_HAND);
                            if (stack.isEmpty()) {
                                player.stopUsingItem();
                                cancel(player, stack);
                                return;
                            }
                        }
                        default -> {}
                    }
                }
                if (session.operation == LIGHT_MATCH) player.level().playSound(null, player.blockPosition(),
                        SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 0.7F, 1.0F);
                session.committed = true;
            }
        }
        playLoadingSounds(player, session, age, item.matchlock());
        if (age >= duration(session.operation, item.matchlock())) {
            player.stopUsingItem(); cancel(player, stack);
        }
    }

    private static void playLoadingSounds(LivingEntity player, Session session, int age, boolean matchlock) {
        if (!(player.level() instanceof ServerLevel level)) return;
        Vec3 position = player.position().add(0, 1, 0);
        switch (session.operation) {
            case POWDER -> {
                if (session.soundTick < 30 && age >= 30) {
                    player.level().playSound(null, player.blockPosition(), SiegeworksSounds.FIREARM_POWDER.get(),
                            SoundSource.PLAYERS, .45F, 1);
                }
            }
            case BALL -> {
                int tick = commitTick(BALL, matchlock);
                if (session.soundTick < tick && age >= tick) {
                    ArtilleryLoadingSounds.playAmmunition(level, position, 1, 1);
                }
            }
            case INSERT_MATCH -> {
                int tick = commitTick(session.operation, matchlock);
                if (session.soundTick < tick && age >= tick) {
                    player.level().playSound(null, player.blockPosition(), SoundEvents.BUNDLE_INSERT,
                            SoundSource.PLAYERS, .45F, .95F);
                }
            }
            case RAM -> {
                int duration = duration(RAM, matchlock);
                // The first and last cues land inside the feed and withdrawal motions of both clips.
                int feed = duration * 35 / 100;
                int seat = commitTick(RAM, matchlock);
                int withdraw = duration * 74 / 100;
                if (session.soundTick < feed && age >= feed) {
                    ArtilleryLoadingSounds.playRamStroke(level, position, .65F, 1.20F);
                }
                if (session.soundTick < seat && age >= seat) {
                    ArtilleryLoadingSounds.playRamStroke(level, position, 1, 1.15F);
                }
                if (session.soundTick < withdraw && age >= withdraw
                        && player.getOffhandItem().is(SiegeworksItems.FIREARM_RAMROD.get())) {
                    ArtilleryLoadingSounds.playRamStroke(level, position, .60F, 1.25F);
                }
            }
            default -> {}
        }
        session.soundTick = age;
    }

    public static void cancel(LivingEntity player, ItemStack stack) {
        var session = SESSIONS.get(player);
        if (session != null && session.weapon == stack) {
            SESSIONS.remove(player);
            if (session.swap != null && player instanceof Player human) session.swap.restore(human);
            if (session.operation != null && player instanceof Player human) human.displayClientMessage(net.minecraft.network.chat.Component.empty(), true);
        }
        if (MinecraftVersionCompat.customData(stack).contains("FirearmAction")) {
            MinecraftVersionCompat.editCustomData(stack, tag -> {
                tag.remove("FirearmAction"); tag.remove("FirearmActionStart");
            });
        }
    }

    private static boolean shoot(LivingEntity player, FirearmItem item) {
        if (!(player.level() instanceof ServerLevel level)) return false;
        var physics = SiegeProfileCatalogs.PROJECTILES.get(item.profile());
        double speed = physics.directHit().referenceSpeed().orElse(0.0);
        if (!(speed > 0) || physics.directHit().damage().isEmpty()) return false;
        Vec3 eye = player.getEyePosition(), direction = player.getLookAngle();
        // Clip the short muzzle offset too: the projectile must not start beyond a wall.
        Vec3 muzzle = eye.add(direction.scale(0.4));
        var hit = level.clip(new ClipContext(eye, muzzle, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.MISS) muzzle = eye;
        FirearmProjectile shot = new FirearmProjectile(SiegeworksEntities.FIREARM_BALL.get(), level);
        shot.setPhysicsProfile(item.profile()); shot.setOwner(player);
        shot.reachBlocksAs(player instanceof Player ? player.getUUID()
                : me.mss1r.siegeworks.api.SiegePlayerAttributionRegistry.playerOwnerOf(player));
        shot.setFiredFromArquebus(item.matchlock());
        shot.setBaseDamage(physics.directHit().damage().get()); shot.setReferenceSpeed(speed);
        shot.setPos(muzzle.x, muzzle.y, muzzle.z);
        shot.shoot(direction.x, direction.y, direction.z, (float) (speed / 20), item.matchlock() ? 0.6F : 1.5F);
        if (!level.addFreshEntity(shot)) return false;
        MinecraftVersionCompat.editCustomData(player.getMainHandItem(),
                tag -> tag.putLong("FirearmShotAt", level.getGameTime()));
        level.playSound(null, muzzle.x, muzzle.y, muzzle.z,
                SiegeworksSounds.FIREARM_FIRE.get(),
                SoundSource.PLAYERS, 3, item.matchlock() ? 1.02F : .90F);
        return true;
    }
}
