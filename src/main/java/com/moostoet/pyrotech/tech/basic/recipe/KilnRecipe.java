package com.moostoet.pyrotech.tech.basic.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;
import java.util.function.Supplier;

/**
 * A kiln recipe: the pit kiln's, and the stone and brick kilns' once machine hoists,
 * since the three had identical fields in 1.12. The burn time is the plain 1.12 number;
 * the block entity applies its duration multiplier. A failed item becomes one of the
 * failure items at random, or pit ash when there are none.
 */
public final class KilnRecipe extends SingleIngredientRecipe {

    private final int burnTime;
    private final float failureChance;
    private final List<ItemStack> failureItems;
    private final Supplier<RecipeType<KilnRecipe>> type;
    private final Supplier<RecipeSerializer<KilnRecipe>> serializer;

    public KilnRecipe(Ingredient ingredient, ItemStack result, int burnTime, float failureChance, List<ItemStack> failureItems,
                      Supplier<RecipeType<KilnRecipe>> type, Supplier<RecipeSerializer<KilnRecipe>> serializer) {
        super(ingredient, result);
        this.burnTime = burnTime;
        this.failureChance = Mth.clamp(failureChance, 0, 1);
        this.failureItems = failureItems;
        this.type = type;
        this.serializer = serializer;
    }

    public int burnTime() {
        return this.burnTime;
    }

    public float failureChance() {
        return this.failureChance;
    }

    public List<ItemStack> failureItems() {
        return this.failureItems;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return this.serializer.get();
    }

    @Override
    public RecipeType<?> getType() {
        return this.type.get();
    }

    public static RecipeSerializer<KilnRecipe> serializer(Supplier<RecipeType<KilnRecipe>> type, Supplier<RecipeSerializer<KilnRecipe>> self) {
        MapCodec<KilnRecipe> codec = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(SingleIngredientRecipe::ingredient),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(SingleIngredientRecipe::result),
            Codec.INT.fieldOf("burn_time").forGetter(KilnRecipe::burnTime),
            Codec.FLOAT.optionalFieldOf("failure_chance", 0f).forGetter(KilnRecipe::failureChance),
            ItemStack.STRICT_CODEC.listOf().optionalFieldOf("failure_items", List.of()).forGetter(KilnRecipe::failureItems)
        ).apply(instance, (ingredient, result, burnTime, failureChance, failureItems) ->
            new KilnRecipe(ingredient, result, burnTime, failureChance, failureItems, type, self)));
        StreamCodec<RegistryFriendlyByteBuf, KilnRecipe> streamCodec = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, SingleIngredientRecipe::ingredient,
            ItemStack.STREAM_CODEC, SingleIngredientRecipe::result,
            ByteBufCodecs.VAR_INT, KilnRecipe::burnTime,
            ByteBufCodecs.FLOAT, KilnRecipe::failureChance,
            ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), KilnRecipe::failureItems,
            (ingredient, result, burnTime, failureChance, failureItems) ->
                new KilnRecipe(ingredient, result, burnTime, failureChance, failureItems, type, self));
        return new SimpleRecipeSerializer<>(codec, streamCodec);
    }
}
