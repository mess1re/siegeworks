package me.mss1r.siegeworks.gametest;

import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.TestFunction;
import java.util.*;

public final class FabricGameTestBridge {
    @GameTestGenerator
    public static Collection<TestFunction> scenarios() {
        String selected = System.getProperty("siegeworks.test.namespaces", "");
        Set<String> namespaces = selected.isBlank() ? Set.of() : Set.copyOf(Arrays.asList(selected.split(",")));
        return SharedGameTests.tests(template -> "siegeworks_fabric_tests:empty", namespaces);
    }
}
