package me.mss1r.siegeworks.client.firearm;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import me.mss1r.axiomata.ballistics.BallisticsCommon;
import me.mss1r.siegeworks.gameplay.firearm.FirearmActions;
import me.mss1r.siegeworks.gameplay.firearm.FirearmStack;
import me.mss1r.siegeworks.item.FirearmItem;
import me.mss1r.siegeworks.particle.SiegeworksParticles;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Ignition and shot effects anchored to the rendered weapon. */
final class FirearmEffects {
    private record ShotProfile(float flashLength, float smokeSize, double spread, int puffCount) {}

    private static final ShotProfile HAND_CANNON = new ShotProfile(.28F, .42F, .048D, 8);
    private static final ShotProfile ARQUEBUS = new ShotProfile(.22F, .34F, .033D, 6);
    private static final java.util.Map<LivingEntity, Long> emittedShots = new java.util.WeakHashMap<>();
    private FirearmEffects() {}

    static void render(LivingEntity owner, FirearmItem item, PoseStack weapon, MultiBufferSource buffers, float partialTick) {
        shot(owner, item, weapon);
        var tag = MinecraftVersionCompat.customData(owner.getMainHandItem());
        long actionAge = owner.level().getGameTime() - tag.getLong("FirearmActionStart");
        boolean burning = item.matchlock() ? FirearmStack.load(owner.getMainHandItem()).matchLit()
                : owner.isUsingItem() && tag.getString("FirearmAction").equals("FIRE")
                    && actionAge >= FirearmActions.HAND_CANNON_IGNITION_TICK
                    && actionAge < FirearmActions.HAND_CANNON_SHOT_TICK;
        if (!burning) return;
        Vector3f socket = FirearmMesh.INSTANCE.ignitionSocket(item.profile().getPath());
        if (socket == null) return;
        Vector3f ignition = weapon.last().pose().transformPosition(socket.div(16));
        float time = owner.tickCount+partialTick;
        sprite(buffers,"flame",ignition,.013F*(1+.12F*(float)Math.sin(time*1.7F)),1);
        for (int i=0;i<2;i++) {
            float age = ((time+i*8)%16)/16;
            Vector3f position = new Vector3f(ignition).add(.008F*(float)Math.sin(time*.13F+i),age*.055F,0);
            sprite(buffers,"generic_"+(7-(int)(age*7)),position,.008F+age*.012F,(1-age)*.35F);
        }
    }

    private static void shot(LivingEntity owner, FirearmItem item, PoseStack weapon) {
        var tag = MinecraftVersionCompat.customData(owner.getMainHandItem());
        if (!tag.contains("FirearmShotAt")) return;
        long fired = tag.getLong("FirearmShotAt");
        long age = owner.level().getGameTime() - fired;
        if (age < 0 || age > 5 || emittedShots.getOrDefault(owner, Long.MIN_VALUE) == fired) return;
        var socket = FirearmMesh.INSTANCE.loadingSocket(item.profile().getPath());
        if (socket == null) return;
        emittedShots.put(owner, fired);
        // The firing event is authoritative; the emitter uses the same muzzle
        // transform as the visible weapon, including the first-person view model.
        Minecraft minecraft = Minecraft.getInstance();
        var camera = minecraft.gameRenderer.getMainCamera();
        var viewToWorld = new org.joml.Quaternionf(camera.rotation());
        //? if <1.21 {
        /*// Camera's 1.20 basis looks along +Z; the renderer looks along -Z.
        viewToWorld.rotateY((float)Math.PI);
        *///?}
        // 1.21 keeps the camera rotation in RenderSystem's model-view matrix;
        // 1.20 carries it in the pose stack. Combine both before leaving view space.
        Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(weapon.last().pose());
        Vec3 muzzle = camera.getPosition().add(vector(viewToWorld.transform(
                modelView.transformPosition(new Vector3f(socket.muzzle()).div(16)))));
        Vec3 forward = vector(viewToWorld.transform(modelView.transformDirection(new Vector3f(0, 0, 1)))).normalize();
        ParticleEngine particles = minecraft.particleEngine;
        RandomSource random = owner.getRandom();
        muzzleBlast(particles, random, muzzle, forward, item.matchlock() ? ARQUEBUS : HAND_CANNON);

        Vector3f socketIgnition = FirearmMesh.INSTANCE.ignitionSocket(item.profile().getPath());
        if (socketIgnition != null) {
            Vec3 ignition = camera.getPosition().add(vector(viewToWorld.transform(
                    modelView.transformPosition(socketIgnition.div(16)))));
            spawn(particles, ParticleTypes.SMALL_FLAME, ignition, new Vec3(0, .035, 0), .65F, 3);
            spawn(particles, SiegeworksParticles.SIEGE_SMOKE.get(), ignition,
                    new Vec3(0, .014, 0), .11F, 16);
        }
    }

