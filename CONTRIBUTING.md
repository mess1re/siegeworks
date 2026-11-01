# Contributing

## Source layout

Stonecutter selects Minecraft 1.20.1 or 1.21.1; Architectury connects the shared code to each loader.

- `src/main` contains shared gameplay, rendering and resources, with Stonecutter conditions for Minecraft API differences.
- `forge/src/main` contains Forge 1.20.1 entry points and adapters.
- `neoforge/src/main` contains NeoForge 1.21.1 entry points and adapters.
- `fabric/src/main` contains Fabric entry points and adapters for both versions.
- `src/gametest` contains shared runtime scenarios. Loader test entry points and event adapters live in `forge/src/gametest`, `neoforge/src/gametest` and `fabric/src/testmod`.

Generated sources under `versions/` and each loader's `versions/` directory are build output.

Forge-only integrations stay out of targets where their dependencies do not exist.

## Optional integrations

Code that references an optional mod belongs in its integration package and must only be initialized after checking that the mod is loaded.

The NeoForge and Fabric Recruits dependency is the [multiloader port](https://github.com/mess1re/recruits/tree/stonecutter-multiloader)
at the revision in `stonecutter.properties.toml`, fetched from JitPack as
`com.github.mess1re.recruits:neoforge-1.21.1:<revision>`. JitPack builds `forge-1.20.1`, `fabric-1.20.1`
and `fabric-1.21.1` from the same revision too.

This revision is a build/test API baseline, not a runtime fork requirement. Integration checks
for `modId=recruits`; Recruits is not included in Siegeworks jars. Crew and firing checks use `siegeworks_recruits`; owned troop traffic uses
`siegeworks_ladder_regression`. Legacy integration checks also use `siegeworks` and `siegeworks_ladder`. These need `-Penable_recruits_compat_runtime=true`.

## Mechanics, configuration and data

- Engine and projectile combat values belong in data definitions.
- Server pacing and policy belong in the server config.
- Loading order, controls, aiming constraints and animation events remain code when they define the mechanic itself.

Do not duplicate a value in code when it already comes from a data definition or server config.

## Verification

Collision, terrain movement, deployment, ladders, siege-tower ramps and projectile behavior have deterministic tests or GameTests. Changes in those areas should update the relevant coverage rather than rely only on a manual client check.

To exercise owned Recruits following move, follow and formation orders through ladders:

```sh
./gradlew :forge:1.20.1:runGameTestServer :neoforge:1.21.1:runGameTestServer -PgameTestNamespaces=siegeworks_ladder_regression -Penable_recruits_compat_runtime=true
```

These scenarios use the ordinary AI and check health, supported landings, route ownership, prolonged stalls and preservation of the original orders.

The regression namespace also exercises owned troops following their commander, formation orders, repeated point orders,
and a fifteen-metre ladder leading to a thin suspended platform. It checks ownership of the distant approach, unexpected
route loss and prolonged stalls, rather than only eventual arrival. For a live reproduction, use
`-Pdebug.recruits=true` with `runClient`, or `/siegeworks debug recruits true` in the world. Ladder traces are opt-in and
limited to one report per troop per second, with route phase, progress, blockage, destination and navigation ownership.

To check the Forge RTS map bridge through its registered order listeners and map-object actions:

```sh
./gradlew :forge:1.20.1:runGameTestServer -PgameTestNamespaces=siegeworks_rts_regression -Penable_recruits_compat_runtime=true -Penable_rts_compat_runtime=true
```

### Working with unpublished Axiomata changes

When SW needs Axiomata changes that are not published yet, build the matching Axiomata checkout
first and point local SW builds at its collected jars. This override is available on every loader; it neither bundles Axiomata
nor changes the published dependency pin:

```sh
./gradlew :neoforge:1.21.1:buildAndCollect -Paxiomata.localDir=/absolute/path/axiomata/build/libs/0.1.0-beta.9
./gradlew :forge:1.20.1:buildAndCollect -Paxiomata.localDir=/absolute/path/axiomata/build/libs/0.1.0-beta.9
./gradlew :fabric:1.20.1:buildAndCollect :fabric:1.21.1:buildAndCollect -Paxiomata.localDir=/absolute/path/axiomata/build/libs/0.1.0-beta.9
```

Use the same property for `runClient`, native `runGameTestServer` and Fabric `runGameTest`.

Fabric's standalone GameTest server does not run the ordinary server-start lifecycle used by Recruits.
Run the shared core scenarios there with Recruits disabled; use a normal client or dedicated server
for Recruits integration checks:

```sh
./gradlew :fabric:1.20.1:runGameTest :fabric:1.21.1:runGameTest -Penable_recruits_compat_runtime=false
```

The default dependency pin resolves all four Axiomata targets from JitPack; the local override is optional.

Shared scenarios use vanilla GameTest annotations and are registered by `SharedGameTests` through each loader's
entry point. Fabric compiles those sources directly; no loader rewrites test Java source.
`-PgameTestFilter=class.method` selects shared tests by a case-insensitive name fragment for isolated reproductions.
Run core physics, config migration and active troop traffic in separate servers. Migration fixtures deliberately change movement settings, and projectile scenarios must not share a world with troop traffic.

To check migration through the native loaders, including values loaded before config correction:

```sh
./gradlew :forge:1.20.1:runGameTestServer :neoforge:1.21.1:runGameTestServer -I scripts/config-migration-tests.init.gradle -PgameTestNamespaces=siegeworks_config,siegeworks_config_upgrade -Penable_recruits_compat_runtime=false -Penable_rts_compat_runtime=false
```

On Fabric, `runLifecycleTest` runs the same scenarios on an ordinary dedicated server and checks the result
before returning. Its disposable world is under `build/lifecycle-server`. Forge Config API Port defaults to global
server configs; repeat with `-Ptest.globalServerConfigs=false` to exercise per-world configs:

```sh
./gradlew :fabric:1.20.1:runLifecycleTest :fabric:1.21.1:runLifecycleTest -Penable_recruits_compat_runtime=false
./gradlew :fabric:1.20.1:runLifecycleTest :fabric:1.21.1:runLifecycleTest -Penable_recruits_compat_runtime=true -PgameTestNamespaces=siegeworks_recruits,siegeworks_ladder_regression
```

`remapTestmodJar` produces a separate Fabric test mod for checking the same scenarios against production jars.
The distributed Siegeworks jars do not contain these tests.

## Assets

Third-party assets need a traceable source and license entry in `THIRD_PARTY_NOTICES.md`.

Engine models live in `src/main/blockbench/models`. After changing one in Blockbench, export the geo file with
GeckoLib and the construction markup with Axiomata's Build Sections plugin (**File → Export → Construction Markup**)
to `src/main/resources/data/siegeworks/construction/<model>.json`.
