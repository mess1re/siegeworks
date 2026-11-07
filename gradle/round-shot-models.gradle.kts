import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.api.tasks.SourceSetContainer
import kotlin.math.*

val roundShotSource = rootProject.file("src/main/resources/assets/siegeworks/models/block/round_shot")
val roundShotDisplay = rootProject.file("src/main/model_data/round_shot_display.json")
val roundShotResources = layout.buildDirectory.dir("generated/round-shot-resources")

val generateRoundShotModels = tasks.register("generateRoundShotModels") {
    inputs.dir(roundShotSource)
    inputs.file(roundShotDisplay)
    inputs.file(rootProject.file("gradle/round-shot-models.gradle.kts"))
    outputs.dir(roundShotResources)
    doLast {
        val outputRoot = roundShotResources.get().asFile.canonicalFile
        val buildRoot = layout.buildDirectory.get().asFile.canonicalFile
        check(outputRoot != buildRoot && outputRoot.toPath().startsWith(buildRoot.toPath()))
        project.delete(outputRoot)
        val target = outputRoot.resolve("assets/siegeworks")
        fun write(path: String, value: Any) {
            target.resolve(path).apply { parentFile.mkdirs(); writeText(JsonOutput.toJson(value)) }
        }
        fun vector(value: Any?): DoubleArray = (value as? List<*>)
            ?.map { (it as Number).toDouble() }?.toDoubleArray() ?: doubleArrayOf(0.0, 0.0, 0.0)
        fun rotate(p: DoubleArray, a: DoubleArray): DoubleArray {
            val x = Math.toRadians(a[0]); val y = Math.toRadians(a[1]); val z = Math.toRadians(a[2])
            val zx = cos(z) * p[0] - sin(z) * p[1]; val zy = sin(z) * p[0] + cos(z) * p[1]
            val yx = cos(y) * zx + sin(y) * p[2]; val yz = -sin(y) * zx + cos(y) * p[2]
            return doubleArrayOf(yx, cos(x) * zy - sin(x) * yz, sin(x) * zy + cos(x) * yz)
        }
        val displays = JsonSlurper().parse(roundShotDisplay) as Map<String, Map<String, Map<String, Any>>>
        fun display(caliber: Int): Map<String, Any> {
            val fraction = (caliber - 5) / 485.0
            val diameter = 3 + 7 * fraction
            return displays.getValue("small").mapValues { (name, small) ->
                val large = displays.getValue("large")[name] ?: small
                val angles = vector(small["rotation"])
                val smallScale = vector(small["scale"] ?: listOf(1, 1, 1))
                val largeScale = vector(large["scale"] ?: listOf(1, 1, 1))
                val desired = DoubleArray(3) { 3 * smallScale[it] * (1 - fraction) + 10 * largeScale[it] * fraction }
                val scale = DoubleArray(3) { desired[it] / diameter }
                val delta = rotate(doubleArrayOf(0.0, -6.5 * smallScale[1], 0.0), angles)
                val translation = vector(small["translation"])
                val centre = DoubleArray(3) { translation[it] + delta[it] }
                if (name == "gui" || name == "fixed") centre.fill(0.0)
                if (name == "ground") centre[1] += (desired[1] - 3 * smallScale[1]) / 2
                if (name.contains("hand")) centre[2] += (desired[2] - 3 * smallScale[2]) / 2
                val offset = rotate(doubleArrayOf(0.0, (diameter / 2 - 8) * scale[1], 0.0), angles)
                mapOf("rotation" to angles.toList(), "scale" to scale.toList(),
                    "translation" to List(3) { centre[it] - offset[it] })
            }
        }
        fun geometry(count: Int, caliber: Int, texture: String): Map<String, Any?> {
            val model = JsonSlurper().parse(roundShotSource.resolve("stone_490_$count.json")) as MutableMap<String, Any?>
            val nativeDiameter = if (count == 1) 10.0 else 8.0
            val fraction = (caliber - 5) / 485.0
            val scale = (3 + (nativeDiameter - 3) * fraction) / nativeDiameter
            for (element in model["elements"] as List<MutableMap<String, Any?>>) {
                for (key in listOf("from", "to")) {
                    val p = vector(element[key])
                    element[key] = listOf(8 + (p[0] - 8) * scale, p[1] * scale, 8 + (p[2] - 8) * scale)
                }
                (element["rotation"] as? MutableMap<String, Any?>)?.let { rotation ->
                    val p = vector(rotation["origin"])
                    rotation["origin"] = listOf(8 + (p[0] - 8) * scale, p[1] * scale, 8 + (p[2] - 8) * scale)
                }
            }
            model["textures"] = mapOf("shot" to texture, "particle" to "#shot")
            return model
        }
        val variants = linkedMapOf<String, Any>()
        val overrides = mutableListOf<Any>()
        for (caliber in 5..490 step 5) {
            for (count in 1..4) {
                val path = "round_shot/iron_${caliber}_$count"
                write("models/block/$path.json", geometry(count, caliber, "siegeworks:block/round_shot/iron"))
                for ((facing, angle) in listOf("north" to 0, "east" to 90, "south" to 180, "west" to 270)) {
                    variants["caliber=${caliber / 5},count=$count,facing=$facing"] =
                        mapOf("model" to "siegeworks:block/$path", "y" to angle)
                }
            }
            val path = "round_shot/iron_$caliber"
            write("models/item/$path.json", mapOf("parent" to "siegeworks:block/round_shot/iron_${caliber}_1", "display" to display(caliber)))
            overrides.add(mapOf("predicate" to mapOf("siegeworks:caliber" to caliber / 490.0), "model" to "siegeworks:item/$path"))
        }
        write("blockstates/cannon_ball.json", mapOf("variants" to variants))
        write("models/item/cannon_ball.json", mapOf("parent" to "siegeworks:item/round_shot/iron_120", "overrides" to overrides))
        for (count in 1..4) {
            write("models/block/round_shot/stone_120_$count.json", geometry(count, 120, "siegeworks:block/round_shot/stone"))
        }
        write("models/item/stone_cannon_ball.json", mapOf("parent" to "siegeworks:block/round_shot/stone_120_1", "display" to display(120)))
        write("models/item/giant_cannon_ball.json", mapOf("parent" to "siegeworks:block/round_shot/stone_490_1", "display" to display(490)))
    }
}

extensions.getByType<SourceSetContainer>().named("main") {
    resources.srcDir(generateRoundShotModels)
}
tasks.named<ProcessResources>("processResources") {
    dependsOn(generateRoundShotModels)
    duplicatesStrategy = DuplicatesStrategy.FAIL
}
