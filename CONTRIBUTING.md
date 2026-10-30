# Contributing

## Source layout

`src` is the source of truth for every supported target. Use Stonecutter branches only where Minecraft or loader APIs differ; do not edit generated sources under `versions/*/build`.

Forge-only integrations stay out of targets where their dependencies do not exist.

## Optional integrations

Code that references an optional mod belongs in its integration package and must only be initialized after checking that the mod is loaded.

The NeoForge Recruits dependency is the [multiloader port](https://github.com/mess1re/recruits/tree/stonecutter-multiloader)
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

### Working with unpublished Axiomata changes

When SW needs Axiomata changes that are not published yet, build the matching Axiomata checkout
first and point local SW builds at its collected jars. This override is available on both loaders; it neither bundles Axiomata
nor changes the published dependency pin:

```sh
./gradlew :1.21.1-neoforge:buildAndCollect -Paxiomata.localDir=/absolute/path/axiomata/build/libs/0.1.0-beta.9
./gradlew :1.20.1-forge:buildAndCollect -Paxiomata.localDir=/absolute/path/axiomata/build/libs/0.1.0-beta.9
```

Use the same property for `runClient` and `runGameTestServer`.

## Assets

Third-party assets need a traceable source and license entry in `THIRD_PARTY_NOTICES.md`.

Engine models live in `src/main/blockbench/models`. After changing one in Blockbench, export the geo file with
GeckoLib and the construction markup with Axiomata's Build Sections plugin (**File → Export → Construction Markup**)
to `src/main/resources/data/siegeworks/construction/<model>.json`.
