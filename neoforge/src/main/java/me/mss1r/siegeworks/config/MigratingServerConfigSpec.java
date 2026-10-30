package me.mss1r.siegeworks.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableCommentedConfig;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class MigratingServerConfigSpec implements IConfigSpec {
    private final ModConfigSpec delegate;
    private ModConfig owner;

    public MigratingServerConfigSpec(ModConfigSpec delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public void validateSpec(ModConfig config) {
        owner = config;
        delegate.validateSpec(config);
    }

    @Override
    public boolean isCorrect(UnmodifiableCommentedConfig config) {
        // Defer correction until acceptConfig exposes the file path. Never let FML remove legacy values first.
        return EngineConfigMigration.needed(config) || delegate.isCorrect(config);
    }

    @Override
    public void correct(CommentedConfig config) {
        delegate.correct(config);
    }

    @Override
    public void acceptConfig(ILoadedConfig loaded) {
        if (loaded != null && EngineConfigMigration.needed(loaded.config())) {
            java.nio.file.Path path = null;
            try {
                path = owner.getFullPath();
            } catch (IllegalStateException ignored) {
                // Synced/default client configs have no file; only the authoritative server migrates files.
            }
            if (path != null) {
                EngineConfigMigration.migrate(loaded.config(), path);
                delegate.correct(loaded.config());
                delegate.acceptConfig(loaded);
                loaded.save();
                return;
            }
        }
        delegate.acceptConfig(loaded);
    }
}
