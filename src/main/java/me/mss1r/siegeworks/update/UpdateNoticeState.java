package me.mss1r.siegeworks.update;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;

import java.net.URI;
import java.util.Optional;

/** Loader-neutral formatting and once-per-launch state for update notices. */
public final class UpdateNoticeState {
    private boolean finished;

    public boolean finished() {
        return finished;
    }

    public Optional<Component> take(boolean pending, boolean outdated, String target, String url) {
        if (finished || pending) return Optional.empty();
        finished = true;
        if (!outdated || target == null) return Optional.empty();

        Component link = Component.translatable("message.siegeworks.update.open")
                .withStyle(style -> style.withColor(ChatFormatting.YELLOW).withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, downloadUrl(url))));
        return Optional.of(Component.translatable("message.siegeworks.update.available", target)
                .append(" ").append(link));
    }

    private static String downloadUrl(String url) {
        if (url != null) {
            try {
                URI uri = URI.create(url);
                if ("https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null) {
                    return url;
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        return "https://www.curseforge.com/projects/1713906";
    }
}
