package me.mss1r.siegeworks.client.firearm;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class FirearmCarryTest {
    @Test void switchingWeaponsNeverBlendsTheirDifferentScales() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        var playback = new FirearmAnimation.Playback();
        Object handCannon = new Object(), arquebus = new Object();
        playback.select(handCannon, "carry", 10);
        playback.current = Map.of("weapon", part(0, 0, 0, 1));
        playback.select(arquebus, "carry", 10.1F);
        assertTrue(playback.previous.isEmpty());
        playback.current = Map.of("weapon", part(0, 0, 0, .7F));
        playback.select(arquebus, "aim", 10.2F);
        assertEquals(.7F, playback.previous.get("weapon").scale());
        playback.select(arquebus, "carry", 20);
        assertTrue(playback.previous.isEmpty(), "resuming after another item must not replay an old pose");
    }

    @Test void walkingKeepsBothToolsRigidlyAttachedToTheirHands() {
        var source = Map.of("right_arm", part(-5,22,0,1), "left_arm", part(5,22,0,1),
                "weapon", part(-6,13,2,.7F), "ramrod", part(6,13,-6,1));
        for (float phase = 0; phase < 6.3F; phase += .2F) {
            var result = FirearmAnimation.walk(source, (float)Math.cos(phase)*.5F, -(float)Math.cos(phase));
            for (String[] pair : new String[][]{{"right_arm","weapon"},{"left_arm","ramrod"}}) {
                var arm = result.get(pair[0]); var tool = result.get(pair[1]);
                var local = new Quaternionf(arm.rotation()).conjugate()
                        .transform(new Vector3f(tool.position()).sub(arm.position()));
                assertEquals(0, local.distance(new Vector3f(source.get(pair[1]).position()).sub(source.get(pair[0]).position())), .0001F);
                assertEquals(source.get(pair[1]).scale(), tool.scale());
            }
        }
        assertNotEquals(source.get("left_arm").rotation(), FirearmAnimation.walk(source,.5F,-1).get("left_arm").rotation());
    }

    private static FirearmAnimation.Part part(float x,float y,float z,float scale) {
        return new FirearmAnimation.Part(new Vector3f(x,y,z),new Quaternionf(),scale);
    }
}
