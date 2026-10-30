package me.mss1r.siegeworks.gametest;

import com.electronwill.nightconfig.core.CommentedConfig;
import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.config.SiegeworksClientConfig;
import me.mss1r.siegeworks.config.SiegeworksConfigSpec;
import me.mss1r.siegeworks.config.SiegeworksServerConfig;
import me.mss1r.siegeworks.config.SiegeworksEngineConfig;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
//? if forge {
/*import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder(ConfigUpgradeGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class ConfigUpgradeGameTests {
    public static final String NAMESPACE = Siegeworks.MOD_ID + "_config";

    @GameTest(template = "empty")
    public static void unversionedServerConfigKeepsItsSettings(GameTestHelper helper) {
        CommentedConfig old = CommentedConfig.inMemory();
        old.set("rules.ownership.enforceOwnership", false);
        old.set("rules.incendiary.fuseTicks", 140);
        SiegeworksServerConfig.SPEC.unwrap().correct(old);
        helper.assertTrue(Integer.valueOf(SiegeworksConfigSpec.FORMAT_VERSION).equals(old.get("configVersion")),
                "The beta.5 config did not receive its layout version");
        helper.assertTrue(Integer.valueOf(140).equals(old.get("rules.incendiary.fuseTicks"))
                        && Boolean.FALSE.equals(old.get("rules.ownership.enforceOwnership")),
                "Adding settings reset valid beta.5 values");
        helper.assertTrue(Double.valueOf(50_000.0D).equals(old.get("rules.terrain.stoneFractureEnergy"))
                        && SiegeworksServerConfig.SPEC.unwrap().isCorrect(old),
                "New server settings were not added correctly");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void engineConfigFillsMissingSettingsWithoutOverwritingSeconds(GameTestHelper helper) {
        CommentedConfig existing = CommentedConfig.inMemory();
        existing.set("culverin.movement.playerSpeed", 2.5D);
        existing.set("culverin.operation.recoverySeconds", 60.0D);
        SiegeworksEngineConfig.SPEC.unwrap().correct(existing);
        helper.assertTrue(Double.valueOf(2.5D).equals(existing.get("culverin.movement.playerSpeed"))
                        && Double.valueOf(60.0D).equals(existing.get("culverin.operation.recoverySeconds"))
                        && SiegeworksEngineConfig.SPEC.unwrap().isCorrect(existing),
                "Filling the engine config changed existing values or left missing fields");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void clientConfigAddsNoticeWithoutResettingPreferences(GameTestHelper helper) {
        CommentedConfig old = CommentedConfig.inMemory();
        old.set("recruitsMap.showTroops", false);
        SiegeworksClientConfig.SPEC.unwrap().correct(old);
        helper.assertTrue(Boolean.FALSE.equals(old.get("recruitsMap.showTroops"))
                        && Boolean.TRUE.equals(old.get("updates.showNotice"))
                        && Integer.valueOf(SiegeworksConfigSpec.FORMAT_VERSION).equals(old.get("configVersion")),
                "The client upgrade reset a preference or omitted a new setting");
        old.set("updates.showNotice", false);
        SiegeworksClientConfig.SPEC.unwrap().correct(old);
        helper.assertTrue(Boolean.FALSE.equals(old.get("updates.showNotice"))
                        && SiegeworksClientConfig.SPEC.unwrap().isCorrect(old),
                "A later config load re-enabled update notices");
        helper.succeed();
    }
}
