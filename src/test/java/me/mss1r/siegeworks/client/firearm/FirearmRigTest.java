package me.mss1r.siegeworks.client.firearm;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class FirearmRigTest {
    @Test void bothWeaponsUseTheSameGroundedOneHandedCarry() throws Exception {
        var hand = clips("hand_cannon").getAsJsonObject("CARRY").getAsJsonArray("frames").get(0).getAsJsonObject();
        var arquebus = clips("arquebus").getAsJsonObject("CARRY").getAsJsonArray("frames").get(0).getAsJsonObject();
        for (String part : new String[]{"right_arm", "left_arm", "torso", "head", "right_leg", "left_leg"}) {
            assertEquals(hand.get(part), arquebus.get(part), part);
        }
        var weapon = arquebus.getAsJsonArray("weapon");
        var grip = rotation(weapon).transform(new Vector3f(0, -.3F, -4).mul(weapon.get(7).getAsFloat())).add(position(weapon));
        var arm = arquebus.getAsJsonArray("right_arm");
        var palm = rotation(arm).transform(new Vector3f(-1,-9,0)).add(position(arm));
        assertEquals(0, grip.distance(palm), .001F);
    }

    private JsonObject clips(String gun) throws Exception {
        try (var reader = new InputStreamReader(getClass().getResourceAsStream(
                "/assets/siegeworks/firearm/animations/" + gun + ".json"), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static Vector3f position(JsonArray part) {
        return new Vector3f(part.get(0).getAsFloat(), part.get(1).getAsFloat(), part.get(2).getAsFloat());
    }

    private static Quaternionf rotation(JsonArray part) {
        return new Quaternionf(part.get(3).getAsFloat(), part.get(4).getAsFloat(),
                part.get(5).getAsFloat(), part.get(6).getAsFloat()).normalize();
    }

    @Test void handCannonDoesNotChangeHandsWhenStartingTheShot() throws Exception {
        var clips = clips("hand_cannon");
        var aim = clips.getAsJsonObject("AIM").getAsJsonArray("frames").get(0).getAsJsonObject();
        var fire = clips.getAsJsonObject("FIRE").getAsJsonArray("frames").get(0).getAsJsonObject();
        for (String part : new String[]{"weapon", "right_arm", "left_arm"}) {
            assertEquals(0, position(aim.getAsJsonArray(part)).distance(position(fire.getAsJsonArray(part))), .001F, part);
        }
    }

    @Test void everyPoseHasAConnectedWaist() throws Exception {
        for (String gun : new String[]{"arquebus", "hand_cannon"}) {
            for (var clip : clips(gun).entrySet()) {
                for (var value : clip.getValue().getAsJsonObject().getAsJsonArray("frames")) {
                    var frame = value.getAsJsonObject();
                    var torso = frame.getAsJsonArray("torso");
                    var waist = rotation(torso).transform(new Vector3f(0, -12, 0)).add(position(torso));
                    var hips = position(frame.getAsJsonArray("right_leg"))
                            .add(position(frame.getAsJsonArray("left_leg"))).mul(.5F);
                    assertTrue(waist.distance(hips) < .04F, gun + "/" + clip.getKey() + ": disconnected waist");
                }
            }
        }
    }

    @Test void ramrodClearsTheForearmThroughoutBothClips() throws Exception {
        for (String gun : new String[]{"arquebus", "hand_cannon"}) {
            var clip = clips(gun).getAsJsonObject("RAM");
            int index = 0;
            for (var value : clip.getAsJsonArray("frames")) {
                var frame = value.getAsJsonObject();
                var arm = frame.getAsJsonArray("left_arm");
                var rod = frame.getAsJsonArray("ramrod");
                var inverse = rotation(arm).conjugate();
                var tip = inverse.transform(position(rod).sub(position(arm)));
                var axis = inverse.transform(rotation(rod).transform(new Vector3f(0, 0, 1)));
                // Expand the vanilla arm by the shaft radius. Exclude the fist,
                // where contact is intended, and test the whole 24-pixel rod.
                float lo = 0, hi = 24;
                float[] low = {-1.15F, -7.5F, -2.15F}, high = {3.15F, 2.15F, 2.15F};
                for (int i = 0; i < 3; i++) {
                    float origin = tip.get(i), direction = axis.get(i);
                    if (Math.abs(direction) < 1e-6F) {
                        if (origin < low[i] || origin > high[i]) hi = -1;
                    } else {
                        float a = (low[i] - origin) / direction, b = (high[i] - origin) / direction;
                        lo = Math.max(lo, Math.min(a, b));
                        hi = Math.min(hi, Math.max(a, b));
                    }
                }
                assertTrue(hi <= lo, gun + ": rod intersects forearm at frame " + index);
                index++;
            }
        }
    }
}
