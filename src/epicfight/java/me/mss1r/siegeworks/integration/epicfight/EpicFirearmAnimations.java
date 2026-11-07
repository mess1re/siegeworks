package me.mss1r.siegeworks.integration.epicfight;

import me.mss1r.siegeworks.item.FirearmItem;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.AnimationManager.AnimationBuilder;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.RangedWeaponCapability;
import yesman.epicfight.world.capabilities.item.Style;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public final class EpicFirearmAnimations {
    public static AnimationAccessor<? extends StaticAnimation> LIVE;
    private static AnimationAccessor<? extends StaticAnimation> AIM;
    private static AnimationAccessor<StaticAnimation> ARQUEBUS_RIGHT, ARQUEBUS_LEFT;
    private EpicFirearmAnimations() {}

    public static void build(AnimationBuilder builder) {
        var viewmodel = builder.nextAccessor("firearm/viewmodel", EpicFirearmAnimation::viewmodel);
        LIVE = builder.nextAccessor("firearm/actions", id -> new EpicFirearmAnimation(id, false, viewmodel));
        AIM = builder.nextAccessor("firearm/aim", id -> new EpicFirearmAnimation(id, true, viewmodel));
        ARQUEBUS_RIGHT = builder.nextAccessor("firearm/arquebus_aim_right",
                id -> new StaticAnimation(true, id, yesman.epicfight.gameasset.Armatures.BIPED));
        ARQUEBUS_LEFT = builder.nextAccessor("firearm/arquebus_aim_left",
                id -> new StaticAnimation(true, id, yesman.epicfight.gameasset.Armatures.BIPED));
    }

    static yesman.epicfight.api.animation.Pose arquebusPose(boolean left, float pitch) {
        return (left ? ARQUEBUS_LEFT : ARQUEBUS_RIGHT).get().getRawPose((pitch + 90) / 180);
    }

    public static CapabilityItem.Builder capability() {
        var builder = RangedWeaponCapability.builder().zoomInType(CapabilityItem.ZoomInType.NONE);
        builder.addAnimationsModifier(LivingMotions.IDLE, LIVE);
        builder.addAnimationsModifier(LivingMotions.AIM, AIM);
        builder.addAnimationsModifier(LivingMotions.RELOAD, LIVE);
        return builder.constructor(FirearmCapability::new);
    }

    static String activeAction(LivingEntity entity) {
        if (!(entity.getMainHandItem().getItem() instanceof FirearmItem)) return "";
        String action = MinecraftVersionCompat.customData(entity.getMainHandItem()).getString("FirearmAction");
        // EF cancels local item use when the offhand changes. The server's loading
        // action remains active through that change and is cleared on cancellation.
        if (actionMotion(action) == LivingMotions.RELOAD) return action;
        return entity.isUsingItem() && entity.getUsedItemHand() == InteractionHand.MAIN_HAND ? action : "";
    }

    static LivingMotion actionMotion(String action) {
        return switch (action) {
            case "AIM", "FIRE" -> LivingMotions.AIM;
            case "POWDER", "BALL", "RAM", "INSERT_MATCH", "LIGHT_MATCH", "EXTINGUISH_MATCH" -> LivingMotions.RELOAD;
            default -> null;
        };
    }

    private static final class FirearmCapability extends RangedWeaponCapability {
        FirearmCapability(CapabilityItem.Builder builder) {
            super((RangedWeaponCapability.Builder) builder);
        }

        @Override public Style getStyle(LivingEntityPatch<?> patch) {
            // Firearms need the offhand for the loading and ignition items.
            return CapabilityItem.Styles.ONE_HAND;
        }

        @Override public LivingMotion getLivingMotion(LivingEntityPatch<?> patch, InteractionHand hand) {
            if (hand != InteractionHand.MAIN_HAND) return null;
            var entity = patch.getOriginal();
            String action = activeAction(entity);
            LivingMotion motion = actionMotion(action);
            // Carry and reload use one continuous viewmodel layer.
            if (patch.isFirstPerson()) return motion == LivingMotions.AIM ? motion : LivingMotions.RELOAD;
            if (motion != null) return motion;
            if (entity.level().isClientSide && EpicFirearmMotion.reloadHandoff(entity, action, false)) return LivingMotions.RELOAD;
            return null;
        }
    }
}
