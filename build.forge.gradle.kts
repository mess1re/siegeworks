import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    `java-library`
    `maven-publish`
    id("net.neoforged.moddev.legacyforge") version "2.0.140"
}

val modId = project.property("mod.id") as String
val modName = project.property("mod.name") as String
val modVersion = project.property("mod.version") as String
val modGroup = project.property("mod.group") as String
val modDescription = project.property("mod.description") as String
val modAuthors = project.property("mod.authors") as String
val modLicense = project.property("mod.license") as String
val minecraftVersion = sc.current.version
val minecraftRange = project.property("mod.minecraft_range") as String
val forgeRange = project.property("mod.forge_range") as String
val loaderRange = project.property("mod.loader_range") as String
val packFormat = project.property("mod.pack_format") as String
val forgeVersion = project.property("deps.forge") as String
val geckolibVersion = project.property("deps.geckolib") as String
val axiomataVersion = providers.gradleProperty("axiomataVersion")
    .getOrElse(project.property("deps.axiomata") as String)
val axiomataModVersion = project.property("deps.axiomata_mod") as String
val architecturyVersion = project.property("deps.architectury") as String
val recruitsVersion = project.property("deps.recruits") as String
val recruitsRange = project.property("deps.recruits_range") as String
val rtsCommandVersion = project.property("deps.rts_command") as String
val rtsCommandRange = project.property("deps.rts_command_range") as String

group = modGroup
version = "$modVersion+${sc.current.version}"

base {
    archivesName.set("$modId-forge")
}

val gameTestSourceSet = sourceSets.create("gametest")
if (providers.gradleProperty("enable_recruits_compat_runtime").orNull?.toBoolean() != true) {
    gameTestSourceSet.java.exclude("me/mss1r/siegeworks/gametest/RecruitsCrewAndFireGameTests.java")
}
val gameTestDirectory = rootProject.file("run/gametest/${sc.current.version}-forge")

configurations.named(gameTestSourceSet.implementationConfigurationName) {
    extendsFrom(configurations.implementation.get())
}
configurations.named(gameTestSourceSet.runtimeOnlyConfigurationName) {
    extendsFrom(configurations.runtimeOnly.get())
}

repositories {
    mavenCentral()
    maven("https://jitpack.io") {
        name = "JitPack"
        content {
            includeGroup("com.github.mess1re")
            includeGroup("com.github.mess1re.axiomata")
        }
        metadataSources {
            mavenPom()
            artifact()
            ignoreGradleMetadataRedirection()
        }
    }
    maven("https://maven.architectury.dev/") { name = "Architectury" }
    maven("https://api.modrinth.com/maven") {
        name = "Modrinth"
        content { includeGroup("maven.modrinth") }
    }
    maven("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/") {
        name = "GeckoLib"
        content { includeGroupByRegex("software\\.bernie.*") }
    }
    maven("https://repo.spongepowered.org/repository/maven-public/") { name = "Sponge" }
}

dependencies {
    "modImplementation"("com.github.mess1re.axiomata:${sc.current.version}-forge:$axiomataVersion")
    "modImplementation"("dev.architectury:architectury-forge:$architecturyVersion")
    "modImplementation"("software.bernie.geckolib:geckolib-forge-$minecraftVersion:$geckolibVersion")
    "modCompileOnly"("maven.modrinth:villager-recruits:$recruitsVersion")
    // The RTS command layer, if the player has it. Compile only and optional in both directions:
    // this mod commands siege engines from Recruits' own screen without it, and it works with
    // Recruits without this mod. Neither is allowed to require the other.
    "modCompileOnly"("com.github.mess1re:recruitsrtscommand:$rtsCommandVersion")
    if (providers.gradleProperty("enable_recruits_compat_runtime").orNull?.toBoolean() == true) {
        "modRuntimeOnly"("maven.modrinth:villager-recruits:$recruitsVersion")
    }
    // The RTS map in the development run, so the map integration can actually be looked at. Its
    // own development client cannot host this mod: it runs Minecraft under Mojang's names and a
    // built Forge jar carries the obfuscated ones, so nothing of ours loads there. This way round
    // works because the map's mixins are @Pseudo with remap = false and touch no Minecraft member,
    // so they do not care which names the game is wearing.
    if (providers.gradleProperty("enable_rts_compat_runtime").orNull?.toBoolean() == true) {
        "modRuntimeOnly"("com.github.mess1re:recruitsrtscommand:$rtsCommandVersion")
    }
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
    add(gameTestSourceSet.implementationConfigurationName, sourceSets.main.get().output)
}

