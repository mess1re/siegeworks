package me.mss1r.siegeworks.gameplay.maintenance;

import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition.Material;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinitions;
import me.mss1r.siegeworks.entity.siege.SiegeLadderEntity;
import me.mss1r.siegeworks.entity.siege.SiegeTowerEntity;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Repair and dismantle costs, derived from the blueprint as loaded by Axiomata. A tag material is paid with any item of
 * the tag and refunded as the item the blueprint names for it in {@code returns}.
 */
public final class SiegeMaintenanceData {
    private static final int HITS_PER_RESOURCE = 4;
    private static final String LADDER_BLUEPRINT = "siegeworks:siege_ladder";
    private static final Map<String, String> ENTITY_TO_SPAWNER = Map.ofEntries(
            Map.entry("siegeworks:serpentine", "siegeworks:serpentine_spawner"),
            Map.entry("siegeworks:culverin", "siegeworks:culverin_spawner"),
            Map.entry("siegeworks:battering_ram", "siegeworks:battering_ram_spawner"),
            Map.entry("siegeworks:mangonel", "siegeworks:mangonel_spawner"),
            Map.entry("siegeworks:trebuchet", "siegeworks:trebuchet_spawner"),
            Map.entry("siegeworks:tower_crossbow", "siegeworks:tower_crossbow_spawner"),
            Map.entry("siegeworks:arcballista", "siegeworks:arcballista_spawner"),
            Map.entry("siegeworks:mantlet", "siegeworks:mantlet_spawner"),
            Map.entry("siegeworks:mons_meg", "siegeworks:mons_meg_spawner"),
            Map.entry("siegeworks:siege_tower", "siegeworks:siege_tower_spawner"),
            Map.entry("siegeworks:siege_ladder", "siegeworks:siege_ladder_spawner")
    );

    private SiegeMaintenanceData() {
    }

    public static MaintenanceRecipe forSiege(AbstractSiegeEntity siege) {
        BlueprintDefinition recipe = findRecipe(siege);
        if (recipe == null) {
            return MaintenanceRecipe.empty();
        }

        // A ladder only cost its base and the sections it has.
        int stages = siege instanceof SiegeLadderEntity ladder ? 1 + ladder.getSections() : Integer.MAX_VALUE;
        Map<String, Material> ingredients = collectIngredients(recipe, stages);
        int authoredHits = collectConstructionHits(recipe, stages);
        int totalHits = authoredHits > 0
                ? authoredHits
                : Math.max(1, total(List.copyOf(ingredients.values())) * HITS_PER_RESOURCE);
        if (siege instanceof SiegeTowerEntity tower) {
            tower.getLeatherMaterials().forEach((item, count) -> add(ingredients, Material.ofItem(item, count)));
        }
        return new MaintenanceRecipe(List.copyOf(ingredients.values()), totalHits);
    }

    public static List<Material> repairCost(AbstractSiegeEntity siege) {
        MaintenanceRecipe recipe = forSiege(siege);
        if (recipe.isEmpty()) {
            return List.of();
        }

        float missingRatio = 1.0F - Math.max(0.0F, Math.min(1.0F, siege.getHealth() / siege.getMaxHealth()));
        if (missingRatio <= 0.001F) {
            return List.of();
        }

        List<Material> result = new ArrayList<>();
        for (Material material : recipe.ingredients()) {
            result.add(material.withCount(Math.max(1, (int) Math.ceil(material.count() * missingRatio))));
        }
        return result;
    }

    public static List<Material> dismantleRefund(AbstractSiegeEntity siege) {
        MaintenanceRecipe recipe = forSiege(siege);
        if (recipe.isEmpty()) {
            return List.of();
        }

        float healthRatio = Math.max(0.0F, Math.min(1.0F, siege.getHealth() / siege.getMaxHealth()));
        List<Material> result = new ArrayList<>();
        for (Material material : recipe.ingredients()) {
            int refund = (int) Math.floor(material.count() * healthRatio * 0.5F);
            if (refund > 0) {
                result.add(material.withCount(refund));
            }
        }
        return result;
    }

    /** One line per material: its name, or "any of" for a tag, and the count. */
    public static String formatItems(List<Material> materials) {
        StringBuilder builder = new StringBuilder();
        for (Material material : materials) {
            if (!builder.isEmpty()) {
                builder.append('\n');
            }
            builder.append(material.displayName().getString()).append(" x").append(material.count());
        }
        return builder.toString();
    }

    /** Count of a material, by its key ({@code minecraft:oak_log} or {@code #minecraft:logs}), in a list. */
    public static int count(List<Material> materials, String key) {
        return materials.stream().filter(material -> material.key().equals(key)).mapToInt(Material::count).sum();
    }

    public static int countResources(AbstractSiegeEntity siege) {
        return total(forSiege(siege).ingredients());
    }

    private static int total(List<Material> materials) {
        return materials.stream().mapToInt(Material::count).sum();
    }

    public static int dismantleRequiredHits(AbstractSiegeEntity siege) {
        MaintenanceRecipe recipe = forSiege(siege);
        return recipe.isEmpty() ? 0 : Math.max(1, (int) Math.ceil(recipe.requiredHits() * 0.5D));
    }

    @Nullable
    private static BlueprintDefinition findRecipe(AbstractSiegeEntity siege) {
        if (siege instanceof SiegeLadderEntity) {
            BlueprintDefinition ladder = BlueprintDefinitions.get(LADDER_BLUEPRINT);
            if (ladder != null) {
                return ladder;
            }
        }
        String spawnerId = ENTITY_TO_SPAWNER.get(BuiltInRegistries.ENTITY_TYPE.getKey(siege.getType()).toString());
        if (spawnerId == null) {
            return null;
        }
        // Use the first matching blueprint by id so the result doesn't depend on load order.
        for (Map.Entry<String, BlueprintDefinition> entry : new TreeMap<>(BlueprintDefinitions.allById()).entrySet()) {
            if (spawnerId.equals(entry.getValue().result().item().toString())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /** Materials of the first {@code stages} stages, merged by key. */
    private static Map<String, Material> collectIngredients(BlueprintDefinition recipe, int stages) {
        Map<String, Material> result = new LinkedHashMap<>();
        for (BlueprintDefinition.Stage stage : recipe.stages().subList(0, Math.min(stages, recipe.stageCount()))) {
            stage.materials().forEach(material -> add(result, material));
        }
        return result;
    }

    private static void add(Map<String, Material> materials, Material material) {
        materials.merge(material.key(), material, (existing, more) -> existing.withCount(existing.count() + more.count()));
    }

    private static int collectConstructionHits(BlueprintDefinition recipe, int stages) {
        return recipe.stages().stream()
                .limit(stages)
                .mapToInt(stage -> Math.max(0, stage.hits()))
                .sum();
    }

    public record MaintenanceRecipe(List<Material> ingredients, int requiredHits) {
        private static MaintenanceRecipe empty() {
            return new MaintenanceRecipe(List.of(), 0);
        }

        public boolean isEmpty() {
            return ingredients.isEmpty();
        }
    }
}
