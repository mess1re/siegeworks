plugins {
    id("dev.architectury.loom")
    id("architectury-plugin")
}

val minecraftVersion = stonecutter.current.version
val architecturyVersion = project.property("deps.architectury") as String
val fabricLoaderVersion = project.property("deps.fabric_loader") as String
val javaVersion = project.property("mod.java").toString().toInt()

group = "${project.property("mod.group")}.common"
version = "${project.property("mod.version")}+$minecraftVersion"
base { archivesName.set("${project.property("mod.id")}-common") }

architectury.common(listOf(if (minecraftVersion == "1.20.1") "forge" else "neoforge", "fabric"))

loom {
    silentMojangMappingsLicense()

}

repositories {
    mavenCentral()
    maven("https://maven.nucleoid.xyz/") { content { includeGroup("eu.pb4") } }
    maven("https://maven.jamieswhiteshirt.com/libs-release") { content { includeGroup("com.jamieswhiteshirt") } }
    exclusiveContent {
        forRepository {
            maven("https://jitpack.io") {
                metadataSources { mavenPom(); artifact(); ignoreGradleMetadataRedirection() }
            }
        }
        filter { includeGroupByRegex("com\\.github\\.mess1re.*") }
    }
    maven("https://maven.blamejared.com/")
    maven("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/") { content { includeGroupByRegex("software\\.bernie.*") } }
    maven("https://maven.architectury.dev/")
    maven("https://maven.fabricmc.net/")
    maven("https://raw.githubusercontent.com/Fuzss/modresources/main/maven/") {
        name = "Fuzs"
        content { includeGroup("fuzs.forgeconfigapiport") }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings(loom.officialMojangMappings())
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    modImplementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")
    modImplementation("dev.architectury:architectury:$architecturyVersion")
    modCompileOnly(project.property("deps.forge_config_api_port") as String)
    modCompileOnly("software.bernie.geckolib:geckolib-${if (minecraftVersion == "1.20.1") "fabric" else "common"}-$minecraftVersion:${project.property("deps.geckolib")}") { isTransitive = false }
    val localAxiomata = providers.gradleProperty("axiomata.localDir").orNull
    if (localAxiomata != null) {
        val jar = file(localAxiomata).listFiles()?.singleOrNull {
            it.name.startsWith("axiomata-fabric-") && it.name.endsWith("+$minecraftVersion.jar")
        }
        require(jar != null) { "No Axiomata Fabric API jar for $minecraftVersion in $localAxiomata" }
        modCompileOnly(files(jar))
    } else {
        modCompileOnly("com.github.mess1re.axiomata:$minecraftVersion-fabric:${project.property("deps.axiomata")}")
    }
    modCompileOnly("com.github.mess1re.recruits:fabric-$minecraftVersion:${project.property("deps.recruits_port")}")
    testRuntimeOnly(project.property("deps.forge_config_api_port") as String)
    testRuntimeOnly("com.electronwill.night-config:core:${if (minecraftVersion == "1.20.1") "3.6.5" else "3.8.1"}")
    testRuntimeOnly("com.electronwill.night-config:toml:${if (minecraftVersion == "1.20.1") "3.6.5" else "3.8.1"}")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.mockito:mockito-core:5.15.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    maxHeapSize = "2G"
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
        "pack_format" to project.property("mod.pack_format"),
        "java_compatibility" to "JAVA_$javaVersion",
        "mod_name" to project.property("mod.name"),
        "mod_version" to project.property("mod.version")
    )
    inputs.properties(props)
    filesMatching(listOf("pack.mcmeta", "siegeworks.mixins.json", "siegeworks.recruits.mixins.json")) { expand(props) }
    if (stonecutter.current.parsed >= "1.21") {
        includeEmptyDirs = false
        filesMatching("data/*/advancements/*.json") {
            filter { line -> if (line.contains("\"icon\":")) line.replace("\"item\":", "\"id\":") else line }
        }
        filesMatching(listOf("data/*/recipes/*.json", "data/*/recipe/*.json")) {
            val source = file.readText()
            val stonecutting = source.contains("\"minecraft:stonecutting\"")
            val stonecuttingCount = Regex("\"count\"\\s*:\\s*(\\d+)").find(source)?.groupValues?.get(1) ?: "1"
            var insideResult = false
            filter { line ->
                if (stonecutting && line.contains("\"result\"")) {
                    val id = Regex("\"result\"\\s*:\\s*\"([^\"]+)\"").find(line)?.groupValues?.get(1)
                    if (id != null) "  \"result\": { \"id\": \"$id\", \"count\": $stonecuttingCount }" else line
                } else if (stonecutting && line.contains("\"count\"")) {
                    ""
                } else if (insideResult && line.contains("\"item\"")) {
                    insideResult = false
                    line.replace("\"item\"", "\"id\"")
                } else {
                    if (line.contains("\"result\"")) insideResult = true
                    line
                }
            }
        }
        eachFile {
            path = path
                .replace("/advancements/", "/advancement/")
                .replace("/recipes/", "/recipe/")
                .replace("/loot_tables/", "/loot_table/")
                .replace("/structures/", "/structure/")
                .replace("/tags/blocks/", "/tags/block/")
                .replace("/tags/items/", "/tags/item/")
        }
    }
}

apply(from = rootProject.file("gradle/round-shot-models.gradle.kts"))


if (minecraftVersion == "1.21.1") {
    sourceSets.main { java.exclude("me/mss1r/siegeworks/mixin/RecruitsPathNodeEvaluatorMixin.java") }
}

val commonJava = sourceSets.main.get().allJava
val checkCommonBoundary = tasks.register("checkCommonBoundary") {
    group = "verification"
    description = "Reject loader APIs in the shared module, except portable config specs."
    dependsOn("stonecutterGenerate")
    inputs.files(commonJava)
    doLast {
        val loaderApi = Regex("\\b(net\\.(minecraftforge|neoforged|fabricmc)|dev\\.architectury\\.platform\\.(forge|neoforge|fabric))\\.")
        val configSpec = Regex("\\b(net\\.minecraftforge\\.common\\.ForgeConfigSpec|net\\.neoforged\\.neoforge\\.common\\.ModConfigSpec)\\b")
        val loaderExtensions = Regex("\\b(canReachRaw|onAddedToWorld|onRemovedFromWorld|onAddedToLevel|onRemovedFromLevel)\\b")
        val loaderDescriptors = Regex("net/(minecraftforge|neoforged|fabricmc)/")
        val violations = commonJava.files.filter {
            val source = it.readText().replace(configSpec, "")
            loaderApi.containsMatchIn(source) || loaderExtensions.containsMatchIn(source) || loaderDescriptors.containsMatchIn(source)
        }
        check(violations.isEmpty()) { "Loader APIs in common: ${violations.joinToString()}" }
    }
}
tasks.named("check") { dependsOn(checkCommonBoundary) }
tasks.withType<JavaCompile>().configureEach { dependsOn(checkCommonBoundary) }


apply(from = rootProject.file("gradle/translation-keys.gradle.kts"))

// Version preprocessing for the shared game tests; each loader supplies its test runner.
sourceSets.create("gametest")
tasks.named("compileGametestJava") { enabled = false }
