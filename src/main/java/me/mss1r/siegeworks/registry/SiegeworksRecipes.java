package me.mss1r.siegeworks.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.item.crafting.SiegeLadderExtensionRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;

public final class SiegeworksRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Siegeworks.MOD_ID, Registries.RECIPE_SERIALIZER);

    public static final RegistrySupplier<RecipeSerializer<SiegeLadderExtensionRecipe>> SIEGE_LADDER_EXTENSION =
            SERIALIZERS.register("siege_ladder_extension",
                    () -> new SimpleCraftingRecipeSerializer<>(SiegeLadderExtensionRecipe::new));

    private SiegeworksRecipes() {
    }

    public static void register() {
        SERIALIZERS.register();
    }
}
