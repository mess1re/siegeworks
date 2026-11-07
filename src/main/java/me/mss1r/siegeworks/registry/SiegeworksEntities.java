package me.mss1r.siegeworks.registry;

import me.mss1r.siegeworks.Siegeworks;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import me.mss1r.siegeworks.entity.projectile.ArcballistaBoltProjectile;
import me.mss1r.siegeworks.entity.projectile.TowerCrossbowBoltProjectile;
import me.mss1r.siegeworks.entity.projectile.CannonProjectile;
import me.mss1r.siegeworks.entity.projectile.FireArrowEntity;
import me.mss1r.siegeworks.entity.projectile.GiantCannonProjectile;
import me.mss1r.siegeworks.entity.projectile.OnagerPassengerProjectile;
import me.mss1r.siegeworks.entity.projectile.SingijeonProjectile;
import me.mss1r.siegeworks.entity.projectile.ScattershotProjectile;
import me.mss1r.siegeworks.entity.projectile.TrebuchetProjectile;
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
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public interface SiegeworksEntities {
    int DEFAULT_TRACKING_RANGE = 100;
    int DEFAULT_UPDATE_INTERVAL = 1;

    DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Siegeworks.MOD_ID, Registries.ENTITY_TYPE);

    RegistrySupplier<EntityType<me.mss1r.siegeworks.entity.projectile.FirearmProjectile>> FIREARM_BALL =
            ENTITY_TYPES.register("firearm_ball", () -> EntityType.Builder.of(
                    me.mss1r.siegeworks.entity.projectile.FirearmProjectile::new, MobCategory.MISC)
                    .sized(0.02F, 0.02F).clientTrackingRange(64).updateInterval(1).build("firearm_ball"));

    RegistrySupplier<EntityType<SerpentineEntity>> SERPENTINE_ENTITY =
            ENTITY_TYPES.register("serpentine", () ->
                    EntityType.Builder.of(SerpentineEntity::new, MobCategory.MISC)
                            .sized(2.4f, 1.8f)
                            .clientTrackingRange(DEFAULT_TRACKING_RANGE)
                            .updateInterval(DEFAULT_UPDATE_INTERVAL)
                            .build("serpentine"));

    RegistrySupplier<EntityType<CulverinEntity>> CULVERIN_ENTITY =
            ENTITY_TYPES.register("culverin", () ->
                    EntityType.Builder.of(CulverinEntity::new, MobCategory.MISC)
                            .sized(2.2f, 1.7f)
                            .clientTrackingRange(DEFAULT_TRACKING_RANGE)
                            .updateInterval(DEFAULT_UPDATE_INTERVAL)
                            .build("culverin"));

    RegistrySupplier<EntityType<BatteringRamEntity>> BATTERING_RAM_ENTITY =
            ENTITY_TYPES.register("battering_ram", () ->
                    EntityType.Builder.of(BatteringRamEntity::new, MobCategory.MISC)
                            .sized(4.0f, 5.75f)
                            .clientTrackingRange(DEFAULT_TRACKING_RANGE)
                            .updateInterval(DEFAULT_UPDATE_INTERVAL)
                            .build("battering_ram"));

    RegistrySupplier<EntityType<OnagerEntity>> ONAGER_ENTITY =
            ENTITY_TYPES.register("onager", () ->
                    EntityType.Builder.of(OnagerEntity::new, MobCategory.MISC)
                            .sized(2.8f, 2.5f)
                            .clientTrackingRange(DEFAULT_TRACKING_RANGE)
                            .updateInterval(DEFAULT_UPDATE_INTERVAL)
                            .build("onager"));

    RegistrySupplier<EntityType<TrebuchetEntity>> TREBUCHET_ENTITY =
            ENTITY_TYPES.register("trebuchet", () ->
                    EntityType.Builder.of(TrebuchetEntity::new, MobCategory.MISC)
                            .sized(5.0f, 7.5f)
                            .clientTrackingRange(DEFAULT_TRACKING_RANGE)
                            .updateInterval(DEFAULT_UPDATE_INTERVAL)
                            .build("trebuchet"));

    RegistrySupplier<EntityType<TowerCrossbowEntity>> TOWER_CROSSBOW_ENTITY =
            ENTITY_TYPES.register("tower_crossbow", () ->
                    EntityType.Builder.of(TowerCrossbowEntity::new, MobCategory.MISC)
                            .sized(4.0f, 1.9f)
                            .clientTrackingRange(DEFAULT_TRACKING_RANGE)
                            .updateInterval(DEFAULT_UPDATE_INTERVAL)
                            .build("tower_crossbow"));

    RegistrySupplier<EntityType<ArcballistaEntity>> ARCBALLISTA_ENTITY =
            ENTITY_TYPES.register("arcballista", () ->
                    EntityType.Builder.of(ArcballistaEntity::new, MobCategory.MISC)
                            .sized(2.1f, 1.15f)
                            .clientTrackingRange(DEFAULT_TRACKING_RANGE)
                            .updateInterval(DEFAULT_UPDATE_INTERVAL)
                            .build("arcballista"));

    RegistrySupplier<EntityType<MantletEntity>> MANTLET_ENTITY =
            ENTITY_TYPES.register("mantlet", () ->
                    EntityType.Builder.of(MantletEntity::new, MobCategory.MISC)
                            .sized(4.5F, 4.4F)
                            .clientTrackingRange(DEFAULT_TRACKING_RANGE)
                            .updateInterval(DEFAULT_UPDATE_INTERVAL)
                            .build("mantlet"));

    RegistrySupplier<EntityType<MonsMegEntity>> MONS_MEG_ENTITY =
            ENTITY_TYPES.register("mons_meg", () ->
                    EntityType.Builder.of(MonsMegEntity::new, MobCategory.MISC)
                            .sized(4.25f, 2.5f)
                            .clientTrackingRange(DEFAULT_TRACKING_RANGE)
                            .updateInterval(DEFAULT_UPDATE_INTERVAL)
                            .build("mons_meg"));

    RegistrySupplier<EntityType<SiegeTowerEntity>> SIEGE_TOWER_ENTITY =
            ENTITY_TYPES.register("siege_tower", () ->
                    EntityType.Builder.of(SiegeTowerEntity::new, MobCategory.MISC)
                            .sized(7.0f, 17.2f)
                            .clientTrackingRange(DEFAULT_TRACKING_RANGE)
                            .updateInterval(DEFAULT_UPDATE_INTERVAL)
                            .build("siege_tower"));

    RegistrySupplier<EntityType<SiegeLadderEntity>> SIEGE_LADDER_ENTITY =
            ENTITY_TYPES.register("siege_ladder", () ->
                    EntityType.Builder.of(SiegeLadderEntity::new, MobCategory.MISC)
                            .sized(1.0f, 3.0f)
                            .clientTrackingRange(DEFAULT_TRACKING_RANGE)
                            .updateInterval(DEFAULT_UPDATE_INTERVAL)
                            .build("siege_ladder"));

    RegistrySupplier<EntityType<HwachaEntity>> HWACHA_ENTITY =
            ENTITY_TYPES.register("hwacha", () ->
                    EntityType.Builder.of(HwachaEntity::new, MobCategory.MISC)
                            .sized(2.2f, 2.6f)
                            .clientTrackingRange(DEFAULT_TRACKING_RANGE)
                            .updateInterval(DEFAULT_UPDATE_INTERVAL)
                            .build("hwacha"));

    RegistrySupplier<EntityType<CannonProjectile>> CANNON_BALL =
            ENTITY_TYPES.register("cannon_ball", () ->
                    EntityType.Builder.<CannonProjectile>of(CannonProjectile::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(DEFAULT_TRACKING_RANGE)
                            .updateInterval(DEFAULT_UPDATE_INTERVAL)
                            .build("cannon_ball"));

    RegistrySupplier<EntityType<CannonProjectile>> STONE_CANNON_BALL =
            ENTITY_TYPES.register("stone_cannon_ball", () ->
                    EntityType.Builder.<CannonProjectile>of(CannonProjectile::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(DEFAULT_TRACKING_RANGE)
                            .updateInterval(DEFAULT_UPDATE_INTERVAL)
                            .build("stone_cannon_ball"));

    RegistrySupplier<EntityType<ScattershotProjectile>> SCATTERSHOT_PROJECTILE =
            ENTITY_TYPES.register("scattershot_projectile", () ->
                    EntityType.Builder.<ScattershotProjectile>of(ScattershotProjectile::new, MobCategory.MISC)
                            .sized(0.08F, 0.08F)
                            .clientTrackingRange(DEFAULT_TRACKING_RANGE)
                            .updateInterval(DEFAULT_UPDATE_INTERVAL)
                            .build("scattershot_projectile"));

    RegistrySupplier<EntityType<TrebuchetProjectile>> TREBUCHET_PROJECTILE =
            ENTITY_TYPES.register("trebuchet_projectile", () ->
                    EntityType.Builder.<TrebuchetProjectile>of(TrebuchetProjectile::new, MobCategory.MISC)
                            .sized(0.75f, 0.75f)
                            .clientTrackingRange(DEFAULT_TRACKING_RANGE)
                            .updateInterval(DEFAULT_UPDATE_INTERVAL)
                            .build("trebuchet_projectile"));

    RegistrySupplier<EntityType<TrebuchetProjectile>> ONAGER_PROJECTILE =
            ENTITY_TYPES.register("onager_projectile", () ->
                    EntityType.Builder.<TrebuchetProjectile>of(TrebuchetProjectile::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(DEFAULT_TRACKING_RANGE)
                            .updateInterval(DEFAULT_UPDATE_INTERVAL)
                            .build("onager_projectile"));

    RegistrySupplier<EntityType<OnagerPassengerProjectile>> ONAGER_PASSENGER_PROJECTILE =
            ENTITY_TYPES.register("onager_passenger_projectile", () ->
                    EntityType.Builder.<OnagerPassengerProjectile>of(OnagerPassengerProjectile::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(DEFAULT_TRACKING_RANGE)
                            .updateInterval(DEFAULT_UPDATE_INTERVAL)
                            .build("onager_passenger_projectile"));

    RegistrySupplier<EntityType<TowerCrossbowBoltProjectile>> TOWER_CROSSBOW_BOLT_PROJECTILE =
            ENTITY_TYPES.register("tower_crossbow_bolt_projectile", () ->
                    EntityType.Builder.<TowerCrossbowBoltProjectile>of(TowerCrossbowBoltProjectile::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(DEFAULT_TRACKING_RANGE)
                            .updateInterval(DEFAULT_UPDATE_INTERVAL)
                            .build("tower_crossbow_bolt_projectile"));

    RegistrySupplier<EntityType<ArcballistaBoltProjectile>> ARCBALLISTA_BOLT_PROJECTILE =
            ENTITY_TYPES.register("arcballista_bolt_projectile", () ->
                    EntityType.Builder.<ArcballistaBoltProjectile>of(ArcballistaBoltProjectile::new, MobCategory.MISC)
                            .sized(0.4f, 0.4f)
                            .clientTrackingRange(DEFAULT_TRACKING_RANGE)
                            .updateInterval(DEFAULT_UPDATE_INTERVAL)
                            .build("arcballista_bolt_projectile"));

    RegistrySupplier<EntityType<FireArrowEntity>> FIRE_ARROW =
            ENTITY_TYPES.register("fire_arrow", () ->
                    EntityType.Builder.<FireArrowEntity>of(FireArrowEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(DEFAULT_TRACKING_RANGE)
                            .updateInterval(DEFAULT_UPDATE_INTERVAL)
                            .build("fire_arrow"));

    RegistrySupplier<EntityType<GiantCannonProjectile>> GIANT_CANNON_BALL_PROJECTILE =
            ENTITY_TYPES.register("giant_cannon_ball_projectile", () ->
                    EntityType.Builder.<GiantCannonProjectile>of(GiantCannonProjectile::new, MobCategory.MISC)
                            .sized(1.0f, 1.0f)
                            .clientTrackingRange(DEFAULT_TRACKING_RANGE)
                            .updateInterval(DEFAULT_UPDATE_INTERVAL)
                            .build("giant_cannon_ball_projectile"));

    RegistrySupplier<EntityType<SingijeonProjectile>> SINGIJEON_PROJECTILE =
            ENTITY_TYPES.register("singijeon_projectile", () ->
                    EntityType.Builder.<SingijeonProjectile>of(SingijeonProjectile::new, MobCategory.MISC)
                            .sized(0.2f, 0.2f)
                            .clientTrackingRange(DEFAULT_TRACKING_RANGE)
                            .updateInterval(DEFAULT_UPDATE_INTERVAL)
                            .build("singijeon_projectile"));

    static void register() {
        ENTITY_TYPES.register();
        Siegeworks.LOG.info("Registering entities for " + Siegeworks.MOD_ID);
    }
}
