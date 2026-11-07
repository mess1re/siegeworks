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

class FirearmLoadingFitTest {
    private JsonObject resource(String path) throws Exception {
        try (var reader = new InputStreamReader(getClass().getResourceAsStream("/assets/siegeworks/"+path), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static Vector3f point(JsonArray a) {
        return new Vector3f(a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat());
    }

    @Test void bothAuthoredInsertionPathsClearTheirActualBores() throws Exception {
        for (String name : new String[]{"arquebus", "hand_cannon"}) {
            var socket = resource("models/firearm/"+name+".json").getAsJsonObject("loading");
            var frames = resource("firearm/animations/"+name+".json").getAsJsonObject("BALL").getAsJsonArray("frames");
            int insideFrames = 0;
            for (var entry : frames) {
                var frame = entry.getAsJsonObject();
                var gun = frame.getAsJsonArray("weapon");
                var ball = frame.getAsJsonArray("component");
                if (ball.get(7).getAsFloat() < .99F) continue;
                var rotation = new Quaternionf(gun.get(3).getAsFloat(),gun.get(4).getAsFloat(),gun.get(5).getAsFloat(),gun.get(6).getAsFloat()).normalize();
                float scale = gun.get(7).getAsFloat();
                Vector3f muzzle = rotation.transform(point(socket.getAsJsonArray("muzzle")).mul(scale)).add(point(gun));
                Vector3f axis = rotation.transform(new Vector3f(0,0,1));
                Vector3f held = point(ball);
                Vector3f position = FirearmLoadingFit.position(held, muzzle, axis);
                // Allow the ball to roll within the four-pixel-wide hand, not leave its palm.
                assertTrue(position.distance(held) < .3F, name+": excessive separation from pushing hand");
                float bore = socket.get("boreDiameter").getAsFloat()*scale;
                float radius = FirearmLoadingFit.diameter(position.distance(muzzle), 1.3F, bore)/2;
                Vector3f delta = new Vector3f(position).sub(muzzle);
                float axial = delta.dot(axis);
                if (axial > radius || delta.length() > 2) continue;
                float radial = new Vector3f(delta).sub(new Vector3f(axis).mul(axial)).length();
                float crossSection = axial > 0 ? (float)Math.sqrt(radius*radius-axial*axial) : radius;
                assertTrue(radial+crossSection < bore/2, name+": ball intersects muzzle at "+delta);
                insideFrames++;
            }
            assertTrue(insideFrames > 3, name+": no visible insertion covered");
        }
    }
}
