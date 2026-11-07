package me.mss1r.siegeworks.client.firearm;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import java.io.InputStreamReader;
import static org.junit.jupiter.api.Assertions.*;

class FirearmAimTest {
    @Test void firingHandCannonKeepsTheStockInTheMainHand() throws Exception {
        try (var reader = new InputStreamReader(getClass().getResourceAsStream(
                "/assets/siegeworks/firearm/animations/hand_cannon.json"), java.nio.charset.StandardCharsets.UTF_8)) {
            var clips = JsonParser.parseReader(reader).getAsJsonObject();
            var aim = parts(clips.getAsJsonObject("AIM").getAsJsonArray("frames").get(0));
            var gun = aim.get("weapon");
            var arm = aim.get("right_arm");
            var localGrip = new Quaternionf(gun.rotation()).conjugate().transform(
                    arm.rotation().transform(new Vector3f(-1,-9,0)).add(arm.position()).sub(gun.position())).div(gun.scale());
            for (var frame : clips.getAsJsonObject("FIRE").getAsJsonArray("frames")) {
                var shot = FirearmAnimation.anchorMainHand(parts(frame), aim);
                var held = shot.get("weapon");
                var holding = shot.get("right_arm");
                var palm = holding.rotation().transform(new Vector3f(-1,-9,0)).add(holding.position());
                var stock = held.rotation().transform(new Vector3f(localGrip).mul(held.scale())).add(held.position());
                assertEquals(0, palm.distance(stock), .0001, "stock detached during ignition/recoil");
                assertEquals(0, arm.position().distance(holding.position()), .0001, "shoulder detached");
            }
        }
    }

    private static java.util.Map<String, FirearmAnimation.Part> parts(com.google.gson.JsonElement value) {
        var frame = new java.util.HashMap<String, FirearmAnimation.Part>();
        value.getAsJsonObject().entrySet().forEach(e -> {
            var a = e.getValue().getAsJsonArray();
            frame.put(e.getKey(), new FirearmAnimation.Part(point(a), new Quaternionf(
                    a.get(3).getAsFloat(), a.get(4).getAsFloat(), a.get(5).getAsFloat(), a.get(6).getAsFloat()), a.get(7).getAsFloat()));
        });
        return frame;
    }

    private static Vector3f point(JsonArray a) {
        return new Vector3f(a.get(0).getAsFloat(),a.get(1).getAsFloat(),a.get(2).getAsFloat());
    }
    private static Vector3f grip(JsonArray a,float x) {
        return new Quaternionf(a.get(3).getAsFloat(),a.get(4).getAsFloat(),a.get(5).getAsFloat(),a.get(6).getAsFloat())
                .transform(new Vector3f(x,-9,0)).add(point(a));
    }
    @Test void barrelFollowsLookWhenHeadAndBodyHaveDifferentYaw() throws Exception {
        for (String gun : new String[]{"hand_cannon", "arquebus"}) {
            try (var reader = new InputStreamReader(getClass().getResourceAsStream(
                    "/assets/siegeworks/firearm/animations/"+gun+".json"), java.nio.charset.StandardCharsets.UTF_8)) {
                var json = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("AIM")
                        .getAsJsonArray("frames").get(0).getAsJsonObject();
                var frame = new java.util.HashMap<String, FirearmAnimation.Part>();
                json.entrySet().forEach(e -> {
                    var a=e.getValue().getAsJsonArray();
                    frame.put(e.getKey(),new FirearmAnimation.Part(point(a),new Quaternionf(
                            a.get(3).getAsFloat(),a.get(4).getAsFloat(),a.get(5).getAsFloat(),a.get(6).getAsFloat()),a.get(7).getAsFloat()));
                });
                for (int body=-180; body<=180; body+=60) for (int offset=-75; offset<=75; offset+=15)
                    for (int pitch=-85; pitch<=85; pitch+=17) {
                        int head=body+offset;
                        var aimed=FirearmAnimation.aim(frame,pitch,head,body,1).get("weapon");
                        var direction=aimed.rotation().transform(new Vector3f(0,0,1));
                        // LivingEntityRenderer's body rotation, model flip and study-space
                        // conversion compose to rotationY(-bodyYaw) in world coordinates.
                        new Quaternionf().rotationY((float)Math.toRadians(-body)).transform(direction);
                        double y=Math.toRadians(head), p=Math.toRadians(pitch);
                        var expected=new Vector3f((float)(-Math.sin(y)*Math.cos(p)),(float)-Math.sin(p),(float)(Math.cos(y)*Math.cos(p)));
                        assertEquals(0,direction.distance(expected),.0001,gun+" body="+body+" head="+head+" pitch="+pitch);
                    }
            }
        }
    }

    @Test void approvedAimingAndRecoilStayReachableThroughoutVerticalTravel() throws Exception {
        for(String gun:new String[]{"hand_cannon","arquebus"}) {
            var resource=getClass().getResourceAsStream("/assets/siegeworks/firearm/animations/"+gun+".json");
            assertNotNull(resource);
            try(var reader=new InputStreamReader(resource,java.nio.charset.StandardCharsets.UTF_8)) {
                var clips=JsonParser.parseReader(reader).getAsJsonObject();
                for(String action:new String[]{"AIM","FIRE"})for(var entry:clips.getAsJsonObject(action).getAsJsonArray("frames")) {
                    var frame=entry.getAsJsonObject();var right=frame.getAsJsonArray("right_arm");var left=frame.getAsJsonArray("left_arm");
                    var s0=point(right);var s1=point(left);var h0=grip(right,-1);var h1=grip(left,1);
                    var origin=point(frame.getAsJsonArray("weapon"));
                    for(int angle=-90;angle<=90;angle+=5) {
                        var look=new Quaternionf().rotationX((float)Math.toRadians(angle));
                        var a=new Vector3f(s0).sub(look.transform(new Vector3f(h0).sub(origin)));
                        var b=new Vector3f(s1).sub(look.transform(new Vector3f(h1).sub(origin)));
                        float r0=s0.distance(h0),r1=s1.distance(h1);
                        assertTrue(a.distance(b)<=r0+r1 && a.distance(b)>=Math.abs(r0-r1),gun+" unreachable at "+angle);
                        var result=FirearmAim.position(a,b,r0,r1,origin);
                        assertEquals(r0,a.distance(result),.0001,gun+" main hand detached");
                        assertEquals(r1,b.distance(result),.0001,gun+" offhand detached");
                    }
                }
            }
        }
    }
}
