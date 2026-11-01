package me.mss1r.siegeworks.integration.recruits;

import dev.architectury.platform.Platform;
import me.mss1r.siegeworks.gametest.SharedGameTests;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraftforge.gametest.GameTestHolder;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;

@GameTestHolder("siegeworks_rts_regression")
public final class NativeRtsGameTests {
    @GameTestGenerator
    public static Collection<TestFunction> scenarios() {
        if (!Platform.isModLoaded("recruits") || !Platform.isModLoaded("recruitsrtscommand")) return List.of();
        return SharedGameTests.testsFor(RtsIntegrationGameTests.class, "siegeworks_rts_regression",
                UnaryOperator.identity(), Set.of());
    }
}
