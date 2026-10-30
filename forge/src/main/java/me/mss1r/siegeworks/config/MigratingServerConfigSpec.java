package me.mss1r.siegeworks.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.utils.UnmodifiableConfigWrapper;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.IConfigSpec;

public final class MigratingServerConfigSpec extends UnmodifiableConfigWrapper<ForgeConfigSpec>
        implements IConfigSpec<MigratingServerConfigSpec> {
    public MigratingServerConfigSpec(ForgeConfigSpec delegate) {
        super(delegate);
    }

    @Override
    public void acceptConfig(CommentedConfig data) {
        if (data instanceof CommentedFileConfig file) {
            EngineConfigMigration.migrate(data, file.getNioPath());
        }
        config.acceptConfig(data);
    }

    @Override
    public boolean isCorrecting() {
        return config.isCorrecting();
    }

    @Override
    public boolean isCorrect(CommentedConfig data) {
        return !EngineConfigMigration.needed(data) && config.isCorrect(data);
    }

    @Override
    public int correct(CommentedConfig data) {
        if (data instanceof CommentedFileConfig file) {
            EngineConfigMigration.migrate(data, file.getNioPath());
        }
        return config.correct(data);
    }

    @Override
    public void afterReload() {
        config.afterReload();
    }
}
