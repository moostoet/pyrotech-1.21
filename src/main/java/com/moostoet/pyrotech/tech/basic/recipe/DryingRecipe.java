package com.moostoet.pyrotech.tech.basic.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.function.Supplier;

/**
 * A drying recipe: the crude drying rack's and the drying rack's, and the two ovens' once
 * machine hoists. The dry time is the plain 1.12 number; the block entity applies its
 * duration multiplier.
 */
public final class DryingRecipe extends SingleIngredientRecipe {

    private final int dryTime;
    private final Supplier<RecipeType<DryingRecipe>> type;
    private final Supplier<RecipeSerializer<DryingRecipe>> serializer;

    public DryingRecipe(Ingredient ingredient, ItemStack result, int dryTime,
                        Supplier<RecipeType<DryingRecipe>> type, Supplier<RecipeSerializer<DryingRecipe>> serializer) {
        super(ingredient, result);
        this.dryTime = dryTime;
        this.type = type;
        this.serializer = serializer;
    }

    public int dryTime() {
        return this.dryTime;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return this.serializer.get();
    }

    @Override
    public RecipeType<?> getType() {
        return this.type.get();
    }

    public static RecipeSerializer<DryingRecipe> serializer(Supplier<RecipeType<DryingRecipe>> type, Supplier<RecipeSerializer<DryingRecipe>> self) {
        MapCodec<DryingRecipe> codec = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(SingleIngredientRecipe::ingredient),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(SingleIngredientRecipe::result),
            Codec.INT.fieldOf("dry_time").forGetter(DryingRecipe::dryTime)
        ).apply(instance, (ingredient, result, dryTime) -> new DryingRecipe(ingredient, result, dryTime, type, self)));
        StreamCodec<RegistryFriendlyByteBuf, DryingRecipe> streamCodec = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, SingleIngredientRecipe::ingredient,
            ItemStack.STREAM_CODEC, SingleIngredientRecipe::result,
            ByteBufCodecs.VAR_INT, DryingRecipe::dryTime,
            (ingredient, result, dryTime) -> new DryingRecipe(ingredient, result, dryTime, type, self));
        return new SimpleRecipeSerializer<>(codec, streamCodec);
    }
}
