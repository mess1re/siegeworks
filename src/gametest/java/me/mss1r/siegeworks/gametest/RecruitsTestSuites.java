package me.mss1r.siegeworks.gametest;

import net.minecraft.gametest.framework.TestFunction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.function.UnaryOperator;

/** Loaded only when the optional Recruits dependency is present. */
final class RecruitsTestSuites {
    static Collection<TestFunction> tests(UnaryOperator<String> templates, Set<String> namespaces) {
        var tests = new ArrayList<TestFunction>();
        tests.addAll(SharedGameTests.testsFor(RecruitsCrewAndFireGameTests.class, "siegeworks_recruits", templates, namespaces));
        tests.addAll(SharedGameTests.testsFor(RecruitsLadderTrafficGameTests.class, "siegeworks_ladder_regression", templates, namespaces));
        return tests;
    }
}
