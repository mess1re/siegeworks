import org.gradle.api.tasks.SourceSetContainer
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


val minecraftRange = project.property("mod.minecraft_range") as String
val forgeRange = project.property("mod.forge_range") as String
val loaderRange = project.property("mod.loader_range") as String
val packFormat = project.property("mod.pack_format") as String
val architecturyVersion = project.property("deps.architectury") as String
val mcVersion = sc.current.version

val commonPath = ":$mcVersion"
evaluationDependsOn(commonPath)
val commonProject = project(commonPath)

group = modGroup
version = "$modVersion+$mcVersion"

base {
    archivesName.set("$modId-forge")
}

architectury {
    platformSetupLoomIde()
    forge {
        // The common jar belongs to the same mod in development; a fake second
        // mod would make Java reject packages shared with native API facades.
        val commonJar = commonProject.tasks.named<Jar>("jar").get().archiveFile.get().asFile.toPath()
        transformers.removeIf { it.apply(commonJar).clazz.simpleName == "GenerateFakeForgeMod" }
    }
}

loom {
    silentMojangMappingsLicense()


    // Remap mixin targets directly in the jar, so neither common nor forge mixins need a refmap.
    mixin {
        useLegacyMixinAp.set(false)
    }

    forge {
        mixinConfig("$modId.mixins.json", "$modId.recruits.mixins.json")
    }

    runs {
        named("client") {
            client()
            runDir(rootProject.file("run/$mcVersion-forge").relativeTo(projectDir).path)
        }
        named("server") {
            server()
            runDir(rootProject.file("run/$mcVersion-forge").relativeTo(projectDir).path)
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
    named("developmentForge").get().extendsFrom(common)
}

repositories {
    maven("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/") { content { includeGroupByRegex("software\\.bernie.*"); includeGroup("com.eliotlash.mclib") } }
    mavenCentral()
    maven("https://api.modrinth.com/maven") { content { includeGroup("maven.modrinth") } }
    maven("https://jitpack.io") {
        content { includeGroupByRegex("com\\.github\\.mess1re.*") }
        metadataSources { mavenPom(); artifact(); ignoreGradleMetadataRedirection() }
    }
    maven("https://maven.architectury.dev/") { name = "Architectury" }
    maven("https://maven.fabricmc.net/") { name = "Fabric" }
    maven("https://maven.minecraftforge.net/") { name = "Forge" }
    maven("https://www.cursemaven.com") {
        name = "CurseMaven"
        content { includeGroup("curse.maven") }
    }
}

dependencies {
    implementation("com.eliotlash.mclib:mclib:20")
    "forgeRuntimeLibrary"("com.eliotlash.mclib:mclib:20")
    testImplementation(project(path = commonPath, configuration = "namedElements")) { isTransitive = false }
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    minecraft("com.mojang:minecraft:$mcVersion")
    mappings(loom.officialMojangMappings())
    forge("net.minecraftforge:forge:${project.property("deps.forge")}")

    modImplementation("software.bernie.geckolib:geckolib-forge-$mcVersion:${project.property("deps.geckolib")}") { isTransitive = false }
    modImplementation("dev.architectury:architectury-forge:$architecturyVersion")

    val localAxiomata = providers.gradleProperty("axiomata.localDir").orNull
    if (localAxiomata != null) {
        val jar = file(localAxiomata).listFiles()?.singleOrNull {
            it.name.startsWith("axiomata-forge-") && it.name.endsWith("+$mcVersion.jar")
        }
        require(jar != null) { "No Axiomata forge jar for $mcVersion in $localAxiomata" }
        modImplementation(files(jar))
    } else {
        modImplementation("com.github.mess1re.axiomata:$mcVersion-forge:${project.property("deps.axiomata")}")
    }
    modCompileOnly("maven.modrinth:villager-recruits:${project.property("deps.recruits")}")
    if (providers.gradleProperty("enable_recruits_compat_runtime").orNull?.toBoolean() == true) {
        modRuntimeOnly("maven.modrinth:villager-recruits:${project.property("deps.recruits")}")
    }
    modCompileOnly("com.github.mess1re:recruitsrtscommand:${project.property("deps.rts_command")}")
    if (providers.gradleProperty("enable_rts_compat_runtime").orNull?.toBoolean() == true) {
        modRuntimeOnly("com.github.mess1re:recruitsrtscommand:${project.property("deps.rts_command")}")
    }
    common(project(path = commonPath, configuration = "namedElements")) { isTransitive = false }
    shadowBundle(project(path = commonPath, configuration = "transformProductionForge"))

}

java {
    withSourcesJar()
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(17)
}

tasks.named<ProcessResources>("processResources") {
    val props = mapOf(
        "axiomata_version" to project.property("deps.axiomata_mod"),
        "geckolib_version" to project.property("deps.geckolib"),
        "recruits_range" to project.property("deps.recruits_range"),
        "rts_command_range" to project.property("deps.rts_command_range"),
        "mod_id" to modId,
        "mod_name" to modName,
        "mod_version" to modVersion,
        "mod_description" to modDescription,
        "mod_authors" to modAuthors,

        "mod_license" to modLicense,


        "minecraft_range" to minecraftRange,
        "forge_range" to forgeRange,
        "loader_range" to loaderRange,
        "architectury_version" to architecturyVersion,
        "pack_format" to packFormat,
        "java_compatibility" to "JAVA_17"
    )
    inputs.properties(props)
    filesMatching(listOf("META-INF/mods.toml", "pack.mcmeta", "siegeworks.recruits.mixins.json")) {
        expand(props)
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

tasks.withType<Jar>().configureEach {
    manifest {
        attributes(
            "MixinConfigs" to "$modId.mixins.json,$modId.recruits.mixins.json",
            "Specification-Title" to modId,
            "Specification-Vendor" to modAuthors,
            "Specification-Version" to "1",
            "Implementation-Title" to modName,
            "Implementation-Version" to modVersion,
            "Implementation-Vendor" to modAuthors
        )
    }
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
            artifactId = if (jitpackBuild) "$mcVersion-forge" else System.getenv("ARTIFACT") ?: modId
            version = if (jitpackBuild) project.version.toString() else System.getenv("VERSION") ?: project.version.toString()
            pom.withXml {
                val dep = asNode().appendNode("dependencies").appendNode("dependency")
                dep.appendNode("groupId", "dev.architectury")
                dep.appendNode("artifactId", "architectury-forge")
                dep.appendNode("version", architecturyVersion)
                dep.appendNode("scope", "runtime")
            }
            artifact(tasks.named("remapJar"))
            artifact(tasks.named("remapSourcesJar")) { classifier = "sources" }
        }
    }
}

// Consumer source jars include the API, not just the loader's entry point.
tasks.named<Jar>("sourcesJar") {
    from(commonProject.sourceSets.main.get().allSource)
}

tasks.withType<Test>().configureEach { useJUnitPlatform() }

tasks.withType<Jar>().configureEach {
    if (!name.startsWith("remap")) {
        from(rootProject.file("LICENSE")) { into("META-INF") }
        from(rootProject.file("LICENSE-ASSETS.md")) { into("META-INF") }
    }
}

// Common APIs and their native compatibility facades share packages. Group them
// as one development mod, just as they are packaged into one production jar.
loom.mods {
    maybeCreate("main").apply {
        sourceSet(sourceSets.main.get())
        configuration(common)
    }
}

val sets = extensions.getByType<SourceSetContainer>()
val testLoom = extensions.getByType<net.fabricmc.loom.api.LoomGradleExtensionAPI>()
val loader = project.path.split(':')[1]
val minecraft = project.name
val commonSets = commonProject.extensions.getByType<SourceSetContainer>()
val gametest = sets.create("gametest")
if (providers.gradleProperty("enable_recruits_compat_runtime").orNull?.toBoolean() != true) {
    gametest.java.exclude("me/mss1r/siegeworks/gametest/RecruitsCrewAndFireGameTests.java")
    gametest.java.exclude("me/mss1r/siegeworks/gametest/RecruitsLadderTrafficGameTests.java")
}
gametest.java.setSrcDirs(commonSets.getByName("gametest").java.srcDirs)
gametest.resources.setSrcDirs(commonSets.getByName("gametest").resources.srcDirs)
gametest.compileClasspath += sets.getByName("main").compileClasspath + sets.getByName("main").output
gametest.runtimeClasspath += sets.getByName("main").runtimeClasspath + sets.getByName("main").output
testLoom.createRemapConfigurations(gametest)
testLoom.mods.maybeCreate("main").sourceSet(gametest)
val gameDirectory = rootProject.file("run/gametest/$minecraft-$loader")
testLoom.runConfigs.register("gameTestServer") {
    server()
    forgeTemplate("gameTestServer")
    source(gametest)
    runDir(gameDirectory.relativeTo(projectDir).path)
    property("$loader.enableGameTest", "true")
    property("$loader.gameTestServer", "true")
    property("$loader.enabledGameTestNamespaces", providers.gradleProperty("gameTestNamespaces")
        .getOrElse("siegeworks_aim_regression,siegeworks_collision,siegeworks_banners,siegeworks_draft,siegeworks_gravity,siegeworks_pole_regression,siegeworks_stack_regression"))
}
testLoom.runConfigs.named("client") {
    property("siegeworks.debug.instantFire", "true")
    property("siegeworks.debug.autoDrive", "true")
    property("siegeworks.debug.recruits", providers.gradleProperty("debug.recruits").getOrElse("false"))
    providers.gradleProperty("test.aimSceneWorld").orNull?.let { world ->
        source(gametest)
        runDir(rootProject.file("build/aim-client-$loader").relativeTo(projectDir).path)
        programArgs("--quickPlaySingleplayer", world)
        property("siegeworks.test.aimScene", "true")
        property("siegeworks.test.aimSceneEngine", providers.gradleProperty("test.aimSceneEngine").getOrElse("tower_crossbow"))
        property("siegeworks.test.aimSceneFloor", providers.gradleProperty("test.aimSceneFloor").getOrElse("full"))
    }
    providers.gradleProperty("test.bannerSceneWorld").orNull?.let { world ->
        source(gametest)
        runDir(rootProject.file("build/banner-client-$loader").relativeTo(projectDir).path)
        programArgs("--quickPlaySingleplayer", world)
        property("siegeworks.test.bannerScene", "true")
    }
}
val resetGameTestWorld = tasks.register<Delete>("resetGameTestWorld") {
    delete(gameDirectory.resolve("world"))
}
val prepareStructures = tasks.register<Copy>("prepareGameTestStructures") {
    dependsOn(resetGameTestWorld)
    from(rootProject.file("src/gametest/resources/gameteststructures"))
    into(gameDirectory.resolve("gameteststructures"))
    doLast { gameDirectory.resolve("eula.txt").writeText("eula=true\n") }
}
tasks.named("compileGametestJava") { dependsOn(commonProject.tasks.named("stonecutterGenerateGametest")) }
tasks.named("processGametestResources") { dependsOn(commonProject.tasks.named("stonecutterGenerateGametest")) }
tasks.matching { it.name == "runGameTestServer" }.configureEach { dependsOn(prepareStructures) }


apply(from = rootProject.file("gradle/client-probes.gradle.kts"))
