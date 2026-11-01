package me.mss1r.siegeworks.gametest;

import net.minecraftforge.gametest.GameTestHolder;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.TestFunction;
import java.util.Collection;
import java.util.Set;
import java.util.function.UnaryOperator;

@GameTestHolder("siegeworks")
public final class NativeSharedGameTests {
    @GameTestGenerator
    public static Collection<TestFunction> scenarios() {
        return SharedGameTests.tests(UnaryOperator.identity(), Set.of());
    }
}
