package me.mss1r.siegeworks;

import me.mss1r.siegeworks.gameplay.ladder.LadderCarry;
import me.mss1r.siegeworks.registry.SiegeworksBlockEntities;
import me.mss1r.siegeworks.registry.SiegeworksBlocks;
import me.mss1r.siegeworks.client.SiegeworksClient;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.event.EntityAttributesHandler;
import me.mss1r.siegeworks.event.ExplosionPhysicsHandler;
import me.mss1r.siegeworks.event.MountedSiegeItemHandler;
import me.mss1r.siegeworks.event.SiegeProfileReloads;
import me.mss1r.siegeworks.event.SiegeDeploymentLimitEvents;
import me.mss1r.siegeworks.event.SiegeOwnershipEvents;
import me.mss1r.siegeworks.gameplay.ballistics.SiegeBallisticsEnvironment;
import me.mss1r.siegeworks.particle.SiegeworksParticles;
import me.mss1r.siegeworks.registry.SiegeworksItemGroups;
import me.mss1r.siegeworks.registry.SiegeworksItems;
import me.mss1r.siegeworks.network.ProfileSync;
import me.mss1r.siegeworks.network.SiegeworksNetworking;
import me.mss1r.siegeworks.registry.SiegeworksSounds;
import com.mojang.logging.LogUtils;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import me.mss1r.siegeworks.command.SiegeworksCommands;
import me.mss1r.siegeworks.debug.SiegeworksDebug;
import org.slf4j.Logger;

public final class Siegeworks {
   public static final String MOD_ID = "siegeworks";
   public static final Logger LOG = LogUtils.getLogger();
   private static boolean initialized;

   private Siegeworks() {
   }

   public static void initialize() {
      if (initialized) {
         return;
      }
      initialized = true;

      SiegeworksBlocks.registerBlocks();
      SiegeworksBlockEntities.registerBlockEntities();
      SiegeworksEntities.register();
      SiegeworksItems.registerItems();
      SiegeworksItemGroups.register();
      SiegeworksSounds.register();
      SiegeworksParticles.register();
      SiegeBallisticsEnvironment.initialize();
      SiegeworksNetworking.register();
      LadderCarry.register();
      CommandRegistrationEvent.EVENT.register(SiegeworksCommands::register);
      LifecycleEvent.SERVER_BEFORE_START.register(server -> SiegeworksDebug.reset());
      SiegeDeploymentLimitEvents.register();
      SiegeOwnershipEvents.register();
      EntityAttributesHandler.register();
      SiegeProfileReloads.register();
      ProfileSync.register();
      ExplosionPhysicsHandler.register();
      MountedSiegeItemHandler.register();
      EnvExecutor.runInEnv(Env.CLIENT, () -> SiegeworksClient::initialize);
   }
}
