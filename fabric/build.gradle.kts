import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    `maven-publish`
    id("dev.architectury.loom")
    id("architectury-plugin")
    id("com.gradleup.shadow")
}

val modId = project.property("mod.id") as String
val modName = project.property("mod.name") as String
val modVersion = project.property("mod.version") as String
val modGroup = project.property("mod.group") as String
val modDescription = project.property("mod.description") as String
val modAuthors = project.property("mod.authors") as String
val modLicense = project.property("mod.license") as String
val modDisplayUrl = "https://github.com/mess1re/siegeworks"
val minecraftRange = project.property("mod.minecraft_range") as String
val fabricLoaderVersion = project.property("deps.fabric_loader") as String
val fabricApiVersion = project.property("deps.fabric_api") as String
val architecturyVersion = project.property("deps.architectury") as String
val javaVersion = project.property("mod.java").toString().toInt()
val mcVersion = sc.current.version

val commonPath = ":$mcVersion"
evaluationDependsOn(commonPath)
val commonProject = project(commonPath)

group = modGroup
version = "$modVersion+$mcVersion"

base {
    archivesName.set("$modId-fabric")
}

architectury {
    platformSetupLoomIde()
    fabric()
}

loom {
    silentMojangMappingsLicense()

    // Remap mixin targets directly in the jar, so neither common nor fabric mixins need a refmap.
    mixin {
        useLegacyMixinAp.set(false)
    }

    // Lets Fabric Loader treat the common output as part of this mod in development, e.g. to find the access widener.
    mods {
        maybeCreate("main").apply {
            sourceSet(sourceSets.main.get())
            sourceSet(commonProject.sourceSets.main.get(), commonProject)
        }
    }

    runs {
        named("client") {
            client()
            providers.gradleProperty("client.server").orNull?.let { programArgs("--quickPlayMultiplayer", it) }
            providers.gradleProperty("client.username").orNull?.let { programArgs("--username", it) }
            runDir(rootProject.file("run/$mcVersion-fabric").relativeTo(projectDir).path)
        }
        named("server") {
            server()
            runDir(rootProject.file("run/$mcVersion-fabric").relativeTo(projectDir).path)
            programArg("--nogui")
        }
    }
}

val common: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}
val shadowBundle: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}
configurations {
    compileClasspath.get().extendsFrom(common)
    runtimeClasspath.get().extendsFrom(common)
    named("developmentFabric").get().extendsFrom(common)
}

repositories {
    maven("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/") { content { includeGroupByRegex("software\\.bernie.*"); includeGroup("com.eliotlash.mclib") } }
    maven("https://maven.nucleoid.xyz/") { content { includeGroup("eu.pb4") } }
    mavenCentral()
    maven("https://maven.jamieswhiteshirt.com/libs-release") { content { includeGroup("com.jamieswhiteshirt") } }
    exclusiveContent {
        forRepository {
            maven("https://jitpack.io") {
                metadataSources { mavenPom(); artifact(); ignoreGradleMetadataRedirection() }
            }
        }
        filter { includeGroupByRegex("com\\.github\\.mess1re.*") }
    }
    maven("https://maven.architectury.dev/") { name = "Architectury" }
    maven("https://maven.fabricmc.net/") { name = "Fabric" }
    maven("https://raw.githubusercontent.com/Fuzss/modresources/main/maven/") {
        name = "Fuzs"
        content { includeGroup("fuzs.forgeconfigapiport") }
    }
}

