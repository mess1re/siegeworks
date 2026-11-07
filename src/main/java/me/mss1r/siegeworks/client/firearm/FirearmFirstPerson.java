package me.mss1r.siegeworks.client.firearm;

import com.google.gson.JsonParser;
import me.mss1r.siegeworks.client.firearm.FirearmAnimation.Part;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/** Camera-space weapon placement and wrist attachments, independent of the player skeleton. */
final class FirearmFirstPerson {
    private final Map<String,Map<String,Part>> placements = new HashMap<>();

    void reload(ResourceManager resources) {
        placements.clear();
        for (String gun : new String[]{"arquebus","hand_cannon"}) {
            var id=MinecraftVersionCompat.id("siegeworks","firearm/first_person/"+gun+".json");
            try(var reader=resources.getResourceOrThrow(id).openAsReader()) {
                Map<String,Part> views=new HashMap<>();
                for(var entry:JsonParser.parseReader(reader).getAsJsonObject().entrySet()) {
                    var data=entry.getValue().getAsJsonObject();
                    var p=data.getAsJsonArray("position");
                    var r=data.getAsJsonArray("rotation");
                    views.put(entry.getKey(),new Part(new Vector3f(p.get(0).getAsFloat(),p.get(1).getAsFloat(),p.get(2).getAsFloat()),
                            new Quaternionf().rotationXYZ((float)Math.toRadians(r.get(0).getAsFloat()),
                                    (float)Math.toRadians(r.get(1).getAsFloat()),(float)Math.toRadians(r.get(2).getAsFloat())),1));
                }
                placements.put(gun,Map.copyOf(views));
            } catch(Exception e) {
                me.mss1r.siegeworks.Siegeworks.LOG.error("Cannot load firearm view model {}",id,e);
            }
        }
    }

    Map<String,Part> place(String gun, String view, Map<String,Part> frame, Part reference) {
        var views=placements.get(gun);
        if(views==null)return Map.of();
        // Carry placement describes the fist, so weapons with different authored
        // origins share a hand position without changing their physical scale.
        if (view.equals("ready")) reference = carryReference(frame);
        return place(frame,reference,views.get(view));
    }

    static Part carryReference(Map<String, Part> frame) {
        Part arm = frame.get("right_arm");
        Vector3f grip = arm.rotation().transform(new Vector3f(-1,-9,0)).add(arm.position());
        return new Part(grip, frame.get("weapon").rotation(), 1);
    }

    static Map<String,Part> place(Map<String,Part> frame, Part reference, Part destination) {
        Quaternionf rotation=new Quaternionf(destination.rotation()).mul(new Quaternionf(reference.rotation()).conjugate());
        Map<String,Part> result=new HashMap<>();
        frame.forEach((name,part)->result.put(name,new Part(
                rotation.transform(new Vector3f(part.position()).sub(reference.position())).add(destination.position()),
                new Quaternionf(rotation).mul(part.rotation()),part.scale())));
        return result;
    }

    Map<String,Part> hands(Map<String,Part> frame, boolean active, float loading) {
        if(frame.isEmpty())return frame;
        Map<String,Part> result=new HashMap<>(frame);
        for(boolean left:new boolean[]{false,true}) {
            String name=left ? "left_arm" : "right_arm";
            Part source=frame.get(name);
            Vector3f grip=source.rotation().transform(new Vector3f(left ? 1 : -1,-9,0)).add(source.position());
            Vector3f shaft=null;
            boolean component=left && frame.containsKey("component");
            if(left && frame.containsKey("ramrod")) {
                Part rod=frame.get("ramrod");
                shaft=rod.rotation().transform(new Vector3f(0,0,1));
                float distance=new Vector3f(grip).sub(rod.position()).dot(shaft);
                grip=new Vector3f(rod.position()).add(new Vector3f(shaft).mul(Math.max(0,Math.min(24,distance))));
            } else if(component) grip=new Vector3f(frame.get("component").position());
            result.put(name,arm(grip,left,active ? shaft : null,component,active,loading));
        }
        Part left = result.get("left_arm");
        result.put("offhand", new Part(left.rotation().transform(new Vector3f(0, -9, -2.3F)).add(left.position()),
                new Quaternionf(left.rotation()), 1));
        return result;
    }

    private static Part arm(Vector3f grip, boolean left, Vector3f shaft, boolean component, boolean active, float loading) {
        // The sleeve extends towards the lower edge of the view, not towards a
        // world-space shoulder. Its dimensions and the wrist contact stay fixed.
        Vector3f y = active ? new Vector3f(left ? 1.5F : -1.5F,-1,-1.5F).normalize()
                : new Vector3f(left ? 2 : -2,-1,-1).normalize();
        if (loading > 0) {
            // Lead each sleeve back towards its own side of the camera as the
            // hand moves between the waist and the muzzle.
            var towardsCamera = new Vector3f(left ? 9 : -9, -44, -8).sub(grip).normalize();
            y.lerp(towardsCamera, loading).normalize();
        }
        Vector3f z;
        if(shaft!=null) {
            z=new Vector3f(shaft).cross(y);
            if(z.lengthSquared()<1e-6F)z.set(1,0,0);
            z.normalize();
            if(z.z<0)z.negate();
        } else {
            z=new Vector3f(0,0,1).sub(new Vector3f(y).mul(y.z)).normalize();
        }
        Vector3f x=new Vector3f(y).cross(z).normalize();
        z=new Vector3f(x).cross(y).normalize();
        Quaternionf rotation=new Matrix3f().setColumn(0,x).setColumn(1,y).setColumn(2,z)
                .getNormalizedRotation(new Quaternionf());
        Vector3f localGrip=new Vector3f(0,-9,shaft!=null ? 2.35F : component ? -2.3F : 0);
        Vector3f position=new Vector3f(grip).sub(rotation.transform(localGrip));
        return new Part(position,rotation,1);
    }
}
