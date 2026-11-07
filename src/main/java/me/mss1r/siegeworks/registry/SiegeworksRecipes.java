package me.mss1r.siegeworks.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.item.crafting.SiegeLadderExtensionRecipe;
import me.mss1r.siegeworks.item.crafting.SiegeManualRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;

public final class SiegeworksRecipes {
    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeType<?>> TYPES =
            DeferredRegister.create(Siegeworks.MOD_ID, Registries.RECIPE_TYPE);
    public static final RegistrySupplier<net.minecraft.world.item.crafting.RecipeType<me.mss1r.siegeworks.item.crafting.CastingRecipe>> CASTING =
            TYPES.register("casting", () -> new net.minecraft.world.item.crafting.RecipeType<>() {
                @Override public String toString() { return "siegeworks:casting"; }
            });
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Siegeworks.MOD_ID, Registries.RECIPE_SERIALIZER);

    public static final RegistrySupplier<RecipeSerializer<SiegeLadderExtensionRecipe>> SIEGE_LADDER_EXTENSION =
            SERIALIZERS.register("siege_ladder_extension",
                    () -> new SimpleCraftingRecipeSerializer<>(SiegeLadderExtensionRecipe::new));
    public static final RegistrySupplier<RecipeSerializer<SiegeManualRecipe>> SIEGE_MANUAL =
            SERIALIZERS.register("siege_manual", () -> new SimpleCraftingRecipeSerializer<>(SiegeManualRecipe::new));
    public static final RegistrySupplier<RecipeSerializer<me.mss1r.siegeworks.item.crafting.CastingRecipe>> CASTING_SERIALIZER =
            SERIALIZERS.register("casting", me.mss1r.siegeworks.item.crafting.CastingRecipe.Serializer::new);

    private SiegeworksRecipes() {
    }

    public static void register() {
        TYPES.register();
        SERIALIZERS.register();
    }
}
