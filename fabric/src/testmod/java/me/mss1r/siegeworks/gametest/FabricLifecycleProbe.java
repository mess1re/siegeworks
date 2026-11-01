package me.mss1r.siegeworks.gametest;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Rotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Runs server-start integration scenarios on an ordinary dedicated server, not GameTestServer. */
public final class FabricLifecycleProbe implements ModInitializer {
    private Collection<GameTestInfo> tests;
    private int ticks;
    private long tickBudget;
    private int failures = 1;

    @Override public void onInitialize() {
        if (!Boolean.getBoolean("siegeworks.test.lifecycle")) return;
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            Thread serverThread = Thread.currentThread();
            // The JVM shutdown hook joins the server; exit only after that thread has finished saving.
            new Thread(() -> {
                try { serverThread.join(); }
                catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new IllegalStateException(exception); }
                System.exit(failures == 0 ? 0 : 1);
            }, "Lifecycle test completion").start();
        });
        ServerLifecycleEvents.SERVER_STARTED.register(server -> server.execute(() -> {
            Set<String> namespaces = Set.copyOf(Arrays.asList(System.getProperty("siegeworks.test.namespaces").split(",")));
            var functions = SharedGameTests.tests(template -> "siegeworks_fabric_tests:empty", namespaces);
            //? if <1.21 {
            /*tickBudget = functions.stream().mapToLong(test -> test.getMaxTicks() + test.getSetupTicks() + 200L).sum();
            *///?} else {
            tickBudget = functions.stream().mapToLong(test -> test.maxTicks() + test.setupTicks() + 200L).sum();
            //?}
            if (functions.isEmpty()) throw new IllegalStateException("No lifecycle tests selected");
            //? if <1.21 {
            /*tests = GameTestRunner.runTests(functions, new BlockPos(0, 100, 0), Rotation.NONE,
                    server.overworld(), GameTestTicker.SINGLETON, 8);
            *///?} else {
            var infos = functions.stream().map(test -> new GameTestInfo(test, Rotation.NONE,
                    server.overworld(), RetryOptions.noRetries())).toList();
            var runner = GameTestRunner.Builder.fromInfo(infos, server.overworld())
                    .newStructureSpawner(new StructureGridSpawner(new BlockPos(0, 100, 0), 8, false)).build();
            tests = runner.getTestInfos();
            runner.start();
            //?}
        }));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (tests == null || (++ticks < tickBudget && tests.stream().anyMatch(test -> !test.isDone()))) return;
            var failed = tests.stream().filter(test -> !test.hasSucceeded()).toList();
            failures = failed.size();
            for (var test : failed) LogUtils.getLogger().error("LIFECYCLE_TEST FAIL {}: {}", test.getTestName(), test.getError());
            try {
                Path report = Path.of(System.getProperty("siegeworks.test.report"));
                Files.createDirectories(report.getParent());
                Files.writeString(report, "{\"tests\":" + tests.size() + ",\"failures\":" + failed.size() + "}");
            } catch (java.io.IOException exception) { throw new IllegalStateException(exception); }
            LogUtils.getLogger().info("LIFECYCLE_TEST completed: {} tests, {} failures", tests.size(), failed.size());
            tests = null;
            server.halt(false);
        });
    }
}
