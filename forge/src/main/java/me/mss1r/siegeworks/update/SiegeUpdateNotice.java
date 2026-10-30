package me.mss1r.siegeworks.update;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.VersionChecker;

import java.net.URI;
import java.util.Optional;

/** Consumes the loader's check once, without starting another request. */
public final class SiegeUpdateNotice {
    private final UpdateNoticeState state = new UpdateNoticeState();
    public boolean finished() { return state.finished(); }
    public Optional<Component> take(VersionChecker.CheckResult result) {
        return state.take(result.status() == VersionChecker.Status.PENDING,
                result.status() == VersionChecker.Status.OUTDATED || result.status() == VersionChecker.Status.BETA_OUTDATED,
                result.target() == null ? null : result.target().toString(), result.url());
    }
}
