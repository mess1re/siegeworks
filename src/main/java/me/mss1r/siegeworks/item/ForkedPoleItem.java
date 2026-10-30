package me.mss1r.siegeworks.item;

import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.entity.siege.SiegeLadderEntity;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
//? if forge {
/*import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import java.util.UUID;
*///?} else {
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.component.ItemAttributeModifiers;
//?}

/**
 * A pole with a fork at its end. While use is held it braces against the siege ladder looked at and pushes it off its
 * wall; see {@link me.mss1r.siegeworks.gameplay.ladder.LadderPush}. Otherwise a slow, long-reaching weapon.
 */
public class ForkedPoleItem extends Item {
    private static final int HOLD_TICKS = 72000;
    private static final int DURABILITY = 64;
    /** Added to the fist's one point of damage. */
    private static final double ATTACK_DAMAGE = 4.0D;
    /** Added to the base four swings a second: one a second. */
    private static final double ATTACK_SPEED = -3.0D;
    /** Blocks of extra reach against entities. */
    private static final double REACH = 1.0D;

    public ForkedPoleItem(Properties properties) {
        super(properties);
    }

    public static Properties properties() {
        //? if forge {
        /*return new Properties().durability(DURABILITY);
        *///?} else {
        return new Properties().durability(DURABILITY).attributes(ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, ATTACK_DAMAGE,
                        AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, ATTACK_SPEED,
                        AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(
                        MinecraftVersionCompat.id(Siegeworks.MOD_ID, "forked_pole_reach"), REACH,
                        AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build());
        //?}
    }

    //? if forge {
    /*private static final UUID REACH_ID = UUID.fromString("5d0f6c55-2f6e-4a5b-9a51-3c1f4e8b7a20");

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot != EquipmentSlot.MAINHAND) {
            return super.getDefaultAttributeModifiers(slot);
        }
        return ImmutableMultimap.<Attribute, AttributeModifier>builder()
                .put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier",
                        ATTACK_DAMAGE, AttributeModifier.Operation.ADDITION))
                .put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier",
                        ATTACK_SPEED, AttributeModifier.Operation.ADDITION))
                .put(net.minecraftforge.common.ForgeMod.ENTITY_REACH.get(), new AttributeModifier(REACH_ID,
                        "Weapon modifier", REACH, AttributeModifier.Operation.ADDITION))
                .build();
    }
    *///?}

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        MinecraftVersionCompat.damageHeldItem(stack, 1, attacker, InteractionHand.MAIN_HAND);
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        // Held as ever until it meets a ladder; the bracing pose is the ladder's to show
        return UseAnim.NONE;
    }

    @Override
    //? if forge {
    /*public int getUseDuration(ItemStack stack) {
    *///?} else {
    public int getUseDuration(ItemStack stack, LivingEntity user) {
    //?}
        return HOLD_TICKS;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (!(level instanceof ServerLevel) || !(user instanceof Player player)) {
            return;
        }
        SiegeLadderEntity ladder = me.mss1r.siegeworks.gameplay.ladder.LadderPush.ladderLookedAt(player);
        if (ladder != null) {
            ladder.braceWith(player, player.getUsedItemHand());
        }
    }
}
