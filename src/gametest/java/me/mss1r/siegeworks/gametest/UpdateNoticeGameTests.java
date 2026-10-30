package me.mss1r.siegeworks.gametest;

import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.update.SiegeUpdateNotice;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.ClickEvent;
//? if <1.21 {
/*import net.minecraftforge.fml.VersionChecker;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.fml.VersionChecker;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.util.Map;

@GameTestHolder(UpdateNoticeGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class UpdateNoticeGameTests {
    public static final String NAMESPACE = Siegeworks.MOD_ID + "_updates";
    private static final ComparableVersion TARGET = new ComparableVersion("0.1.0-beta.6");
    private static final String PAGE = "https://modrinth.com/mod/siegeworks/versions?g=1.20.1&l=forge";

    @GameTest(template = "empty")
    public static void pendingCheckWaitsThenNotifiesOnlyOnce(GameTestHelper helper) {
        var notice = new SiegeUpdateNotice();
        helper.assertTrue(notice.take(new VersionChecker.CheckResult(
                        VersionChecker.Status.PENDING, null, null, null)).isEmpty() && !notice.finished(),
                "A pending check was treated as final");
        var result = result(VersionChecker.Status.BETA_OUTDATED, PAGE);
        var message = notice.take(result).orElseThrow();
        helper.assertTrue(message.getSiblings().size() == 2 && notice.finished()
                        && notice.take(result).isEmpty(),
                "The update notice was repeated or did not finish its check");
        ClickEvent click = message.getSiblings().get(1).getStyle().getClickEvent();
        helper.assertTrue(click != null && click.getAction() == ClickEvent.Action.OPEN_URL
                        && PAGE.equals(click.getValue()),
                "The notice does not link to the matching download page");
        helper.assertTrue(!message.getString().contains("CHANGELOG_SENTINEL"),
                "A changelog was appended to the notice");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void currentOfflineAndDevelopmentBuildsStayQuiet(GameTestHelper helper) {
        for (var status : new VersionChecker.Status[] {VersionChecker.Status.UP_TO_DATE,
                VersionChecker.Status.AHEAD, VersionChecker.Status.BETA, VersionChecker.Status.FAILED}) {
            var notice = new SiegeUpdateNotice();
            helper.assertTrue(notice.take(result(status, PAGE)).isEmpty() && notice.finished(),
                    "An update notice appeared for " + status);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void stableUpdateAlsoNotifies(GameTestHelper helper) {
        helper.assertTrue(new SiegeUpdateNotice().take(result(VersionChecker.Status.OUTDATED, PAGE)).isPresent(),
                "Only beta updates generated a notice");
        helper.assertTrue(new SiegeUpdateNotice().take(new VersionChecker.CheckResult(
                        VersionChecker.Status.OUTDATED, null, Map.of(), PAGE)).isEmpty(),
                "An update with no target version generated a notice");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void invalidDownloadLinksUseTheProjectPage(GameTestHelper helper) {
        for (String url : new String[] {"file:///etc/passwd", "javascript:alert(1)", "broken url"}) {
            var message = new SiegeUpdateNotice().take(result(VersionChecker.Status.OUTDATED, url)).orElseThrow();
            ClickEvent click = message.getSiblings().get(1).getStyle().getClickEvent();
            helper.assertTrue(click != null && "https://www.curseforge.com/projects/1713906".equals(click.getValue()),
                    "An invalid download URL made it into chat: " + url);
        }
        helper.succeed();
    }

    private static VersionChecker.CheckResult result(VersionChecker.Status status, String url) {
        return new VersionChecker.CheckResult(status, TARGET, Map.of(TARGET, "CHANGELOG_SENTINEL"), url);
    }
}
