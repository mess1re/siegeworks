val probeLoader = project.path.split(':')[1]
val probeMinecraft = project.name
val isFabricProbe = probeLoader == "fabric"
val probeWorld = providers.gradleProperty("test.aimSceneWorld").orNull

if (isFabricProbe || probeWorld != null) {
    tasks.matching { it.name == if (isFabricProbe) "runProbeClient" else "runClient" }.configureEach {
        doLast {
            val directory = if (isFabricProbe) "client-probe/$probeMinecraft-fabric" else "aim-client-$probeLoader"
            val log = rootProject.file("build/$directory/logs/latest.log").readText()
            val engine = providers.gradleProperty("test.aimSceneEngine").getOrElse(if (isFabricProbe) "manual_suite" else "tower_crossbow")
            val (result, expected) = when (engine) {
                "manual_suite", "manual_frame_suite" -> "AIM_FRAME" to 6
                "draft_suite" -> "DRAFT_CLIENT" to 6
                "gravity_scene" -> "GRAVITY_CLIENT" to 1
                "arcballista_parking" -> "ARCBALLISTA_PARK" to 1
                else -> "AIM_CLIENT" to 0
            }
            check(!log.contains("$result FAIL")) { "Client regression failed; see build/$directory/logs/latest.log" }
            if (expected > 0) {
                check(Regex("$result PASS").findAll(log).count() == expected) {
                    "Client probe did not complete $expected scenarios ($result)"
                }
            } else {
                check(log.contains("$result completed")) { "Client probe did not complete" }
            }
        }
    }
}
