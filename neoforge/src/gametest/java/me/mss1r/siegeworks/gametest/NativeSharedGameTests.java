package me.mss1r.siegeworks.gametest;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.TestFunction;
import java.util.Collection;
import java.util.Set;
import java.util.function.UnaryOperator;

@GameTestHolder("siegeworks")
public final class NativeSharedGameTests {
    @GameTestGenerator
    public static Collection<TestFunction> scenarios() {
        Set<String> namespaces = new java.util.HashSet<>(java.util.Arrays.asList(
                System.getProperty("siegeworks.test.namespaces", "").split(",")));
        namespaces.remove("");
        return SharedGameTests.tests(UnaryOperator.identity(), namespaces);
    }
}
