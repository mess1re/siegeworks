import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    `java-library`
    `maven-publish`
    id("net.neoforged.moddev") version "2.0.140"
}

val modId = project.property("mod.id") as String
val modName = project.property("mod.name") as String
val modVersion = project.property("mod.version") as String
val modGroup = project.property("mod.group") as String
val modDescription = project.property("mod.description") as String
val modAuthors = project.property("mod.authors") as String
val modLicense = project.property("mod.license") as String
val minecraftRange = project.property("mod.minecraft_range") as String
val loaderRange = project.property("mod.loader_range") as String
val packFormat = project.property("mod.pack_format") as String
val neoForgeVersion = project.property("deps.neoforge") as String
val geckolibVersion = project.property("deps.geckolib") as String
val axiomataVersion = providers.gradleProperty("axiomataVersion")
    .getOrElse(project.property("deps.axiomata") as String)
val axiomataModVersion = project.property("deps.axiomata_mod") as String
val architecturyVersion = project.property("deps.architectury") as String
val recruitsVersion = project.property("deps.recruits") as String
val recruitsRange = project.property("deps.recruits_range") as String

group = modGroup
version = "$modVersion+${sc.current.version}"

base {
    archivesName.set("$modId-neoforge")
}

val gameTestSourceSet = sourceSets.create("gametest")
if (providers.gradleProperty("enable_recruits_compat_runtime").orNull?.toBoolean() != true) {
    gameTestSourceSet.java.exclude("me/mss1r/siegeworks/gametest/RecruitsCrewAndFireGameTests.java")
}
val gameTestDirectory = rootProject.file("run/gametest/${sc.current.version}-neoforge")

configurations.named(gameTestSourceSet.implementationConfigurationName) {
    extendsFrom(configurations.implementation.get())
}
configurations.named(gameTestSourceSet.compileOnlyConfigurationName) {
    extendsFrom(configurations.compileOnly.get())
}
configurations.named(gameTestSourceSet.runtimeOnlyConfigurationName) {
    extendsFrom(configurations.runtimeOnly.get())
}

repositories {
    mavenLocal {
        content {
            includeGroup("com.github.nekomario28.recruits")
        }
    }
    mavenCentral()
    maven("https://jitpack.io") {
        name = "JitPack"
        content { includeGroup("com.github.mess1re.axiomata") }
        metadataSources {
            mavenPom()
            artifact()
            ignoreGradleMetadataRedirection()
        }
    }
    maven("https://maven.architectury.dev/") { name = "Architectury" }
    maven("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/") {
        name = "GeckoLib"
        content { includeGroupByRegex("software\\.bernie.*") }
    }
}

dependencies {
    implementation("com.github.mess1re.axiomata:${sc.current.version}-neoforge:$axiomataVersion")
    implementation("dev.architectury:architectury-neoforge:$architecturyVersion")
    implementation("software.bernie.geckolib:geckolib-neoforge-${sc.current.version}:$geckolibVersion")
    compileOnly("com.github.nekomario28.recruits:1.21.1-neoforge:$recruitsVersion")
    if (providers.gradleProperty("enable_recruits_compat_runtime").orNull?.toBoolean() == true) {
        runtimeOnly("com.github.nekomario28.recruits:1.21.1-neoforge:$recruitsVersion")
    }
    add(gameTestSourceSet.implementationConfigurationName, sourceSets.main.get().output)
}

neoForge {
    version = neoForgeVersion
    addModdingDependenciesTo(gameTestSourceSet)

    runs {
        register("client") {
            client()
            gameDirectory = rootProject.file("run/${sc.current.version}-neoforge")
            systemProperty("siegeworks.debug.instantFire", "true")
            systemProperty("siegeworks.debug.autoDrive", "true")
        }
        register("server") {
            server()
            gameDirectory = rootProject.file("run/${sc.current.version}-neoforge")
            systemProperty("siegeworks.debug.instantFire", "true")
            systemProperty("siegeworks.debug.autoDrive", "true")
            programArgument("--nogui")
        }
        register("gameTestServer") {
            type = "gameTestServer"
            sourceSet = gameTestSourceSet
            gameDirectory = gameTestDirectory
            systemProperty(
                "neoforge.enabledGameTestNamespaces",
                providers.gradleProperty("gameTestNamespaces")
                    .getOrElse("$modId,${modId}_collision,${modId}_tower,${modId}_ladder")
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

sourceSets.main {
    java.exclude(
        // RTS has no 1.21.1 target; Recruits' own command screen remains available without it.
        "me/mss1r/siegeworks/integration/rts/**"
    )
    resources.srcDir(rootProject.file("src/generated/resources"))
}

java {
    withSourcesJar()
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
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
    options.release.set(21)
}

tasks.named<ProcessResources>("processResources") {
    val props = mapOf(
        "minecraft_version_range" to minecraftRange,
        "loader_version_range" to loaderRange,
        "geckolib_version" to geckolibVersion,
        "axiomata_mod_version" to axiomataModVersion,
        "architectury_version" to architecturyVersion,
        "recruits_version_range" to recruitsRange,
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
        "META-INF/neoforge.mods.toml",
        "pack.mcmeta"
    )) { expand(props) }
    filesMatching(listOf("data/*/recipes/*.json", "data/*/recipe/*.json")) {
        var insideResult = false
        filter { line ->
            if (line.contains("\"result\"")) {
                insideResult = true
            }
            if (insideResult && line.contains("\"item\"")) {
                insideResult = false
                line.replace("\"item\"", "\"id\"")
            } else {
                line
            }
        }
    }
    exclude("META-INF/mods.toml")
    includeEmptyDirs = false
    eachFile {
        path = path
            .replace("/recipes/", "/recipe/")
            .replace("/loot_tables/", "/loot_table/")
            .replace("/structures/", "/structure/")
            .replace("/tags/blocks/", "/tags/block/")
            .replace("/tags/items/", "/tags/item/")
            .replace("/tags/entity_types/", "/tags/entity_type/")
    }
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
            "MixinConfigs" to "$modId.mixins.json"
        )
    }
}

tasks.register<Copy>("buildAndCollect") {
    dependsOn("build")
    from(tasks.named<Jar>("jar"), tasks.named<Jar>("sourcesJar"))
    into(rootProject.layout.buildDirectory.dir("libs/$modVersion"))
}