    private static void muzzleBlast(ParticleEngine particles, RandomSource random, Vec3 muzzle, Vec3 forward,
                                    ShotProfile profile) {
        spawn(particles, BallisticsCommon.MUZZLE_CORE.get(), muzzle.add(forward.scale(.045)), forward, 1, 3);
        for (int i = 0; i < 6; i++) {
            double depth = (i + 1) / 6.0;
            Vec3 position = muzzle.add(forward.scale(.025 + profile.flashLength * depth))
                    .add(scatter(random, profile.spread * depth * .55));
            Vec3 velocity = forward.scale(.042 + .026 * depth);
            spawn(particles, i < 3 ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME,
                    position, velocity, .9F - (float) depth * .25F, 2 + random.nextInt(3));
        }

        for (int i = 0; i < profile.puffCount; i++) {
            double depth = (i + random.nextDouble()) / profile.puffCount;
            Vec3 position = muzzle.add(forward.scale(.06 + .17 * depth))
                    .add(scatter(random, profile.spread * depth));
            Vec3 velocity = forward.scale(.028 + .077 * depth)
                    .add(scatter(random, profile.spread * .55)).add(0, .012 + .012 * depth, 0);
            spawn(particles, SiegeworksParticles.SIEGE_SMOKE.get(), position, velocity,
                    profile.smokeSize * (.70F + random.nextFloat() * .42F), 28 + random.nextInt(12));
            // Solid pressure puffs fill the center while the animated smoke breaks up around it.
            if (i % 2 == 0) {
                spawn(particles, ParticleTypes.POOF, position, velocity, profile.smokeSize * 2, 12 + random.nextInt(5));
            }
        }
    }

    private static Vec3 scatter(RandomSource random, double radius) {
        return new Vec3(random.nextDouble() - .5, random.nextDouble() - .5, random.nextDouble() - .5).scale(2 * radius);
    }

    private static Vec3 vector(Vector3f value) {
        return new Vec3(value.x, value.y, value.z);
    }

    private static void spawn(ParticleEngine particles, ParticleOptions type, Vec3 position, Vec3 velocity,
                              float scale, int lifetime) {
        var particle = particles.createParticle(type, position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
        if (particle != null) {
            particle.scale(scale);
            particle.setLifetime(lifetime);
        }
    }

    private static void sprite(MultiBufferSource buffers, String texture, Vector3f position, float radius, float alpha) {
        var buffer=buffers.getBuffer(RenderType.entityTranslucent(MinecraftVersionCompat.id("minecraft","textures/particle/"+texture+".png")));
        Vector3f viewPosition = RenderSystem.getModelViewMatrix().transformPosition(new Vector3f(position));
        Matrix4f matrix=new Matrix4f(RenderSystem.getModelViewMatrix()).invert().translate(viewPosition);
        for (int i=0;i<4;i++) {
            float x=(i==0 || i==3) ? -radius : radius;
            float y=(i==0 || i==1) ? -radius : radius;
            float u=(i==0 || i==3) ? 0 : 1;
            float v=(i==0 || i==1) ? 1 : 0;
            //? if <1.21 {
            /*buffer.vertex(matrix,x,y,0).color(1F,1F,1F,alpha).uv(u,v)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(0,0,1).endVertex();
            *///?} else {
            buffer.addVertex(matrix,x,y,0).setColor(1F,1F,1F,alpha).setUv(u,v)
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0,0,1);
            //?}
        }
    }
}
