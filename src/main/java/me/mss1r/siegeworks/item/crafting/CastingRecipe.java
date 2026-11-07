package me.mss1r.siegeworks.item.crafting;

import me.mss1r.siegeworks.gameplay.ammunition.RoundShotSize;
import me.mss1r.siegeworks.item.RoundShotItem;
import me.mss1r.siegeworks.registry.SiegeworksRecipes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
//? if <1.21 {
/*import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.Container;
*///?} else {
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.Codec;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
//?}

//? if <1.21 {
/*public final class CastingRecipe implements Recipe<Container> {
    private ResourceLocation id;
*///?} else {
public final class CastingRecipe implements Recipe<SingleRecipeInput> {
//?}
    public final Ingredient ingredient;
    public final Item result;
    public final int referenceCaliber, referenceCost, maxBatch;

    public CastingRecipe(Ingredient ingredient, Item result, int referenceCaliber, int referenceCost, int maxBatch) {
        if (ingredient.isEmpty() || !(result instanceof RoundShotItem shot)
                || !(shot.getBlock() instanceof me.mss1r.siegeworks.block.CaliberShotBlock)
                || !RoundShotSize.valid(referenceCaliber) || referenceCost < 1 || referenceCost > 64 || maxBatch < 1 || maxBatch > 64) {
            throw new IllegalArgumentException("Invalid casting recipe");
        }
        this.ingredient = ingredient; this.result = result;
        this.referenceCaliber = referenceCaliber; this.referenceCost = referenceCost; this.maxBatch = maxBatch;
    }

    public record Batch(int cost, int count) {}

    public Batch batch(int caliber) {
        if (!RoundShotSize.valid(caliber)) throw new IllegalArgumentException("Invalid caliber");
        long referenceVolume = (long) referenceCaliber * referenceCaliber * referenceCaliber;
        long requiredVolume = (long) caliber * caliber * caliber * referenceCost;
        if (requiredVolume >= referenceVolume) return new Batch((int) ((requiredVolume + referenceVolume - 1) / referenceVolume), 1);
        return new Batch(1, (int) Math.min(maxBatch, referenceVolume / requiredVolume));
    }

    public ItemStack output(int caliber) {
        return RoundShotItem.withCaliber(new ItemStack(result, batch(caliber).count()), caliber);
    }

    //? if <1.21 {
    /*@Override public ResourceLocation getId() { return id; }
    @Override public boolean matches(Container input, Level level) { return input.getContainerSize() > 0 && ingredient.test(input.getItem(0)); }
    @Override public ItemStack assemble(Container input, RegistryAccess registries) { return output(referenceCaliber); }
    @Override public ItemStack getResultItem(RegistryAccess registries) { return output(referenceCaliber); }
    *///?} else {
    @Override public boolean matches(SingleRecipeInput input, Level level) { return ingredient.test(input.item()); }
    @Override public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) { return output(referenceCaliber); }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return output(referenceCaliber); }
    //?}
    @Override public boolean canCraftInDimensions(int width, int height) { return width * height > 0; }
    @Override public boolean isSpecial() { return true; }
    @Override public RecipeSerializer<?> getSerializer() { return SiegeworksRecipes.CASTING_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return SiegeworksRecipes.CASTING.get(); }

    public static final class Serializer implements RecipeSerializer<CastingRecipe> {
        //? if <1.21 {
        /*@Override public CastingRecipe fromJson(ResourceLocation id, JsonObject json) {
            CastingRecipe recipe = new CastingRecipe(Ingredient.fromJson(json.get("ingredient")),
                    BuiltInRegistries.ITEM.get(new ResourceLocation(GsonHelper.getAsString(json, "result"))),
                    GsonHelper.getAsInt(json, "reference_caliber"), GsonHelper.getAsInt(json, "reference_cost"), GsonHelper.getAsInt(json, "max_batch"));
            recipe.id = id; return recipe;
        }
        @Override public CastingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            CastingRecipe recipe = new CastingRecipe(Ingredient.fromNetwork(buf), buf.readById(BuiltInRegistries.ITEM),
                    buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
            recipe.id = id; return recipe;
        }
        @Override public void toNetwork(FriendlyByteBuf buf, CastingRecipe recipe) {
            recipe.ingredient.toNetwork(buf); buf.writeId(BuiltInRegistries.ITEM, recipe.result);
            buf.writeVarInt(recipe.referenceCaliber); buf.writeVarInt(recipe.referenceCost); buf.writeVarInt(recipe.maxBatch);
        }
        *///?} else {
        private static final MapCodec<CastingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(r -> r.ingredient),
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("result").forGetter(r -> r.result),
                Codec.INT.fieldOf("reference_caliber").forGetter(r -> r.referenceCaliber),
                Codec.INT.fieldOf("reference_cost").forGetter(r -> r.referenceCost),
                Codec.INT.fieldOf("max_batch").forGetter(r -> r.maxBatch)).apply(i, CastingRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, CastingRecipe> STREAM = StreamCodec.of(
                (buf, r) -> {
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buf, r.ingredient); buf.writeVarInt(BuiltInRegistries.ITEM.getId(r.result));
                    buf.writeVarInt(r.referenceCaliber); buf.writeVarInt(r.referenceCost); buf.writeVarInt(r.maxBatch);
                }, buf -> new CastingRecipe(Ingredient.CONTENTS_STREAM_CODEC.decode(buf), BuiltInRegistries.ITEM.byId(buf.readVarInt()),
                        buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
        @Override public MapCodec<CastingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, CastingRecipe> streamCodec() { return STREAM; }
        //?}
    }
}
