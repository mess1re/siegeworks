# Contributing

## Source layout

Stonecutter selects Minecraft 1.20.1 or 1.21.1; Architectury connects the shared code to each loader.

- `src/main` contains shared gameplay, rendering and resources, with Stonecutter conditions for Minecraft API differences.
- `forge/src/main` contains Forge 1.20.1 entry points and adapters.
- `neoforge/src/main` contains NeoForge 1.21.1 entry points and adapters.
- `fabric/src/main` contains Fabric entry points and adapters for both versions.
- `src/gametest` contains shared runtime scenarios; `fabric/src/testmod` supplies the Fabric test entry points.

Generated sources under `versions/` and each loader's `versions/` directory are build output.

Forge-only integrations stay out of targets where their dependencies do not exist.

## Optional integrations

Code that references an optional mod belongs in its integration package and must only be initialized after checking that the mod is loaded.

The NeoForge and Fabric Recruits dependency is the [multiloader port](https://github.com/mess1re/recruits/tree/stonecutter-multiloader)
at the revision in `stonecutter.properties.toml`, fetched from JitPack as
`com.github.mess1re.recruits:neoforge-1.21.1:<revision>`. JitPack builds `forge-1.20.1`, `fabric-1.20.1`
and `fabric-1.21.1` from the same revision too.

This revision is a build/test API baseline, not a runtime fork requirement. Integration checks
for `modId=recruits`; Recruits is not included in Siegeworks jars. Integration GameTests use the `siegeworks`
and `siegeworks_ladder` namespaces and need `-Penable_recruits_compat_runtime=true`.

## Mechanics, configuration and data

- Engine and projectile combat values belong in data definitions.
- Server pacing and policy belong in the server config.
- Loading order, controls, aiming constraints and animation events remain code when they define the mechanic itself.

Do not duplicate a value in code when it already comes from a data definition or server config.

## Verification

Collision, terrain movement, deployment, ladders, siege-tower ramps and projectile behavior have deterministic tests or GameTests. Changes in those areas should update the relevant coverage rather than rely only on a manual client check.

To exercise ladder traffic with real Recruits, including three separate groups of twenty walking up and down:

```sh
./gradlew :forge:1.20.1:runGameTestServer :neoforge:1.21.1:runGameTestServer -PgameTestNamespaces=siegeworks_ladder,siegeworks_ladder_regression,siegeworks_collision -Penable_recruits_compat_runtime=true
```

These tests check health throughout the passage, spacing, actual supported landings, blocked entrances and exits, and cancellation of transient movement control. Keep the ordinary AI enabled in the movement scenarios; making the actors invulnerable or replacing their walk with teleports defeats the checks.

The regression namespace also exercises owned troops following their commander, formation orders, repeated point orders,
and a fifteen-metre ladder leading to a thin suspended platform. It checks ownership of the distant approach, unexpected
route loss and prolonged stalls, rather than only eventual arrival. For a live reproduction, use
`-Pdebug.recruits=true` with `runClient`, or `/siegeworks debug recruits true` in the world. Ladder traces are opt-in and
limited to one report per troop per second, with route phase, progress, blockage, destination and navigation ownership.

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
./gradlew :fabric:1.20.1:runGameTest :fabric:1.21.1:runGameTest -Penable_recruits_compat_runtime=false -Paxiomata.localDir=/absolute/path/axiomata/build/libs/0.1.0-beta.9
```

The Fabric targets require Axiomata's Fabric builds. Until those artifacts are published, supply all four
matching Axiomata jars through the local override above.

## Assets

Third-party assets need a traceable source and license entry in `THIRD_PARTY_NOTICES.md`.

Engine models live in `src/main/blockbench/models`. After changing one in Blockbench, export the geo file with
GeckoLib and the construction markup with Axiomata's Build Sections plugin (**File → Export → Construction Markup**)
to `src/main/resources/data/siegeworks/construction/<model>.json`.
