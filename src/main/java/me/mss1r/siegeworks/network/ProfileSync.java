package me.mss1r.siegeworks.network;

import me.mss1r.axiomata.data.profile.ProfileSynchronization;
import me.mss1r.siegeworks.data.profile.SiegeProfileCatalogs;

/** Syncs projectile profiles and pot fillings on join and after a datapack reload. */
public final class ProfileSync {
    private ProfileSync() {}

    public static void register() {
        ProfileSynchronization.register(SiegeProfileCatalogs.PROJECTILES,
                ProjectileProfilesS2CPayload::current, SiegeworksNetworking::sendToPlayer);
        ProfileSynchronization.register(SiegeProfileCatalogs.POT_FILLINGS,
                PotFillingsS2CPayload::current, SiegeworksNetworking::sendToPlayer);
    }
}
