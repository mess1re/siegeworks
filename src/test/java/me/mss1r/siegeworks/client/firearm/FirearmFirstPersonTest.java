package me.mss1r.siegeworks.client.firearm;

import com.google.gson.JsonParser;
import me.mss1r.siegeworks.client.firearm.FirearmAnimation.Part;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FirearmFirstPersonTest {
    @Test void idleWeaponsClearTheCameraAndStayBesideTheSightLine() throws Exception {
        for (String gun : new String[]{"arquebus", "hand_cannon"}) {
            try (var placements = new InputStreamReader(getClass().getResourceAsStream(
                    "/assets/siegeworks/firearm/first_person/" + gun + ".json"), StandardCharsets.UTF_8);
                 var meshReader = new InputStreamReader(getClass().getResourceAsStream(
                    "/assets/siegeworks/models/firearm/" + gun + ".json"), StandardCharsets.UTF_8);
                 var animationReader = new InputStreamReader(getClass().getResourceAsStream(
                    "/assets/siegeworks/firearm/animations/" + gun + ".json"), StandardCharsets.UTF_8)) {
                var ready = JsonParser.parseReader(placements).getAsJsonObject().getAsJsonObject("ready");
                var r = ready.getAsJsonArray("rotation");
                var p = ready.getAsJsonArray("position");
                var rotation = new Quaternionf().rotationXYZ((float)Math.toRadians(r.get(0).getAsFloat()),
                        (float)Math.toRadians(r.get(1).getAsFloat()), (float)Math.toRadians(r.get(2).getAsFloat()));
                var origin = new Vector3f(p.get(0).getAsFloat(), p.get(1).getAsFloat(), p.get(2).getAsFloat());
                assertTrue(rotation.transform(new Vector3f(0,0,1)).y > .5F, "idle should be carried upright beside the view");
                var carry = frame(JsonParser.parseReader(animationReader).getAsJsonObject()
                        .getAsJsonObject("CARRY").getAsJsonArray("frames").get(0));
                var placed = FirearmFirstPerson.place(carry, FirearmFirstPerson.carryReference(carry), new Part(origin, rotation, 1));
                Part arm = placed.get("right_arm"), weapon = placed.get("weapon");
                var grip = arm.rotation().transform(new Vector3f(-1,-9,0)).add(arm.position());
                assertEquals(0, grip.distance(origin), .0001F, "resting hand moved with the weapon pivot");
                Part viewArm = new FirearmFirstPerson().hands(placed, false, 0).get("right_arm");
                for (var part : JsonParser.parseReader(meshReader).getAsJsonObject().getAsJsonArray("parts")) {
                    for (var quad : part.getAsJsonObject().getAsJsonArray("quads")) for (var vertex : quad.getAsJsonArray()) {
                        var v = vertex.getAsJsonArray();
                        var point = new Vector3f(v.get(0).getAsFloat(),v.get(1).getAsFloat(),v.get(2).getAsFloat());
                        point.mul(weapon.scale());
                        weapon.rotation().transform(point).add(weapon.position());
                        assertTrue(point.z > 3, gun + " stock intersects the camera");
                        assertTrue(point.x < -3, gun + " crosses the centre of the view");
                        var local = new Quaternionf(viewArm.rotation()).conjugate()
                                .transform(new Vector3f(point).sub(viewArm.position()));
                        assertFalse(Math.abs(local.x)<2.25F && Math.abs(local.z)<2.25F && local.y>-7 && local.y<2,
                                gun + "/" + part.getAsJsonObject().get("name") + " intersects the forearm above the fist");
                    }
                }
            }
        }
    }

    @Test void loadingSleevesContinueOutsideTheViewInsteadOfShowingShoulderCaps() throws Exception {
        for (String gun : new String[]{"arquebus", "hand_cannon"}) {
            try (var placementReader = new InputStreamReader(getClass().getResourceAsStream(
                    "/assets/siegeworks/firearm/first_person/" + gun + ".json"), StandardCharsets.UTF_8);
                 var animationReader = new InputStreamReader(getClass().getResourceAsStream(
                    "/assets/siegeworks/firearm/animations/" + gun + ".json"), StandardCharsets.UTF_8)) {
                var placement = JsonParser.parseReader(placementReader).getAsJsonObject().getAsJsonObject("loading");
                var p = placement.getAsJsonArray("position");
                var r = placement.getAsJsonArray("rotation");
                Part destination = new Part(new Vector3f(p.get(0).getAsFloat(), p.get(1).getAsFloat(), p.get(2).getAsFloat()),
                        new Quaternionf().rotationXYZ((float)Math.toRadians(r.get(0).getAsFloat()),
                                (float)Math.toRadians(r.get(1).getAsFloat()), (float)Math.toRadians(r.get(2).getAsFloat())), 1);
                var clips = JsonParser.parseReader(animationReader).getAsJsonObject();
                for (String action : new String[]{"RAM", "BALL", "POWDER"}) {
                    var frames = clips.getAsJsonObject(action).getAsJsonArray("frames");
                    Part reference = frame(frames.get(0)).get("weapon");
                    for (var value : frames) {
                        var posed = new FirearmFirstPerson().hands(FirearmFirstPerson.place(frame(value), reference, destination), true, 1);
                        var bore = posed.get("weapon").rotation().transform(new Vector3f(0, 0, 1));
                        float minimumRise = action.equals("RAM") ? .4F : .5F;
                        assertTrue(bore.y > minimumRise, gun + " " + action + " points the muzzle too low to load");
                        Part arm = posed.get("left_arm");
                        for (float x : new float[]{-2.25F, 2.25F}) for (float z : new float[]{-2.25F, 2.25F}) {
                            Vector3f corner = arm.rotation().transform(new Vector3f(x, 2, z)).add(arm.position());
                            float vertical = (float)Math.tan(Math.toRadians(70.0 / 2));
                            boolean inView = corner.z > .1F && Math.abs(corner.y) < corner.z * vertical
                                    && Math.abs(corner.x) < corner.z * vertical * 16 / 9;
                            assertFalse(inView, gun + " " + action + " exposes the sleeve's shoulder cap");
                        }
                    }
                }
            }
        }
    }

    @Test void cameraPlacementPreservesToolDistancesAndScaleThroughoutLoading() throws Exception {
        for (String gun : new String[]{"arquebus", "hand_cannon"}) {
            try (var reader = new InputStreamReader(getClass().getResourceAsStream(
                    "/assets/siegeworks/firearm/animations/" + gun + ".json"), StandardCharsets.UTF_8)) {
                var clips = JsonParser.parseReader(reader).getAsJsonObject();
                for (String action : new String[]{"RAM", "BALL", "POWDER"}) {
                    var frames = clips.getAsJsonObject(action).getAsJsonArray("frames");
                    Part reference = frame(frames.get(0)).get("weapon");
                    Part destination = new Part(new Vector3f(-3, -10, 14),
                            new Quaternionf().rotationXYZ(-.7F, .35F, 0), 1);
                    for (var value : frames) {
                        var source = frame(value);
                        var placed = FirearmFirstPerson.place(source, reference, destination);
                        for (String name : new String[]{"ramrod", "component"}) {
                            if (!source.containsKey(name)) continue;
                            assertEquals(source.get(name).position().distance(source.get("weapon").position()),
                                    placed.get(name).position().distance(placed.get("weapon").position()), .0001F);
                            assertEquals(source.get(name).scale(), placed.get(name).scale());
                        }
                        if (action.equals("RAM")) {
                            var posed = new FirearmFirstPerson().hands(placed, true, 1);
                            Part arm = posed.get("left_arm"), rod = posed.get("ramrod");
                            var inverse = new Quaternionf(arm.rotation()).conjugate();
                            var axis = inverse.transform(rod.rotation().transform(new Vector3f(0, 0, 1)));
                            var point = inverse.transform(new Vector3f(rod.position()).sub(arm.position()));
                            // A tangent shaft stays outside the forearm along its entire length.
                            assertEquals(0, axis.z, .0001F);
                            assertEquals(2.35F, point.z, .0001F);
                        }
                    }
                }
            }
        }
    }

    private static Map<String, Part> frame(com.google.gson.JsonElement value) {
        Map<String, Part> parts = new HashMap<>();
        value.getAsJsonObject().entrySet().forEach(entry -> {
            var a = entry.getValue().getAsJsonArray();
            parts.put(entry.getKey(), new Part(new Vector3f(a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()),
                    new Quaternionf(a.get(3).getAsFloat(), a.get(4).getAsFloat(), a.get(5).getAsFloat(), a.get(6).getAsFloat()).normalize(),
                    a.get(7).getAsFloat()));
        });
        return parts;
    }
}
