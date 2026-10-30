package me.mss1r.siegeworks.gametest;

import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.config.SiegeBlockDamage;
import me.mss1r.siegeworks.config.SiegeworksServerConfig;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
//? if <1.21 {
/*import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder(FractureConfigGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class FractureConfigGameTests {
    public static final String NAMESPACE = Siegeworks.MOD_ID + "_fracture";

    private FractureConfigGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 40, batch = "fracture_config")
    public static void higherFractureEnergyNeedsMoreHits(GameTestHelper helper) {
        double before = SiegeworksServerConfig.getStoneFractureEnergy();
        SiegeBlockDamage damageBefore = SiegeworksServerConfig.getBlockDamage();
        var ball = MinecraftVersionCompat.id(Siegeworks.MOD_ID, "cannon_ball");
        try {
            SiegeworksServerConfig.setBlockDamage(SiegeBlockDamage.EVERYWHERE);
            int[] hits = new int[3];
            double[] energies = {25_000.0D, 50_000.0D, 100_000.0D};
            for (int index = 0; index < energies.length; index++) {
                SiegeworksServerConfig.setStoneFractureEnergy(energies[index]);
                helper.getLevel().random.setSeed(7L);
                hits[index] = SiegeImpactBalanceGameTests.shotsToBreach(helper, ball, 300.0D, 6);
                Siegeworks.LOG.info("Stone fracture config: {} J/m3, {} hits to breach", energies[index], hits[index]);
            }
            helper.assertTrue(hits[0] > 0 && hits[0] < hits[1] && hits[1] < hits[2],
                    "Raising fracture energy did not raise the hits needed: "
                            + hits[0] + ", " + hits[1] + ", " + hits[2]);
        } finally {
            SiegeworksServerConfig.setStoneFractureEnergy(before);
            SiegeworksServerConfig.setBlockDamage(damageBefore);
        }
        helper.succeed();
    }
}