dependencies {
    providers.gradleProperty("runtime.localJars").orNull?.let { directory ->
        modRuntimeOnly(fileTree(directory) { include("*.jar") })
    }
    if (mcVersion == "1.20.1") implementation("com.eliotlash.mclib:mclib:20")
    if (mcVersion == "1.20.1") {
        modImplementation("com.jamieswhiteshirt:reach-entity-attributes:2.4.0")
        include("com.jamieswhiteshirt:reach-entity-attributes:2.4.0")
    }
    minecraft("com.mojang:minecraft:$mcVersion")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")

    modImplementation("dev.architectury:architectury-fabric:$architecturyVersion")
    modImplementation(project.property("deps.forge_config_api_port") as String)
    modImplementation("software.bernie.geckolib:geckolib-fabric-$mcVersion:${project.property("deps.geckolib")}") { isTransitive = false }
    // GeckoLib's API uses javax.annotation; the source remapper must resolve those types too.
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")

    val localAxiomata = providers.gradleProperty("axiomata.localDir").orNull
    if (localAxiomata != null) {
        val jar = file(localAxiomata).listFiles()?.singleOrNull {
            it.name.startsWith("axiomata-fabric-") && it.name.endsWith("+$mcVersion.jar")
        }
        require(jar != null) { "No Axiomata fabric jar for $mcVersion in $localAxiomata" }
        modImplementation(files(jar))
        // Loom strips nested jars from a local remapped dependency in development.
        modRuntimeOnly("eu.pb4:common-protection-api:1.0.0") { isTransitive = false }
    } else {
        modImplementation("com.github.mess1re.axiomata:$mcVersion-fabric:${project.property("deps.axiomata")}")
    }
    modCompileOnly("com.github.mess1re.recruits:fabric-$mcVersion:${project.property("deps.recruits_port")}")
    if (providers.gradleProperty("enable_recruits_compat_runtime").orNull?.toBoolean() == true) {
        modRuntimeOnly("com.github.mess1re.recruits:fabric-$mcVersion:${project.property("deps.recruits_port")}")
    }
    common(project(path = commonPath, configuration = "namedElements")) { isTransitive = false }
    shadowBundle(project(path = commonPath, configuration = "transformProductionFabric"))
}

java {
    withSourcesJar()
    toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(javaVersion)
}

tasks.named<ProcessResources>("processResources") {
    val props = mapOf(
        "mod_id" to modId,
        "mod_name" to modName,
        "mod_version" to modVersion,
        "mod_description" to modDescription,
        "mod_authors" to modAuthors,
        "mod_license" to modLicense,
        "mod_display_url" to modDisplayUrl,
        "minecraft_range" to minecraftRange,
        "fabric_loader_version" to fabricLoaderVersion,
        "fabric_api_version" to fabricApiVersion.substringBefore('+'),
        "architectury_version" to architecturyVersion,
        "axiomata_version" to project.property("deps.axiomata_mod"),
        "geckolib_version" to project.property("deps.geckolib"),
        "config_api_version" to project.property("deps.forge_config_api_port").toString().substringAfterLast(':'),
        "java_version" to javaVersion,
        "java_compatibility" to "JAVA_$javaVersion"
    )
    inputs.properties(props)
    filesMatching(listOf("fabric.mod.json", "siegeworks.fabric.mixins.json")) {
        expand(props)
        if (name == "siegeworks.fabric.mixins.json" && mcVersion != "1.20.1") {
            filter { line -> if (line.contains("\"ExplosionMixin\"")) "" else line }
        }
    }
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    configurations = listOf(shadowBundle)
    archiveClassifier.set("dev-shadow")
    exclude("architectury.common.json")
}

tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar") {
    inputFile.set(tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar").flatMap { it.archiveFile })
    archiveClassifier.set(null as String?)
}

tasks.named<JavaExec>("runServer") {
    standardInput = System.`in`
}

tasks.register<Copy>("buildAndCollect") {
    dependsOn("build")
    from(tasks.named("remapJar"), tasks.named("remapSourcesJar"))
    into(rootProject.layout.buildDirectory.dir("libs/$modVersion"))
}

// One artifact per loader and Minecraft version, so the Fabric and NeoForge 1.21.1 builds do not share a name.
publishing {
    publications {
        create<MavenPublication>("mod") {
            val jitpackBuild = System.getenv("JITPACK") == "true"
            groupId = if (jitpackBuild) project.group.toString() else System.getenv("GROUP") ?: project.group.toString()
            artifactId = if (jitpackBuild) "$mcVersion-fabric" else System.getenv("ARTIFACT") ?: "$modId-fabric-$mcVersion"
            version = if (jitpackBuild) project.version.toString() else System.getenv("VERSION") ?: project.version.toString()
            pom.withXml {
                val dependencies = asNode().appendNode("dependencies")
                for (coordinate in listOf(
                    "dev.architectury:architectury-fabric:$architecturyVersion",
                    "net.fabricmc.fabric-api:fabric-api:$fabricApiVersion",
                    project.property("deps.forge_config_api_port").toString()
                )) {
                    val parts = coordinate.split(':')
                    val dependency = dependencies.appendNode("dependency")
                    dependency.appendNode("groupId", parts[0])
                    dependency.appendNode("artifactId", parts[1])
                    dependency.appendNode("version", parts[2])
                    dependency.appendNode("scope", "runtime")
                }
            }
            artifact(tasks.named("remapJar"))
            artifact(tasks.named("remapSourcesJar")) { classifier = "sources" }
        }
    }
}

