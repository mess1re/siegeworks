package me.mss1r.siegeworks.client;

import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.client.aim.SiegeAimOverlay;
import me.mss1r.siegeworks.client.projectile.StuckBoltLayer;
import me.mss1r.siegeworks.particle.SiegeworksParticles;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.world.entity.EntityType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import java.util.List;
import me.mss1r.siegeworks.client.harness.HarnessLayer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;

@SuppressWarnings("removal")
@EventBusSubscriber(
   modid = Siegeworks.MOD_ID,
   bus = EventBusSubscriber.Bus.MOD,
   value = {Dist.CLIENT}
)
public final class SiegeworksClientModEvents {
   @SubscribeEvent
   public static void clientSetup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
      event.enqueueWork(() -> dev.architectury.registry.menu.MenuRegistry.registerScreenFactory(
              me.mss1r.siegeworks.registry.SiegeworksMenus.CASTING.get(), CastingScreen::new));
      event.enqueueWork(() -> dev.architectury.event.events.client.ClientTickEvent.CLIENT_POST.register(SiegeUpdateNotifier::tick));
   }

   private SiegeworksClientModEvents() {
   }

   @SubscribeEvent
   public static void addHarnessLayer(EntityRenderersEvent.AddLayers event) {
      for (EntityType<? extends AbstractHorse> type : List.of(
              EntityType.HORSE, EntityType.DONKEY, EntityType.MULE,
              EntityType.SKELETON_HORSE, EntityType.ZOMBIE_HORSE,
              EntityType.LLAMA, EntityType.TRADER_LLAMA, EntityType.CAMEL)) {
         LivingEntityRenderer<AbstractHorse, EntityModel<AbstractHorse>> renderer = event.getRenderer(type);
         if (renderer instanceof LivingEntityRenderer<?, ?>) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            LivingEntityRenderer raw = renderer;
            raw.addLayer(new HarnessLayer<>(raw));
         }
      }

      for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
         var renderer = event.getEntityRenderer(type);
         if (renderer instanceof LivingEntityRenderer<?, ?> livingRenderer) {
            addStuckBoltLayer(event, livingRenderer);
         }
      }

      for (var skin : event.getSkins()) {
         Object renderer = event.getSkin(skin);
         if (renderer instanceof LivingEntityRenderer<?, ?> livingRenderer) {
            addStuckBoltLayer(event, livingRenderer);
         }
      }
   }

   @SuppressWarnings({"rawtypes", "unchecked"})
   private static void addStuckBoltLayer(EntityRenderersEvent.AddLayers event,
                                         LivingEntityRenderer<?, ?> renderer) {
      // Renderer replacements from other mods may not expose a vanilla entity model.
      if (renderer.getModel() == null) {
         return;
      }
      LivingEntityRenderer raw = renderer;
      raw.addLayer(new StuckBoltLayer<>(event.getContext(), raw));
   }

   @SubscribeEvent
   public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
      Siegeworks.LOG.info("Registering Siegeworks particle providers");
      me.mss1r.axiomata.ballistics.client.ParticleProviders.register(event, SiegeworksParticles.SET);
   }

   @SubscribeEvent
   public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
      event.registerAbove(VanillaGuiOverlay.CROSSHAIR.id(), "siege_aim",
              (forgeGui, graphics, partialTick, width, height) ->
                      SiegeAimOverlay.render(graphics, partialTick, width, height));
   }

}
