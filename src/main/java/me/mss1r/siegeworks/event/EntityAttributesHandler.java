package me.mss1r.siegeworks.event;

import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.entity.siege.ArcballistaEntity;
import me.mss1r.siegeworks.entity.siege.TowerCrossbowEntity;
import me.mss1r.siegeworks.entity.siege.BatteringRamEntity;
import me.mss1r.siegeworks.entity.siege.CulverinEntity;
import me.mss1r.siegeworks.entity.siege.SerpentineEntity;
import me.mss1r.siegeworks.entity.siege.OnagerEntity;
import me.mss1r.siegeworks.entity.siege.MantletEntity;
import me.mss1r.siegeworks.entity.siege.MonsMegEntity;
import me.mss1r.siegeworks.entity.siege.HwachaEntity;
import me.mss1r.siegeworks.entity.siege.SiegeLadderEntity;
import me.mss1r.siegeworks.entity.siege.SiegeTowerEntity;
import me.mss1r.siegeworks.entity.siege.TrebuchetEntity;
import me.mss1r.siegeworks.Siegeworks;
import dev.architectury.registry.level.entity.EntityAttributeRegistry;

public final class EntityAttributesHandler {
   private EntityAttributesHandler() {
   }

   public static void register() {
      Siegeworks.LOG.info("Registering siegeworks entity attributes");
      EntityAttributeRegistry.register(SiegeworksEntities.SERPENTINE_ENTITY, SerpentineEntity::createAttributes);
      EntityAttributeRegistry.register(SiegeworksEntities.CULVERIN_ENTITY, CulverinEntity::createAttributes);
      EntityAttributeRegistry.register(SiegeworksEntities.MONS_MEG_ENTITY, MonsMegEntity::createAttributes);
      EntityAttributeRegistry.register(SiegeworksEntities.BATTERING_RAM_ENTITY, BatteringRamEntity::createAttributes);
      EntityAttributeRegistry.register(SiegeworksEntities.ONAGER_ENTITY, OnagerEntity::createAttributes);
      EntityAttributeRegistry.register(SiegeworksEntities.TREBUCHET_ENTITY, TrebuchetEntity::createAttributes);
      EntityAttributeRegistry.register(SiegeworksEntities.TOWER_CROSSBOW_ENTITY, TowerCrossbowEntity::createAttributes);
      EntityAttributeRegistry.register(SiegeworksEntities.ARCBALLISTA_ENTITY, ArcballistaEntity::createAttributes);
      EntityAttributeRegistry.register(SiegeworksEntities.MANTLET_ENTITY, MantletEntity::createAttributes);
      EntityAttributeRegistry.register(SiegeworksEntities.SIEGE_TOWER_ENTITY, SiegeTowerEntity::createAttributes);
      EntityAttributeRegistry.register(SiegeworksEntities.SIEGE_LADDER_ENTITY, SiegeLadderEntity::createAttributes);
      EntityAttributeRegistry.register(SiegeworksEntities.HWACHA_ENTITY, HwachaEntity::createAttributes);
   }
}
