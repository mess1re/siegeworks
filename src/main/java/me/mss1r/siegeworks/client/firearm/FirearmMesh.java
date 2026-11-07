package me.mss1r.siegeworks.client.firearm;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Baked local-space quads; original UVs and resource-pack textures are retained. */
public final class FirearmMesh implements ResourceManagerReloadListener {
    public static final FirearmMesh INSTANCE = new FirearmMesh();
    private record Quad(float[][] vertices, Vector3f normal) {}
    public record LoadingSocket(Vector3f muzzle, float diameter) {}
    private record Model(ResourceLocation texture, Map<String,List<Quad>> parts, LoadingSocket loading, Vector3f ignition, Vector3f groundOrigin) {}
    private final Map<String, Model> models = new HashMap<>();

    @Override public void onResourceManagerReload(ResourceManager manager) {
        models.clear();
        for (String name : List.of("hand_cannon", "arquebus", "firearm_loading_ramrod")) {
            ResourceLocation id = MinecraftVersionCompat.id("siegeworks", "models/firearm/" + name + ".json");
            try (var reader = manager.getResourceOrThrow(id).openAsReader()) {
                var root = JsonParser.parseReader(reader).getAsJsonObject();
                Map<String,List<Quad>> parts = new HashMap<>();
                for (var part : root.getAsJsonArray("parts")) {
                  List<Quad> quads = new ArrayList<>();
                  for (var face : part.getAsJsonObject().getAsJsonArray("quads")) {
                    float[][] vertices = new float[4][5];
                    for (int i = 0; i < 4; i++) for (int j = 0; j < 5; j++) vertices[i][j] = face.getAsJsonArray().get(i).getAsJsonArray().get(j).getAsFloat();
                    Vector3f a = point(vertices[1]).sub(point(vertices[0]));
                    Vector3f b = point(vertices[2]).sub(point(vertices[0]));
                    Vector3f normal = a.cross(b);
                    if (normal.lengthSquared() > 1e-12F) quads.add(new Quad(vertices, normal.normalize()));
                  }
                  parts.computeIfAbsent(part.getAsJsonObject().get("name").getAsString(),ignored->new ArrayList<>()).addAll(quads);
                }
                LoadingSocket socket = null;
                if (root.has("loading")) {
                    var loading = root.getAsJsonObject("loading");
                    var muzzle = loading.getAsJsonArray("muzzle");
                    socket = new LoadingSocket(new Vector3f(muzzle.get(0).getAsFloat(), muzzle.get(1).getAsFloat(), muzzle.get(2).getAsFloat()),
                            loading.get("boreDiameter").getAsFloat());
                }
                var ign = root.getAsJsonArray("ignition");
                Vector3f ignition = ign == null ? null : new Vector3f(ign.get(0).getAsFloat(),ign.get(1).getAsFloat(),ign.get(2).getAsFloat());
                Vector3f low = new Vector3f(Float.POSITIVE_INFINITY), high = new Vector3f(Float.NEGATIVE_INFINITY);
                for (var quads : parts.values()) for (var quad : quads) for (float[] vertex : quad.vertices) {
                    low.min(point(vertex)); high.max(point(vertex));
                }
                Vector3f groundOrigin = new Vector3f((low.x+high.x)/2, low.y, (low.z+high.z)/2).div(16);
                models.put(name, new Model(MinecraftVersionCompat.id("siegeworks", "textures/item/" + name + ".png"), Map.copyOf(parts), socket, ignition, groundOrigin));
            } catch (Exception exception) {
                me.mss1r.siegeworks.Siegeworks.LOG.error("Cannot load firearm model {}", id, exception);
            }
        }
    }

    private static Vector3f point(float[] p) { return new Vector3f(p[0], p[1], p[2]); }

    public LoadingSocket loadingSocket(String name) {
        Model model = models.get(name);
        return model == null ? null : model.loading;
    }

    public Vector3f ignitionSocket(String name) {
        Model model = models.get(name);
        return model == null || model.ignition == null ? null : new Vector3f(model.ignition);
    }

    public void alignGround(String name, PoseStack poses) {
        Model model = models.get(name);
        if (model != null) {
            var origin = model.groundOrigin;
            poses.translate(-origin.x, -origin.y, -origin.z);
        }
    }

    public void renderIcon(String name, PoseStack poses, MultiBufferSource buffers, int light, int overlay) {
        Model model = models.get(name);
        if (model == null) return;
        var rotation = new org.joml.Quaternionf().rotationZ((float)Math.toRadians(-45))
                .rotateY((float)Math.toRadians(-65));
        Vector3f low = new Vector3f(Float.POSITIVE_INFINITY), high = new Vector3f(Float.NEGATIVE_INFINITY);
        for (var quads : model.parts.values()) for (var quad : quads) for (float[] vertex : quad.vertices) {
            Vector3f point = rotation.transform(point(vertex));
            low.min(point); high.max(point);
        }
        Vector3f center = new Vector3f(low).add(high).mul(.5F);
        float scale = 14F / Math.max(high.x-low.x, high.y-low.y);
        poses.scale(scale,scale,scale);
        poses.translate(-center.x/16,-center.y/16,-center.z/16);
        poses.mulPose(rotation);
        render(name,poses,buffers,light,overlay);
    }

    public void render(String name, PoseStack poses, MultiBufferSource buffers, int light, int overlay) {
        render(name,poses,buffers,light,overlay,part->true);
    }

    public void render(String name, PoseStack poses, MultiBufferSource buffers, int light, int overlay,
                       java.util.function.Predicate<String> visiblePart) {
        Model model = models.get(name);
        if (model == null) return;
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(model.texture));
        var pose = poses.last();
        for (var part : model.parts.entrySet()) {
          if(!visiblePart.test(part.getKey())) continue;
          for (Quad quad : part.getValue()) {
            Vector3f n = pose.normal().transform(new Vector3f(quad.normal));
            for (float[] v : quad.vertices) {
                //? if <1.21 {
                /*buffer.vertex(pose.pose(), v[0] / 16, v[1] / 16, v[2] / 16)
                        .color(255,255,255,255).uv(v[3],v[4]).overlayCoords(overlay).uv2(light)
                        .normal(n.x,n.y,n.z).endVertex();
                *///?} else {
                buffer.addVertex(pose.pose(), v[0] / 16, v[1] / 16, v[2] / 16)
                        .setColor(255,255,255,255).setUv(v[3],v[4]).setOverlay(overlay).setLight(light)
                        .setNormal(n.x,n.y,n.z);
                //?}
            }
          }
        }
    }
}