tasks.named<Jar>("sourcesJar") { from(commonProject.sourceSets.main.get().allSource) }
tasks.withType<Jar>().configureEach {
    if (!name.startsWith("remap")) {
        if (mcVersion == "1.20.1") {
            from(rootProject.file("licenses/reach-entity-attributes-MIT.txt")) { into("META-INF/licenses") }
        }
        from(rootProject.file("LICENSE")) { into("META-INF") }
        from(rootProject.file("LICENSE-ASSETS.md")) { into("META-INF") }
    }
}


// Runtime integration tests are isolated from the distributed mod.
val testmod = sourceSets.create("testmod")
configurations.named(testmod.implementationConfigurationName) { extendsFrom(configurations.implementation.get()) }
configurations.named(testmod.runtimeOnlyConfigurationName) { extendsFrom(configurations.runtimeOnly.get()) }
testmod.compileClasspath += sourceSets.main.get().compileClasspath + sourceSets.main.get().output
testmod.runtimeClasspath += sourceSets.main.get().runtimeClasspath + sourceSets.main.get().output
loom.createRemapConfigurations(testmod)
dependencies {
    add("modTestmodImplementation", "net.fabricmc.fabric-api:fabric-gametest-api-v1:${if (mcVersion == "1.20.1") "1.2.15+1802ada577" else "2.0.4+6fc22b9919"}")
}
loom.mods {
    maybeCreate("siegeworks_fabric_tests").sourceSet(testmod)
}
val gameTestDirectory = rootProject.file("run/$mcVersion-fabric-gametest")
extra["gameTestDirectory"] = gameTestDirectory

loom.runs {
    register("gameTest") {
        property("siegeworks.test.filter", providers.gradleProperty("gameTestFilter").getOrElse(""))
        server()
        source(testmod)
        runDir(gameTestDirectory.relativeTo(projectDir).path)
        vmArg("-Dfabric-api.gametest")
        property("siegeworks.test.namespaces", providers.gradleProperty("gameTestNamespaces").getOrElse(
            "siegeworks_aim_regression,siegeworks_collision,siegeworks_banners,siegeworks_draft,siegeworks_gravity,siegeworks_pole_regression,siegeworks_stack_regression"))
        vmArg("-Dfabric-api.gametest.report-file=${layout.buildDirectory.file("test-results/fabric-gametest.xml").get().asFile}")
    }
}
tasks.named<ProcessResources>("processTestmodResources") {
    from(rootProject.file("src/gametest/resources")) { include("data/**") }
    inputs.property("mod_version", modVersion)
    filesMatching("fabric.mod.json") { expand("mod_version" to modVersion) }
}

val resetGameTestWorld = tasks.register<Delete>("resetGameTestWorld") {
    delete(gameTestDirectory.resolve("world"))
}

tasks.withType<JavaExec>().matching { it.name == "runGameTest" }.configureEach {
    dependsOn(resetGameTestWorld)
    doFirst {
        val directory = gameTestDirectory
        directory.mkdirs()
        directory.resolve("eula.txt").writeText("eula=true\n")
        layout.buildDirectory.dir("test-results").get().asFile.mkdirs()
    }
}

val sharedGameTests = commonProject.sourceSets.getByName("gametest").java
val portableScenarios = objects.sourceDirectorySet("portableScenarios", "Shared runtime test scenarios").apply {
    srcDirs(sharedGameTests.srcDirs)
    include("**/ConfigMigrationGameTests.java")
    include("**/AimRegressionGameTests.java")
    include("**/BannerGameTests.java")
    include("**/SiegeAdvancementGameTests.java")
    include("**/BatteringRamCollisionGameTests.java")
    include("**/BoltThrowerCollisionGameTests.java")
    include("**/ConfigUpgradeGameTests.java")
    include("**/DraftTeamGameTests.java")
    include("**/FieldGunCollisionGameTests.java")
    include("**/HwachaCollisionGameTests.java")
    include("**/LadderBraceTargetGameTests.java")
    include("**/MantletCollisionGameTests.java")
    include("**/MonsMegCollisionGameTests.java")
    include("**/OnagerCollisionGameTests.java")
    include("**/SiegeGravityGameTests.java")
    include("**/SiegeLadderCollisionGameTests.java")
    include("**/SiegeLadderQueueGameTests.java")
    include("**/SiegeStackReloadGameTests.java")
    include("**/SiegeTerrainCollisionGameTests.java")
    include("**/SiegeTowerCollisionGameTests.java")
    include("**/StructureCollisionGameTests.java")
    include("**/TrebuchetCollisionGameTests.java")
    include("**/SiegeGameTestPlayers.java")
    include("**/SiegeImpactBalanceGameTests.java")
    include("**/BoltBalanceGameTests.java")
    include("**/ShotFlightGameTests.java")
    include("**/PhysicalImpactGameTests.java")
    include("**/FragmentCalibrationGameTests.java")
    include("**/StructureDamageGameTests.java")
    include("**/ProjectileCalibrationGameTests.java")
    include("**/StoneAmmunitionGameTests.java")
    include("**/CastingGameTests.java")
    include("**/FirearmGameTests.java")
    include("**/ManualRecipeGameTests.java")
    include("**/SharedGameTests.java")
    include("**/RecruitsTestSuites.java")
    include("**/RecruitsFirearmGameTests.java")
    include("**/GameTestQuiet.java")
    include("**/RecruitsCrewAndFireGameTests.java")
    include("**/RecruitsLadderTrafficGameTests.java")
}
testmod.java.source(portableScenarios)

