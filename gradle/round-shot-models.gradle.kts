import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.language.jvm.tasks.ProcessResources
import kotlin.math.*

// Bake ordinary Minecraft models once at build time; placed ammunition needs no block entity renderer.
tasks.named<ProcessResources>("processResources") {
    inputs.file(rootProject.file("gradle/round-shot-models.gradle.kts"))
    doLast {
        val source = rootProject.file("src/main/resources/assets/siegeworks")
        val target = destinationDir.resolve("assets/siegeworks")
        fun read(path: String): MutableMap<String, Any?> = JsonSlurper().parse(source.resolve(path)) as MutableMap<String, Any?>
        fun write(path: String, value: Any) { target.resolve(path).apply { parentFile.mkdirs(); writeText(JsonOutput.toJson(value)) } }
        fun vector(value: Any?): DoubleArray = (value as? List<*>)?.map { (it as Number).toDouble() }?.toDoubleArray() ?: doubleArrayOf(0.0,0.0,0.0)
        fun rotate(p: DoubleArray, a: DoubleArray): DoubleArray {
            val x = Math.toRadians(a[0]); val y = Math.toRadians(a[1]); val z = Math.toRadians(a[2])
            val zx = cos(z)*p[0]-sin(z)*p[1]; val zy = sin(z)*p[0]+cos(z)*p[1]
            val yx = cos(y)*zx+sin(y)*p[2]; val yz = -sin(y)*zx+cos(y)*p[2]
            return doubleArrayOf(yx,cos(x)*zy-sin(x)*yz,sin(x)*zy+cos(x)*yz)
        }
        val oldDisplay = read("models/block/cannon_ball_1.json")["display"] as Map<String,Map<String,Any>>
        val bigDisplay = read("models/block/giant_cannon_ball_1.json")["display"] as Map<String,Map<String,Any>>
        fun display(caliber: Int): Map<String,Any> {
            val t = (caliber-5)/485.0; val diameter = 3+7*t
            return oldDisplay.mapValues { (name, old) ->
                val big = bigDisplay[name] ?: old
                val angles = vector(old["rotation"])
                val oldScale = vector(old["scale"] ?: listOf(1,1,1)); val bigScale = vector(big["scale"] ?: listOf(1,1,1))
                val desired = DoubleArray(3) { 3*oldScale[it]*(1-t)+10*bigScale[it]*t }
                val scale = DoubleArray(3) { desired[it]/diameter }
                val delta = rotate(doubleArrayOf(0.0,-6.5*oldScale[1],0.0),angles)
                val oldTranslation = vector(old["translation"])
                val centre = DoubleArray(3) { oldTranslation[it]+delta[it] }
                if (name == "gui" || name == "fixed") centre.fill(0.0)
                if (name == "ground") centre[1] += (desired[1]-3*oldScale[1])/2
                if (name.contains("hand")) centre[2] += (desired[2]-3*oldScale[2])/2
                val offset = rotate(doubleArrayOf(0.0,(diameter/2-8)*scale[1],0.0),angles)
                mapOf("rotation" to angles.toList(),"scale" to scale.toList(),"translation" to List(3) { centre[it]-offset[it] })
            }
        }
        fun geometry(count: Int, caliber: Int, texture: String): MutableMap<String,Any?> {
            val model = read("models/block/giant_cannon_ball_$count.json")
            val native = if(count==1) 10.0 else 8.0
            val scale = (3+(native-3)*(caliber-5)/485.0)/native
            for(element in model["elements"] as List<MutableMap<String,Any?>>) {
                for(key in listOf("from","to")) {
                    val p = vector(element[key]); element[key] = listOf(8+(p[0]-8)*scale,p[1]*scale,8+(p[2]-8)*scale)
                }
                (element["rotation"] as? MutableMap<String,Any?>)?.set("origin",listOf(8,0,8))
            }
            model.remove("display");model["textures"] = mapOf("0" to texture,"particle" to texture)
            return model
        }
        val variants = linkedMapOf<String,Any>()
        val overrides = mutableListOf<Any>()
        for(caliber in 5..490 step 5) {
            for(count in 1..4) {
                val path = "round_shot/iron_${caliber}_$count"
                write("models/block/$path.json",geometry(count,caliber,"siegeworks:block/iron_round_shot"))
                for((facing,angle) in listOf("north" to 0,"east" to 90,"south" to 180,"west" to 270)) {
                    variants["caliber=${caliber/5},count=$count,facing=$facing"] = mapOf("model" to "siegeworks:block/$path","y" to angle)
                }
            }
            val path = "round_shot/iron_$caliber"
            write("models/item/$path.json",mapOf("parent" to "siegeworks:block/round_shot/iron_${caliber}_1","display" to display(caliber)))
            overrides.add(mapOf("predicate" to mapOf("siegeworks:caliber" to caliber/490.0),"model" to "siegeworks:item/$path"))
        }
        write("blockstates/cannon_ball.json",mapOf("variants" to variants))
        write("models/item/cannon_ball.json",mapOf("parent" to "siegeworks:item/round_shot/iron_120","overrides" to overrides))
        for(count in 1..4) write("models/block/stone_cannon_ball_$count.json",geometry(count,120,"siegeworks:block/giant_cannon_ball"))
        write("models/item/stone_cannon_ball.json",mapOf("parent" to "siegeworks:block/stone_cannon_ball_1","display" to display(120)))
        write("models/item/giant_cannon_ball.json",mapOf("parent" to "siegeworks:block/giant_cannon_ball_1","display" to display(490)))
    }
}
