val probeLoader = project.path.split(':')[1]
val probeMinecraft = project.name
val isFabricProbe = probeLoader == "fabric"
val probeWorld = providers.gradleProperty("test.aimSceneWorld").orNull

if (isFabricProbe || probeWorld != null) {
    tasks.matching { it.name == if (isFabricProbe) "runProbeClient" else "runClient" }.configureEach {
        doLast {
            val directory = if (isFabricProbe) "client-probe/$probeMinecraft-fabric" else "aim-client-$probeLoader"
            val log = rootProject.file("build/$directory/logs/latest.log").readText()
            check(!log.contains("AIM_MANUAL FAIL")) { "Client aiming regression failed; see build/$directory/logs/latest.log" }
            val engine = providers.gradleProperty("test.aimSceneEngine").getOrElse(if (isFabricProbe) "manual_suite" else "tower_crossbow")
            val manual = engine in listOf("manual_suite", "culverin", "serpentine", "mons_meg")
            if (manual) {
                val expected = if (engine == "manual_suite") 3 else 1
                check(Regex("AIM_MANUAL PASS").findAll(log).count() == expected) { "Client aiming probe did not complete $expected engines" }
            } else {
                check(log.contains("AIM_CLIENT completed")) { "Client aiming probe did not complete" }
            }
        }
    }
}