tasks.named("compileTestmodJava") { dependsOn(commonProject.tasks.named("stonecutterGenerateGametest")) }

tasks.named<ProcessResources>("processTestmodResources") {
    if (mcVersion == "1.21.1") eachFile { path = path.replace("/structures/", "/structure/") }
}



// Config migration and optional integrations need the real dedicated-server lifecycle.
val lifecycleDirectory = rootProject.file("build/lifecycle-server/$mcVersion-fabric")
val lifecycleReport = layout.buildDirectory.file("test-results/lifecycle.json")
val globalLifecycleConfigs = providers.gradleProperty("test.globalServerConfigs").getOrElse("true").toBooleanStrict()
val lifecycleConfigs = lifecycleDirectory.resolve(if (globalLifecycleConfigs) "config" else "world/serverconfig")
val resetLifecycleWorld = tasks.register<Delete>("resetLifecycleWorld") {
    delete(lifecycleDirectory)
}
val lifecycleNamespaces = providers.gradleProperty("gameTestNamespaces")
    .getOrElse("siegeworks_config,siegeworks_config_upgrade")
val prepareLifecycleTest = tasks.register("prepareLifecycleTest") {
    dependsOn(resetLifecycleWorld)
    doLast {
        if ("siegeworks_config_upgrade" in lifecycleNamespaces.split(",")) {
            project.copy {
                from(rootProject.file("src/gametest/resources/config-migration"))
                into(lifecycleConfigs)
            }
        }
        lifecycleReport.get().asFile.delete()
        lifecycleDirectory.resolve("config").mkdirs()
        lifecycleDirectory.resolve("config/forgeconfigapiport.toml").writeText("forceGlobalServerConfigs=$globalLifecycleConfigs\n")
        lifecycleDirectory.resolve("eula.txt").writeText("eula=true\n")
        lifecycleDirectory.resolve("server.properties").writeText("""
            online-mode=false
            server-ip=127.0.0.1
            server-port=0
            level-type=minecraft:flat
            generator-settings={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains"}
            view-distance=2
            simulation-distance=2
            max-tick-time=120000
        """.trimIndent())
    }
}
loom.runs.register("lifecycleTest") {
    server()
    source(testmod)
    runDir(lifecycleDirectory.relativeTo(projectDir).path)
    programArgs("nogui")
    property("fabric-api.gametest.command", "true")
    property("siegeworks.test.lifecycle", "true")
    property("siegeworks.test.namespaces", lifecycleNamespaces)
    property("siegeworks.test.report", lifecycleReport.get().asFile.absolutePath)
    property("siegeworks.test.configDirectory", lifecycleConfigs.absolutePath)
}
tasks.named("runLifecycleTest") {
    dependsOn(prepareLifecycleTest)
    doLast {
        val result = lifecycleReport.get().asFile
        check(result.isFile && result.readText().contains("\"failures\":0")) {
            "Dedicated-server lifecycle checks failed; see $lifecycleDirectory/logs/latest.log"
        }
    }
}

val testmodJar = tasks.register<Jar>("testmodJar") {
    archiveClassifier.set("tests-dev")
    from(testmod.output)
}
tasks.register<net.fabricmc.loom.task.RemapJarTask>("remapTestmodJar") {
    dependsOn(testmodJar)
    inputFile.set(testmodJar.flatMap { it.archiveFile })
    archiveClassifier.set("tests")
    addNestedDependencies.set(false)
}
