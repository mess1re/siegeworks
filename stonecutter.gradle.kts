import dev.architectury.plugin.TransformingTask
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

plugins {
    id("dev.kikugie.stonecutter")
    id("dev.architectury.loom") version "1.17-SNAPSHOT" apply false
    id("architectury-plugin") version "3.5-SNAPSHOT" apply false
    id("com.gradleup.shadow") version "9.4.3" apply false
}

stonecutter active "1.21.1"

// TransformingTask shares JVM properties and a static logger across projects.
// https://github.com/architectury/architectury-transformer/issues/31
abstract class ArchitecturyTransformerGate : BuildService<BuildServiceParameters.None>

val transformerGate = gradle.sharedServices.registerIfAbsent(
    "siegeworksArchitecturyTransformerGate", ArchitecturyTransformerGate::class.java
) {
    maxParallelUsages.set(1)
}

subprojects {
    val platform = path.split(':').getOrNull(1)
    if (platform in listOf("forge", "neoforge", "fabric")) extra["loom.platform"] = platform
    tasks.withType<JavaExec>().configureEach {
        systemProperty("siegeworks.test.filter", providers.gradleProperty("gameTestFilter").getOrElse(""))
    }
    tasks.withType<TransformingTask>().configureEach {
        usesService(transformerGate)
        transformerLocation.set(layout.buildDirectory.dir("architectury-transformer/$name"))
    }
}

stonecutter parameters {
    val version = current.version
    val module = branch.id.ifEmpty { "common" }

    properties {
        tags(version, module)
    }

    swaps["mod_id"] = "\"${properties.get<String>("mod.id")}\";"
    swaps["mod_version"] = "\"${properties.get<String>("mod.version")}\";"
    swaps["minecraft"] = "\"${node.metadata.version}\";"
}