legacyForge {
    version = forgeVersion
    validateAccessTransformers = true
    addModdingDependenciesTo(gameTestSourceSet)
    gameTestSourceSet.compileClasspath += sourceSets.main.get().compileClasspath

    runs {
        configureEach {
            gameDirectory = rootProject.file("run/${sc.current.version}-forge")
            systemProperty("forge.logging.markers", "REGISTRIES")
            systemProperty("forge.logging.console.level", "info")
            // Takes structures out of mob pathfinding, for telling "the machine is in his way"
            // apart from whatever else a mob may be doing. Run with -PstructurePathfinding=off.
            systemProperty(
                "axiomata.structurePathfinding",
                providers.gradleProperty("structurePathfinding").getOrElse("on")
            )
            // Reports which controller took each recruit's tick. Run with -PrecruitsDebug=true.
            systemProperty(
                "siegeworks.debug.recruits",
                providers.gradleProperty("recruitsDebug").getOrElse("false")
            )
        }
        register("client") {
            client()
            systemProperty("siegeworks.debug.instantFire", "true")
            systemProperty("siegeworks.debug.autoDrive", "true")
        }
        register("server") {
            server()
            systemProperty("siegeworks.debug.instantFire", "true")
            systemProperty("siegeworks.debug.autoDrive", "true")
            programArgument("--nogui")
        }
        register("gameTestServer") {
            type = "gameTestServer"
            sourceSet = gameTestSourceSet
            gameDirectory = gameTestDirectory
            systemProperty(
                "forge.enabledGameTestNamespaces",
                providers.gradleProperty("gameTestNamespaces")
                    .getOrElse("$modId,${modId}_collision,${modId}_tower,${modId}_ladder")
            )
        }
        register("data") {
            data()
            gameDirectory = rootProject.file("run-data")
            programArguments.addAll(
                "--mod", modId,
                "--all",
                "--output", rootProject.file("src/generated/resources").absolutePath,
                "--existing", rootProject.file("src/main/resources").absolutePath
            )
        }
    }

    mods {
        register(modId) {
            sourceSet(sourceSets.main.get())
            sourceSet(gameTestSourceSet)
        }
    }
}

mixin {
    add(sourceSets.main.get(), "$modId.refmap.json")
    config("$modId.mixins.json")
    config("$modId.recruits.mixins.json")
}

sourceSets.main {
    resources.srcDir(rootProject.file("src/generated/resources"))
}

java {
    withSourcesJar()
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = System.getenv("GROUP") ?: project.group.toString()
            artifactId = System.getenv("ARTIFACT") ?: modId
            version = System.getenv("VERSION") ?: project.version.toString()
            from(components["java"])
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(17)
}

tasks.named<ProcessResources>("processResources") {
    val props = mapOf(
        "minecraft_version_range" to minecraftRange,
        "forge_version_range" to forgeRange,
        "loader_version_range" to loaderRange,
        "geckolib_version" to geckolibVersion,
        "axiomata_version" to axiomataVersion,
        "axiomata_mod_version" to axiomataModVersion,
        "architectury_version" to architecturyVersion,
        "recruits_version_range" to recruitsRange,
        "rts_command_version_range" to rtsCommandRange,
        "mod_id" to modId,
        "mod_name" to modName,
        "mod_license" to modLicense,
        "mod_version" to modVersion,
        "mod_authors" to modAuthors,
        "mod_description" to modDescription,
        "pack_format" to packFormat
    )
    inputs.properties(props)
    filesMatching(listOf(
        "META-INF/mods.toml",
        "pack.mcmeta"
    )) { expand(props) }
    exclude("META-INF/neoforge.mods.toml")
}

// Every language file is checked on both targets, not only the one that happens to be built.
apply(from = rootProject.file("gradle/translation-keys.gradle.kts"))

tasks.named("createMinecraftArtifacts") {
    dependsOn("stonecutterGenerate")
}

val resetGameTestWorld = tasks.register<Delete>("resetGameTestWorld") {
    delete(gameTestDirectory.resolve("world"))
}

val prepareGameTestStructures = tasks.register<Copy>("prepareGameTestStructures") {
    dependsOn(resetGameTestWorld)
    from(rootProject.file("src/gametest/resources/gameteststructures"))
    into(gameTestDirectory.resolve("gameteststructures"))
}

tasks.named("runGameTestServer") {
    dependsOn(prepareGameTestStructures)
}

tasks.withType<Jar>().configureEach {
    includeEmptyDirs = false
    from(rootProject.file("LICENSE")) {
        into("META-INF")
        rename { "LICENSE_siegeworks" }
    }
    from(rootProject.file("LICENSE-ASSETS.md")) { into("META-INF") }
    from(rootProject.file("THIRD_PARTY_NOTICES.md")) { into("META-INF") }
    manifest {
        attributes(
            "Specification-Title" to modId,
            "Specification-Vendor" to modAuthors,
            "Specification-Version" to "1",
            "Implementation-Title" to modName,
            "Implementation-Version" to archiveVersion.get(),
            "Implementation-Vendor" to modAuthors,
            "MixinConfigs" to "$modId.mixins.json,$modId.recruits.mixins.json"
        )
    }
}

tasks.register<Copy>("buildAndCollect") {
    dependsOn("build")
    from(tasks.named("reobfJar"), tasks.named<Jar>("sourcesJar"))
    into(rootProject.layout.buildDirectory.dir("libs/$modVersion"))
}
