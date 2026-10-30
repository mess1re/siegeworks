package me.mss1r.siegeworks.client.pose;

import me.mss1r.siegeworks.client.pose.generated.GeneratedSiegePoses;

public final class SiegePoseProfiles {
    public static final SiegePoseProfile ARCBALLISTA = profile(
            GeneratedSiegePoses.ARCBALLISTA,
            new SiegeAimBinding(0.0F, -2.75F, 1.0F),
            SiegeHeadPolicy.aim(true),
            8.0F
    );
    public static final SiegePoseProfile TOWER_CROSSBOW = profile(
            GeneratedSiegePoses.TOWER_CROSSBOW,
            new SiegeAimBinding(0.35F, 1.5F, 1.5F),
            SiegeHeadPolicy.aim(true),
            4.0F
    );
    public static final SiegePoseProfile SERPENTINE = fieldGun(GeneratedSiegePoses.SERPENTINE);
    public static final SiegePoseProfile CULVERIN = fieldGun(GeneratedSiegePoses.CULVERIN);
    public static final SiegePoseProfile MONS_MEG = profile(
            GeneratedSiegePoses.MONS_MEG,
            new SiegeAimBinding(0.0F, 1.0F, 1.0F),
            SiegeHeadPolicy.aim(true),
            10.0F
    );
    public static final SiegePoseProfile HWACHA = profile(
            GeneratedSiegePoses.HWACHA,
            new SiegeAimBinding(0.0F, 1.0F, 0.0F),
            SiegeHeadPolicy.aim(true),
            10.0F
    );
    public static final SiegePoseProfile ONAGER = profile(
            GeneratedSiegePoses.ONAGER,
            SiegeAimBinding.NONE,
            SiegeHeadPolicy.look(35.0F, 35.0F),
            8.0F
    );
    public static final SiegePoseProfile MANTLET = profile(
            GeneratedSiegePoses.MANTLET,
            SiegeAimBinding.NONE,
            SiegeHeadPolicy.look(35.0F, 35.0F),
            10.0F
    );
    public static final SiegePoseProfile BATTERING_RAM_FIRST = profile(
            GeneratedSiegePoses.BATTERING_RAM_FIRST,
            SiegeAimBinding.NONE,
            SiegeHeadPolicy.look(35.0F, 35.0F),
            10.0F
    );
    public static final SiegePoseProfile BATTERING_RAM_SECOND = profile(
            GeneratedSiegePoses.BATTERING_RAM_SECOND,
            SiegeAimBinding.NONE,
            SiegeHeadPolicy.look(35.0F, 35.0F),
            10.0F
    );
    public static final SiegePoseProfile TREBUCHET = profile(
            GeneratedSiegePoses.TREBUCHET,
            SiegeAimBinding.NONE,
            SiegeHeadPolicy.look(45.0F, 45.0F),
            0.0F
    );
    public static final SiegePoseProfile SIEGE_LADDER_CLIMB = profile(
            GeneratedSiegePoses.SIEGE_LADDER_CLIMB,
            SiegeAimBinding.NONE,
            SiegeHeadPolicy.look(70.0F, 55.0F),
            0.0F
    );
    private static final SiegePoseProfile[] TOWER_PUSHING = {
            towerPushing(GeneratedSiegePoses.TOWER_PUSHING_1),
            towerPushing(GeneratedSiegePoses.TOWER_PUSHING_2),
            towerPushing(GeneratedSiegePoses.TOWER_PUSHING_3),
            towerPushing(GeneratedSiegePoses.TOWER_PUSHING_4),
            towerPushing(GeneratedSiegePoses.TOWER_PUSHING_5),
            towerPushing(GeneratedSiegePoses.TOWER_PUSHING_6)
    };
    public static final SiegePoseProfile ONAGER_PAYLOAD = profile(
            pose(
                    "runtime/onager_payload",
                    rotation(0.0F, 0.0F, 0.0F),
                    rotation(0.0F, 0.0F, 0.0F),
                    rotation(0.0F, 0.0F, 0.0F),
                    rotation(-10.0F, 0.0F, 0.0F),
                    rotation(-10.0F, 0.0F, 0.0F),
                    rotation(-81.0F, 18.0F, 4.5F),
                    rotation(-81.0F, -18.0F, -4.5F)
            ),
            SiegeAimBinding.NONE,
            SiegeHeadPolicy.look(80.0F, 80.0F),
            0.0F
    );

    private SiegePoseProfiles() {
    }

    public static SiegePoseProfile towerPushing(int crewSlot) {
        int index = Math.max(0, Math.min(crewSlot, TOWER_PUSHING.length - 1));
        return TOWER_PUSHING[index];
    }

    private static SiegePoseProfile towerPushing(SiegePoseDefinition definition) {
        return profile(
                definition,
                SiegeAimBinding.NONE,
                SiegeHeadPolicy.look(25.0F, 35.0F),
                10.0F
        );
    }

    private static SiegePoseProfile fieldGun(SiegePoseDefinition definition) {
        return profile(
                definition,
                new SiegeAimBinding(0.0F, 2.75F, 0.0F),
                SiegeHeadPolicy.aim(true),
                10.0F
        );
    }

    private static SiegePoseProfile profile(SiegePoseDefinition definition, SiegeAimBinding aimBinding,
                                            SiegeHeadPolicy headPolicy, float legSwingScale) {
        return new SiegePoseProfile(definition, aimBinding, headPolicy, legSwingScale);
    }

    private static SiegePoseDefinition pose(String source,
                                            SiegePoseRotation upperBody, SiegePoseRotation head,
                                            SiegePoseRotation body, SiegePoseRotation rightArm,
                                            SiegePoseRotation leftArm, SiegePoseRotation rightLeg,
                                            SiegePoseRotation leftLeg) {
        return new SiegePoseDefinition(
                source, upperBody,
                head, body, rightArm, leftArm, rightLeg, leftLeg
        );
    }

    private static SiegePoseRotation rotation(float xRot, float yRot, float zRot) {
        return SiegePoseRotation.fromDegrees(xRot, yRot, zRot);
    }
}
