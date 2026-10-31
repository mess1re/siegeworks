package me.mss1r.siegeworks.client.aim;

import me.mss1r.siegeworks.client.SiegeworksKeyMappings;
import me.mss1r.siegeworks.config.SiegeworksClientConfig;
import me.mss1r.siegeworks.entity.siege.AbstractFieldGunEntity;
import me.mss1r.siegeworks.gameplay.crew.SiegePassengerPhysics;
import net.minecraft.client.KeyMapping;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public final class SiegeAimController {
    private static AbstractSiegeEntity controlledSiege;
    private static float freeLookYaw;
    private static float freeLookPitch;
    private static boolean freeLookInitialized;
    private static boolean freeLookLastTick;
    /** Switched on by a press, for keys set to toggle rather than to be held. */
    private static boolean freeLookToggled;
    private static boolean barrelSightToggled;

    private SiegeAimController() {
    }

    public static void tick(LocalPlayer player) {
        AbstractSiegeEntity siege = SiegeAimView.findControlledSiege(player);
        if (siege == null) {
            reset();
            return;
        }

        initializeFor(siege);
        lockPlayerBodyToSiege(player, siege);
        freeLookToggled = toggled(SiegeworksKeyMappings.FREE_LOOK, freeLookToggled);
        barrelSightToggled = toggled(SiegeworksKeyMappings.BARREL_SIGHT, barrelSightToggled);
        boolean freeLook = isFreeLookActive(player, siege);
        if (freeLook && !freeLookLastTick) {
            initializeFreeLook(siege);
        }
        if (freeLookLastTick && !freeLook) {
            alignPlayerWithSiege(player, siege, true);
            freeLookInitialized = false;
        }
        freeLookLastTick = freeLook;
    }

    public static boolean isFreeLookActive(Player player, AbstractSiegeEntity siege) {
        return player.isLocalPlayer()
                && SiegeAimView.findControlledSiege(player) == siege
                && (SiegeworksClientConfig.freeLookToggles() ? freeLookToggled : SiegeworksKeyMappings.FREE_LOOK.isDown());
    }

    /** Whether the view is set over the barrel of a cannon whose gunner stands beside it, to sight along it. */
    public static boolean isBarrelSightActive(Player player, AbstractSiegeEntity siege) {
        return siege instanceof AbstractFieldGunEntity
                && player.isLocalPlayer()
                && SiegeAimView.findControlledSiege(player) == siege
                && (SiegeworksClientConfig.barrelSightToggles()
                        ? barrelSightToggled
                        : SiegeworksKeyMappings.BARREL_SIGHT.isDown());
    }

    /** Flips a toggled state on each press; presses made while the key is set to be held are let go of all the same. */
    private static boolean toggled(KeyMapping key, boolean state) {
        while (key.consumeClick()) {
            state = !state;
        }
        return state;
    }

    public static void applyFreeLookInput(AbstractSiegeEntity siege, float yawDelta, float pitchDelta) {
        initializeFor(siege);
        initializeFreeLook(siege);
        freeLookYaw = Mth.wrapDegrees(freeLookYaw + yawDelta);
        freeLookPitch = Mth.clamp(freeLookPitch + pitchDelta, -89.0F, 89.0F);
    }

    public static float getFreeLookYaw(AbstractSiegeEntity siege) {
        initializeFor(siege);
        initializeFreeLook(siege);
        return freeLookYaw;
    }

    public static float getFreeLookPitch(AbstractSiegeEntity siege) {
        initializeFor(siege);
        initializeFreeLook(siege);
        return freeLookPitch;
    }

    public static void alignPlayerWithSiege(LocalPlayer player, AbstractSiegeEntity siege,
                                             boolean includePitch) {
        float yaw = siege.getRenderedAimYaw();
        player.setYRot(yaw);
        player.yRotO = yaw;
        player.setYHeadRot(yaw);
        player.yHeadRotO = yaw;
        lockPlayerBodyToSiege(player, siege);
        if (includePitch) {
            float pitch = siege.getRenderedAimPitch();
            player.setXRot(pitch);
            player.xRotO = pitch;
        }
    }

    /** Applies the bounded mouse view of an engine whose elevation follows the operator directly. */
    public static void applyManualAimInput(LocalPlayer player, AbstractSiegeEntity siege) {
        float pitch = Mth.clamp(player.getXRot(), siege.getMinAimPitch(), siege.getMaxAimPitch());
        alignPlayerWithSiege(player, siege, false);
        player.setXRot(pitch);
        player.xRotO = pitch;
        SiegePassengerPhysics.clampView(siege, player, player);
    }

    private static void lockPlayerBodyToSiege(LocalPlayer player, AbstractSiegeEntity siege) {
        float bodyYaw = siege.getPassengerVisualYaw(player);
        player.setYBodyRot(bodyYaw);
    }

    private static void initializeFor(AbstractSiegeEntity siege) {
        if (controlledSiege == siege) {
            return;
        }

        controlledSiege = siege;
        freeLookInitialized = false;
        freeLookLastTick = false;
    }

    private static void initializeFreeLook(AbstractSiegeEntity siege) {
        if (freeLookInitialized) {
            return;
        }

        freeLookYaw = siege.getRenderedAimYaw();
        freeLookPitch = siege.getRenderedAimPitch();
        freeLookInitialized = true;
    }

    private static void reset() {
        controlledSiege = null;
        freeLookToggled = false;
        barrelSightToggled = false;
        freeLookYaw = 0.0F;
        freeLookPitch = 0.0F;
        freeLookInitialized = false;
        freeLookLastTick = false;
    }
}
