package me.mss1r.siegeworks.gametest;

import me.mss1r.siegeworks.config.ConfigMigrationGameTests;
import net.minecraft.gametest.framework.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/** Registers unchanged vanilla GameTest scenarios through each loader's test entry point. */
public final class SharedGameTests {
    private record Suite(Class<?> type, String namespace) {}
    private static final List<Suite> SUITES = List.of(
            new Suite(ManualRecipeGameTests.class, "siegeworks_recipes"),
            new Suite(SiegeAdvancementGameTests.class, "siegeworks_advancements"),
            new Suite(FirearmGameTests.class, "siegeworks_firearms"),
            new Suite(CastingGameTests.class, "siegeworks_stone_ammunition"),
            new Suite(StoneAmmunitionGameTests.class, "siegeworks_stone_ammunition"),
            new Suite(SiegeImpactBalanceGameTests.class, "siegeworks_impact_balance"),
            new Suite(BoltBalanceGameTests.class, BoltBalanceGameTests.NAMESPACE),
            new Suite(ShotFlightGameTests.class, "siegeworks"),
            new Suite(PhysicalImpactGameTests.class, PhysicalImpactGameTests.NAMESPACE),
            new Suite(FragmentCalibrationGameTests.class, PhysicalImpactGameTests.NAMESPACE),
            new Suite(StructureDamageGameTests.class, PhysicalImpactGameTests.NAMESPACE),
            new Suite(ProjectileCalibrationGameTests.class, PhysicalImpactGameTests.NAMESPACE),
            new Suite(ConfigMigrationGameTests.class, "siegeworks_config"),
            new Suite(AimRegressionGameTests.class, "siegeworks_aim_regression"),
            new Suite(BannerGameTests.class, "siegeworks_banners"),
            new Suite(BatteringRamCollisionGameTests.class, BatteringRamCollisionGameTests.NAMESPACE),
            new Suite(BoltThrowerCollisionGameTests.class, BoltThrowerCollisionGameTests.NAMESPACE),
            new Suite(ConfigUpgradeGameTests.class, ConfigUpgradeGameTests.NAMESPACE),
            new Suite(DraftTeamGameTests.class, "siegeworks_draft"),
            new Suite(FieldGunCollisionGameTests.class, FieldGunCollisionGameTests.NAMESPACE),
            new Suite(HwachaCollisionGameTests.class, HwachaCollisionGameTests.NAMESPACE),
            new Suite(LadderBraceTargetGameTests.class, LadderBraceTargetGameTests.NAMESPACE),
            new Suite(MantletCollisionGameTests.class, MantletCollisionGameTests.NAMESPACE),
            new Suite(MonsMegCollisionGameTests.class, MonsMegCollisionGameTests.NAMESPACE),
            new Suite(OnagerCollisionGameTests.class, OnagerCollisionGameTests.NAMESPACE),
            new Suite(SiegeGravityGameTests.class, "siegeworks_gravity"),
            new Suite(SiegeLadderCollisionGameTests.class, SiegeLadderCollisionGameTests.NAMESPACE),
            new Suite(SiegeLadderQueueGameTests.class, SiegeLadderQueueGameTests.NAMESPACE),
            new Suite(SiegeStackReloadGameTests.class, SiegeStackReloadGameTests.NAMESPACE),
            new Suite(SiegeTerrainCollisionGameTests.class, SiegeTerrainCollisionGameTests.NAMESPACE),
            new Suite(SiegeTowerCollisionGameTests.class, SiegeTowerCollisionGameTests.NAMESPACE),
            new Suite(StructureCollisionGameTests.class, StructureCollisionGameTests.NAMESPACE),
            new Suite(TrebuchetCollisionGameTests.class, TrebuchetCollisionGameTests.NAMESPACE));

    private SharedGameTests() {}

    public static Collection<TestFunction> tests(UnaryOperator<String> templates, Set<String> namespaces) {
        List<TestFunction> tests = new ArrayList<>(tests(SUITES, templates, namespaces));
        if (dev.architectury.platform.Platform.isModLoaded("recruits")) {
            tests.addAll(RecruitsTestSuites.tests(templates, namespaces));
        }
        return tests;
    }

    public static Collection<TestFunction> testsFor(Class<?> type, String namespace,
                                              UnaryOperator<String> templates, Set<String> namespaces) {
        return tests(List.of(new Suite(type, namespace)), templates, namespaces);
    }

    private static Collection<TestFunction> tests(List<Suite> suites, UnaryOperator<String> templates, Set<String> namespaces) {
        List<TestFunction> tests = new ArrayList<>();
        for (Suite suite : suites) {
            Arrays.stream(suite.type().getDeclaredMethods()).sorted(Comparator.comparing(Method::getName)).forEach(method -> {
                if (method.isAnnotationPresent(BeforeBatch.class) || method.isAnnotationPresent(AfterBatch.class)) {
                    throw new IllegalStateException("Batch lifecycle callbacks need a loader adapter: " + method);
                }
                GameTest test = method.getAnnotation(GameTest.class);
                if (test == null) return;
                if (!Modifier.isStatic(method.getModifiers()) || method.getReturnType() != void.class
                        || !Arrays.equals(method.getParameterTypes(), new Class<?>[]{GameTestHelper.class})) {
                    throw new IllegalStateException("Invalid shared GameTest method: " + method);
                }
                String template = test.template().contains(":") ? test.template() : suite.namespace() + ":" + test.template();
                if (!namespaces.isEmpty() && !namespaces.contains(template.substring(0, template.indexOf(':')))) return;
                String name = suite.type().getSimpleName().toLowerCase(Locale.ROOT) + "." + method.getName().toLowerCase(Locale.ROOT);
                String filter = System.getProperty("siegeworks.test.filter", "").toLowerCase(Locale.ROOT);
                if (!filter.isEmpty() && !name.contains(filter)) return;
                Consumer<GameTestHelper> run = helper -> invoke(method, helper);
                tests.add(new TestFunction(test.batch(), name, templates.apply(template),
                        StructureUtils.getRotationForRotationSteps(test.rotationSteps()), test.timeoutTicks(), test.setupTicks(), test.required(),
                        //? if <1.21 {
                        /*test.requiredSuccesses(), test.attempts(), run));
                        *///?} else {
                        test.manualOnly(), test.attempts(), test.requiredSuccesses(), test.skyAccess(), run));
                        //?}
            });
        }
        return tests;
    }

    private static void invoke(Method method, GameTestHelper helper) {
        try {
            method.invoke(null, helper);
        } catch (InvocationTargetException exception) {
            if (exception.getCause() instanceof RuntimeException runtime) throw runtime;
            if (exception.getCause() instanceof Error error) throw error;
            throw new RuntimeException("Shared GameTest failed: " + method, exception.getCause());
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot invoke shared GameTest: " + method, exception);
        }
    }
}
