package me.mss1r.siegeworks.client.pose;

import me.mss1r.siegeworks.entity.siege.AbstractFieldGunEntity;
import me.mss1r.siegeworks.entity.siege.ArcballistaEntity;
import me.mss1r.siegeworks.entity.siege.BatteringRamEntity;
import me.mss1r.siegeworks.entity.siege.CulverinEntity;
import me.mss1r.siegeworks.entity.siege.HwachaEntity;
import me.mss1r.siegeworks.entity.siege.OnagerEntity;
import me.mss1r.siegeworks.entity.siege.MantletEntity;
import me.mss1r.siegeworks.entity.siege.MonsMegEntity;
import me.mss1r.siegeworks.entity.siege.SiegeTowerEntity;
import me.mss1r.siegeworks.entity.siege.TowerCrossbowEntity;
import me.mss1r.siegeworks.entity.siege.TrebuchetEntity;
import me.mss1r.axiomata.collision.system.StructureClimbingSystem;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public final class SiegePoseResolver {
    private SiegePoseResolver() {
    }

    public static ResolvedSiegePose resolve(LivingEntity rider, @Nullable AbstractSiegeEntity siege, float age) {
        float partialTick = Mth.clamp(age - rider.tickCount, 0.0F, 1.0F);
        if (siege == null) {
            StructureClimbingSystem.ClimbPoseState climb =
                    StructureClimbingSystem.getClimbPoseState(rider, partialTick);
            return rider.getVehicle() == null && climb.weight() > 0.0F
                    ? climbing(SiegePoseProfiles.SIEGE_LADDER_CLIMB,
                    climb.weight(), climb.inclineRadians(), climb.uphillYawDegrees())
                    : null;
        }

        if (siege instanceof SiegeTowerEntity tower) {
            int crewSlot = tower.getPushingCrewSlot(rider);
            if (crewSlot >= 0) {
                return fixed(SiegePoseProfiles.towerPushing(crewSlot));
            }
        }
        if (siege instanceof ArcballistaEntity arcballista) {
            arcballista.updateRenderedAim(partialTick);
            return aimed(SiegePoseProfiles.ARCBALLISTA,
                    arcballista.getRenderedModelPitch(), arcballista.getRenderedAimPitch());
        }
        if (siege instanceof TowerCrossbowEntity towerCrossbow) {
            towerCrossbow.updateRenderedAim(partialTick);
            return aimed(SiegePoseProfiles.TOWER_CROSSBOW,
                    towerCrossbow.getRenderedModelPitch(), towerCrossbow.getRenderedAimPitch());
        }
        if (siege instanceof AbstractFieldGunEntity fieldGun) {
            fieldGun.updateRenderedAim(partialTick);
            SiegePoseProfile profile = fieldGun instanceof CulverinEntity
                    ? SiegePoseProfiles.CULVERIN
                    : SiegePoseProfiles.SERPENTINE;
            return aimed(profile, fieldGun.getRenderedModelPitch(), fieldGun.getRenderedAimPitch());
        }
        if (siege instanceof MonsMegEntity monsMeg) {
            monsMeg.updateRenderedAim(partialTick);
            return aimed(SiegePoseProfiles.MONS_MEG,
                    monsMeg.getRenderedModelPitch(), monsMeg.getRenderedAimPitch());
        }
        if (siege instanceof HwachaEntity hwacha) {
            hwacha.updateRenderedAim(partialTick);
            return aimed(SiegePoseProfiles.HWACHA,
                    hwacha.getRenderedModelPitch(), hwacha.getRenderedAimPitch());
        }
        if (siege instanceof BatteringRamEntity batteringRam) {
            int seat = batteringRam.getPassengers().indexOf(rider);
            return fixed(seat == 1
                    ? SiegePoseProfiles.BATTERING_RAM_SECOND
                    : SiegePoseProfiles.BATTERING_RAM_FIRST);
        }
        if (siege instanceof OnagerEntity onager) {
            return fixed(onager.isLaunchPayload(rider)
                    ? SiegePoseProfiles.ONAGER_PAYLOAD
                    : SiegePoseProfiles.ONAGER);
        }
        if (siege instanceof MantletEntity) {
            return fixed(SiegePoseProfiles.MANTLET);
        }
        if (siege instanceof TrebuchetEntity) {
            return fixed(SiegePoseProfiles.TREBUCHET);
        }
        return null;
    }

    private static ResolvedSiegePose fixed(SiegePoseProfile profile) {
        return new ResolvedSiegePose(profile, 0.0F, 0.0F);
    }

    private static ResolvedSiegePose aimed(SiegePoseProfile profile, float modelPitch, float aimPitch) {
        return new ResolvedSiegePose(profile, modelPitch, aimPitch);
    }

    private static ResolvedSiegePose climbing(SiegePoseProfile profile, float blendWeight,
                                              float inclineRadians, float uphillYawDegrees) {
        return new ResolvedSiegePose(profile, 0.0F, 0.0F, true, blendWeight, inclineRadians,
                uphillYawDegrees);
    }
}
