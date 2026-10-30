package me.mss1r.siegeworks.integration.recruits;

import dev.architectury.registry.registries.RegistrySupplier;
import me.mss1r.siegeworks.api.SiegeAmmunitionMode;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.registry.SiegeworksItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

enum SiegeCommandType {
    TOWER_CROSSBOW(SiegeworksEntities.TOWER_CROSSBOW_ENTITY, SiegeworksItems.TOWER_CROSSBOW_SPAWNER,
            Kind.ARTILLERY),
    ARCBALLISTA(SiegeworksEntities.ARCBALLISTA_ENTITY, SiegeworksItems.ARCBALLISTA_SPAWNER,
            Kind.ARTILLERY),
    CULVERIN(SiegeworksEntities.CULVERIN_ENTITY, SiegeworksItems.CULVERIN_SPAWNER,
            Kind.ARTILLERY),
    SERPENTINE(SiegeworksEntities.SERPENTINE_ENTITY, SiegeworksItems.SERPENTINE_SPAWNER,
            Kind.ARTILLERY),
    MONS_MEG(SiegeworksEntities.MONS_MEG_ENTITY, SiegeworksItems.MONS_MEG_SPAWNER,
            Kind.ARTILLERY),
    ONAGER(SiegeworksEntities.ONAGER_ENTITY, SiegeworksItems.ONAGER_SPAWNER,
            Kind.ARTILLERY, SiegeAmmunitionMode.AUTO, SiegeAmmunitionMode.STANDARD,
            SiegeAmmunitionMode.INCENDIARY),
    TREBUCHET(SiegeworksEntities.TREBUCHET_ENTITY, SiegeworksItems.TREBUCHET_SPAWNER,
            Kind.ARTILLERY, SiegeAmmunitionMode.AUTO, SiegeAmmunitionMode.STANDARD,
            SiegeAmmunitionMode.INCENDIARY),
    HWACHA(SiegeworksEntities.HWACHA_ENTITY, SiegeworksItems.HWACHA_SPAWNER,
            Kind.ARTILLERY, SiegeAmmunitionMode.AUTO, SiegeAmmunitionMode.STANDARD,
            SiegeAmmunitionMode.EXPLOSIVE),
    BATTERING_RAM(SiegeworksEntities.BATTERING_RAM_ENTITY, SiegeworksItems.BATTERING_RAM_SPAWNER,
            Kind.RAM),
    MANTLET(SiegeworksEntities.MANTLET_ENTITY, SiegeworksItems.MANTLET_SPAWNER,
            Kind.MANTLET),
    SIEGE_LADDER(SiegeworksEntities.SIEGE_LADDER_ENTITY, SiegeworksItems.SIEGE_LADDER_SPAWNER,
            Kind.LADDER),
    SIEGE_TOWER(SiegeworksEntities.SIEGE_TOWER_ENTITY, SiegeworksItems.SIEGE_TOWER_SPAWNER,
            Kind.TOWER);

    enum Kind {
        ARTILLERY,
        RAM,
        MANTLET,
        LADDER,
        TOWER
    }

    private final RegistrySupplier<? extends EntityType<?>> entityType;
    private final RegistrySupplier<? extends Item> icon;
    private final Kind kind;
    private final Set<SiegeAmmunitionMode> ammunitionModes;

    SiegeCommandType(RegistrySupplier<? extends EntityType<?>> entityType,
                     RegistrySupplier<? extends Item> icon, Kind kind,
                     SiegeAmmunitionMode... ammunitionModes) {
        this.entityType = entityType;
        this.icon = icon;
        this.kind = kind;
        this.ammunitionModes = Set.of(ammunitionModes);
    }

    static SiegeCommandType from(AbstractSiegeEntity siege) {
        for (SiegeCommandType type : values()) {
            if (type.matches(siege)) {
                return type;
            }
        }
        return null;
    }

    boolean matches(AbstractSiegeEntity siege) {
        return siege.getType() == entityType.get();
    }

    ResourceLocation id() {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entityType.get());
    }

    ItemStack icon() {
        return new ItemStack(icon.get());
    }

    Kind kind() {
        return kind;
    }

    boolean supports(SiegeAmmunitionMode mode) {
        return ammunitionModes.contains(mode);
    }
}
