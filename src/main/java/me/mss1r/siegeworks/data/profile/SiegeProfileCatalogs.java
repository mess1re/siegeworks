package me.mss1r.siegeworks.data.profile;

import me.mss1r.axiomata.ballistics.profile.ProjectilePhysicsProfile;
import me.mss1r.axiomata.data.profile.ProfileCatalog;

public final class SiegeProfileCatalogs {
    public static final ProfileCatalog<SiegeEngineProfile> ENGINES =
            new ProfileCatalog<>(SiegeEngineProfile.DEFAULT);
    public static final ProfileCatalog<ProjectilePhysicsProfile> PROJECTILES =
            new ProfileCatalog<>(ProjectilePhysicsProfile.DEFAULT);
    public static final ProfileCatalog<PotFillingProfile> POT_FILLINGS =
            new ProfileCatalog<>(PotFillingProfile.DEFAULT);

    private SiegeProfileCatalogs() {
    }
}
