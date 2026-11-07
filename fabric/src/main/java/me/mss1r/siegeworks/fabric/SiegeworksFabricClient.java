package me.mss1r.siegeworks.fabric;

import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.platform.Platform;
import me.mss1r.siegeworks.client.harness.HarnessLayer;
import me.mss1r.siegeworks.client.item.SiegeDeploymentItemRenderer;
import me.mss1r.siegeworks.client.projectile.StuckBoltLayer;
import me.mss1r.siegeworks.client.aim.SiegeAimOverlay;
import me.mss1r.siegeworks.integration.recruits.RecruitsCompat;
import me.mss1r.siegeworks.item.SiegeDeploymentItem;
import me.mss1r.siegeworks.item.SiegeLadderDeploymentItem;
import me.mss1r.siegeworks.particle.SiegeworksParticles;
import me.mss1r.axiomata.ballistics.client.ParticleProviders;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import java.util.List;

public final class SiegeworksFabricClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        dev.architectury.registry.menu.MenuRegistry.registerScreenFactory(
                me.mss1r.siegeworks.registry.SiegeworksMenus.CASTING.get(),
                me.mss1r.siegeworks.client.CastingScreen::new);
        me.mss1r.siegeworks.client.SiegeUpdateNotifier.register();
        ParticleProviders.register(SiegeworksParticles.SET);
        HudRenderCallback.EVENT.register((graphics, delta) -> {
            //? if >=1.21 {
            float partialTick = delta.getGameTimeDeltaPartialTick(false);
            //?} else {
            /*float partialTick = delta;
            *///?}
            SiegeAimOverlay.render(graphics, partialTick, graphics.guiWidth(), graphics.guiHeight());
        });
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
            if (List.of(EntityType.HORSE, EntityType.DONKEY, EntityType.MULE, EntityType.SKELETON_HORSE,
                    EntityType.ZOMBIE_HORSE, EntityType.LLAMA, EntityType.TRADER_LLAMA, EntityType.CAMEL).contains(type)) {
                addHarness(helper, renderer);
            }
            addBolts(helper, renderer, context);
        });
        var firearmRenderer = new me.mss1r.siegeworks.client.firearm.FirearmItemRenderer();
        BuiltinItemRendererRegistry.INSTANCE.register(me.mss1r.siegeworks.registry.SiegeworksItems.HAND_CANNON.get(), firearmRenderer::renderByItem);
        BuiltinItemRendererRegistry.INSTANCE.register(me.mss1r.siegeworks.registry.SiegeworksItems.ARQUEBUS.get(), firearmRenderer::renderByItem);
        var itemRenderer = SiegeDeploymentItemRenderer.create();
        for (var item : BuiltInRegistries.ITEM) {
            if (item instanceof SiegeDeploymentItem && !(item instanceof SiegeLadderDeploymentItem)) {
                BuiltinItemRendererRegistry.INSTANCE.register(item, itemRenderer::renderByItem);
            }
        }
        if (Platform.isModLoaded("recruits"))
            ClientLifecycleEvent.CLIENT_SETUP.register(mc -> RecruitsCompat.clientSetup());
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addHarness(LivingEntityFeatureRendererRegistrationCallback.RegistrationHelper helper,
                                   net.minecraft.client.renderer.entity.LivingEntityRenderer renderer) {
        helper.register(new HarnessLayer(renderer));
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addBolts(LivingEntityFeatureRendererRegistrationCallback.RegistrationHelper helper,
                                net.minecraft.client.renderer.entity.LivingEntityRenderer renderer,
                                net.minecraft.client.renderer.entity.EntityRendererProvider.Context context) {
        if (renderer.getModel() != null) helper.register(new StuckBoltLayer(context, renderer));
    }
}
