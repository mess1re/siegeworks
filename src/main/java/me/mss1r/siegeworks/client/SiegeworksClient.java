package me.mss1r.siegeworks.client;

import me.mss1r.siegeworks.client.ladder.LadderCarryClient;
import me.mss1r.siegeworks.gameplay.ladder.LadderPush;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.registries.RegistrySupplier;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.client.audio.ProjectileFlightSoundController;
import me.mss1r.siegeworks.client.entity.InvisibleEntityRenderer;
import me.mss1r.siegeworks.client.entity.arcballista.ArcballistaRenderer;
import me.mss1r.siegeworks.client.entity.batteringram.BatteringRamRenderer;
import me.mss1r.siegeworks.client.entity.fieldgun.FieldGunRenderer;
import me.mss1r.siegeworks.client.entity.hwacha.HwachaRenderer;
import me.mss1r.siegeworks.client.entity.onager.OnagerRenderer;
import me.mss1r.siegeworks.client.entity.mantlet.MantletRenderer;
import me.mss1r.siegeworks.client.entity.monsmeg.MonsMegRenderer;
import me.mss1r.siegeworks.client.entity.siegeladder.SiegeLadderRenderer;
import me.mss1r.siegeworks.client.entity.siegetower.SiegeTowerRenderer;
import me.mss1r.siegeworks.client.entity.towercrossbow.TowerCrossbowRenderer;
import me.mss1r.siegeworks.client.entity.trebuchet.TrebuchetRenderer;
import me.mss1r.siegeworks.client.projectile.RoundShotRenderer;
import me.mss1r.siegeworks.block.CaliberShotBlock;
import me.mss1r.siegeworks.gameplay.ammunition.RoundShotSize;
import me.mss1r.siegeworks.registry.SiegeworksBlocks;
import me.mss1r.siegeworks.client.projectile.CrossbowBoltRenderer;
import me.mss1r.siegeworks.client.projectile.FireArrowRenderer;
import me.mss1r.siegeworks.client.projectile.ScattershotProjectileRenderer;
import me.mss1r.siegeworks.client.projectile.SingijeonRenderer;
import me.mss1r.siegeworks.client.projectile.TrebuchetProjectileRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public final class SiegeworksClient {
    private static boolean initialized;

    private SiegeworksClient() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        ClientLifecycleEvent.CLIENT_SETUP.register(client -> {
            dev.architectury.registry.item.ItemPropertiesRegistry.register(me.mss1r.siegeworks.registry.SiegeworksItems.CANNON_BALL.get(),
                    me.mss1r.siegeworks.platform.MinecraftVersionCompat.id("siegeworks", "caliber"),
                    (stack, level, entity, seed) -> me.mss1r.siegeworks.item.RoundShotItem.caliber(stack) / 490.0F);

            dev.architectury.registry.client.rendering.RenderTypeRegistry.register(
                    net.minecraft.client.renderer.RenderType.cutout(),
                    me.mss1r.siegeworks.registry.SiegeworksBlocks.CANNON_BALL.get(),
                    me.mss1r.siegeworks.registry.SiegeworksBlocks.STONE_CANNON_BALL.get(),
                    me.mss1r.siegeworks.registry.SiegeworksBlocks.GIANT_CANNON_BALL.get());
        });

        dev.architectury.registry.ReloadListenerRegistry.register(net.minecraft.server.packs.PackType.CLIENT_RESOURCES,
                me.mss1r.siegeworks.client.firearm.FirearmMesh.INSTANCE,
                me.mss1r.siegeworks.platform.MinecraftVersionCompat.id("siegeworks", "firearm_models"));
        ClientTickEvent.CLIENT_POST.register(me.mss1r.siegeworks.client.firearm.FirearmClient::tick);
        dev.architectury.registry.ReloadListenerRegistry.register(net.minecraft.server.packs.PackType.CLIENT_RESOURCES,
                me.mss1r.siegeworks.client.firearm.FirearmAnimation.INSTANCE,
                me.mss1r.siegeworks.platform.MinecraftVersionCompat.id("siegeworks", "firearm_animations"));
        registerEntityRenderers();
        KeyMappingRegistry.register(SiegeworksKeyMappings.FREE_LOOK);
        KeyMappingRegistry.register(SiegeworksKeyMappings.BARREL_SIGHT);
        ClientTickEvent.CLIENT_POST.register(ProjectileFlightSoundController::tick);
        ClientTickEvent.CLIENT_POST.register(SiegeworksClientGameEvents::onClientTick);
        ClientTickEvent.CLIENT_PRE.register(LadderCarryClient::beforeTick);
        ClientTickEvent.CLIENT_POST.register(LadderCarryClient::afterTick);
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> LadderCarryClient.leaveWorld());
        ClientPlayerEvent.CLIENT_PLAYER_RESPAWN.register((oldPlayer, newPlayer) -> LadderCarryClient.leaveWorld());
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> LadderPush.forgetSeen());
        ClientPlayerEvent.CLIENT_PLAYER_RESPAWN.register((oldPlayer, newPlayer) -> LadderPush.forgetSeen());
    }

    private static void registerEntityRenderers() {
        register(SiegeworksEntities.FIREARM_BALL, InvisibleEntityRenderer::new);
        register(SiegeworksEntities.CANNON_BALL, context -> new RoundShotRenderer<>(context,
                SiegeworksBlocks.CANNON_BALL.get().defaultBlockState()
                        .setValue(CaliberShotBlock.CALIBER, RoundShotSize.MAX_CALIBER / RoundShotSize.STEP)));
        register(SiegeworksEntities.STONE_CANNON_BALL, context -> new RoundShotRenderer<>(context,
                SiegeworksBlocks.GIANT_CANNON_BALL.get().defaultBlockState()));
        register(SiegeworksEntities.SCATTERSHOT_PROJECTILE, ScattershotProjectileRenderer::new);
        register(SiegeworksEntities.GIANT_CANNON_BALL_PROJECTILE, context -> new RoundShotRenderer<>(context,
                SiegeworksBlocks.GIANT_CANNON_BALL.get().defaultBlockState()));
        register(SiegeworksEntities.SERPENTINE_ENTITY,
                context -> new FieldGunRenderer<>(context, "serpentine"));
        register(SiegeworksEntities.CULVERIN_ENTITY,
                context -> new FieldGunRenderer<>(context, "culverin"));
        register(SiegeworksEntities.MONS_MEG_ENTITY, MonsMegRenderer::new);
        register(SiegeworksEntities.BATTERING_RAM_ENTITY, BatteringRamRenderer::new);
        register(SiegeworksEntities.ONAGER_ENTITY, OnagerRenderer::new);
        register(SiegeworksEntities.TREBUCHET_ENTITY, TrebuchetRenderer::new);
        register(SiegeworksEntities.TOWER_CROSSBOW_ENTITY, TowerCrossbowRenderer::new);
        register(SiegeworksEntities.ARCBALLISTA_ENTITY, ArcballistaRenderer::new);
        register(SiegeworksEntities.TREBUCHET_PROJECTILE, TrebuchetProjectileRenderer::new);
        register(SiegeworksEntities.ONAGER_PROJECTILE, TrebuchetProjectileRenderer::new);
        register(SiegeworksEntities.ONAGER_PASSENGER_PROJECTILE, InvisibleEntityRenderer::new);
        register(SiegeworksEntities.TOWER_CROSSBOW_BOLT_PROJECTILE, CrossbowBoltRenderer::new);
        register(SiegeworksEntities.ARCBALLISTA_BOLT_PROJECTILE, CrossbowBoltRenderer::new);
        register(SiegeworksEntities.FIRE_ARROW, FireArrowRenderer::new);
        register(SiegeworksEntities.MANTLET_ENTITY, MantletRenderer::new);
        register(SiegeworksEntities.SIEGE_TOWER_ENTITY, SiegeTowerRenderer::new);
        register(SiegeworksEntities.SIEGE_LADDER_ENTITY, SiegeLadderRenderer::new);
        register(SiegeworksEntities.HWACHA_ENTITY, HwachaRenderer::new);
        register(SiegeworksEntities.SINGIJEON_PROJECTILE, SingijeonRenderer::new);
    }

    private static <T extends Entity> void register(RegistrySupplier<EntityType<T>> entityType,
                                                    EntityRendererProvider<T> rendererProvider) {
        EntityRendererRegistry.register(entityType, rendererProvider);
    }
}
