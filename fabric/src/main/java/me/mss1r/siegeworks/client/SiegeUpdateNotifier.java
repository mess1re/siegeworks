package me.mss1r.siegeworks.client;

import com.google.gson.JsonParser;
import dev.architectury.event.events.client.ClientTickEvent;
import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.config.SiegeworksClientConfig;
import me.mss1r.siegeworks.update.UpdateNoticeState;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.minecraft.client.Minecraft;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/** Fabric supplies no FML version checker, so perform one bounded asynchronous check per launch. */
public final class SiegeUpdateNotifier {
    private static final String FEED = "https://raw.githubusercontent.com/mess1re/siegeworks/main/updates/fabric.json";
    private static final String CURRENT_VERSION = FabricLoader.getInstance().getModContainer(Siegeworks.MOD_ID).orElseThrow().getMetadata().getVersion().getFriendlyString();
    private static final UpdateNoticeState NOTICE = new UpdateNoticeState();
    private static final Check NONE = new Check(false, null, null);
    private static CompletableFuture<Check> check;
    private static int ticks = 100;
    private SiegeUpdateNotifier() {}

    public static void register() { ClientTickEvent.CLIENT_POST.register(SiegeUpdateNotifier::tick); }

    private static void tick(Minecraft minecraft) {
        if (NOTICE.finished() || minecraft.player == null || minecraft.screen != null || !SiegeworksClientConfig.showUpdateNotice()) return;
        if (check == null) {
            if (--ticks <= 0) check = CompletableFuture.supplyAsync(SiegeUpdateNotifier::fetch).exceptionally(error -> NONE);
        } else if (check.isDone()) {
            Check result = check.join();
            NOTICE.take(false, result.outdated(), result.target(), result.url()).ifPresent(minecraft.player::sendSystemMessage);
        }
    }

    private static Check fetch() {
        try {
            var connection = URI.create(FEED).toURL().openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "Siegeworks/" + CURRENT_VERSION + " Fabric");
            byte[] bytes;
            try (var stream = connection.getInputStream()) { bytes = stream.readNBytes(65537); }
            if (bytes.length > 65536) return NONE;
            var data = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
            var minecraft = FabricLoader.getInstance().getModContainer("minecraft").orElseThrow().getMetadata().getVersion().getFriendlyString();
            var promos = data.getAsJsonObject("promos");
            var latest = promos == null ? null : promos.get(minecraft + "-latest");
            if (latest == null) return NONE;
            String target = latest.getAsString();
            String url = data.has("homepage") ? data.get("homepage").getAsString() : null;
            return new Check(Version.parse(target).compareTo(Version.parse(CURRENT_VERSION)) > 0, target, url);
        } catch (Exception ignored) { return NONE; }
    }
    private record Check(boolean outdated, String target, String url) {}
}
