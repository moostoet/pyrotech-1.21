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

import java.util.Optional;

/**
 * A tanning rack recipe: the input dries into the result over {@code time} day ticks
 * under the sky, or spoils into the rain failure item, when there is one, after enough
 * rain. The time is the plain 1.12 number; the block entity applies its multiplier.
 */
public final class TanningRackRecipe extends SingleIngredientRecipe {

    private final Optional<ItemStack> rainFailure;
    private final int time;

    public TanningRackRecipe(Ingredient ingredient, ItemStack result, Optional<ItemStack> rainFailure, int time) {
        super(ingredient, result);
        this.rainFailure = rainFailure;
        this.time = time;
    }

    public Optional<ItemStack> rainFailure() {
        return this.rainFailure;
    }

    public int time() {
        return this.time;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TechBasicRecipeSerializers.TANNING_RACK.get();
    }

    @Override
    public RecipeType<?> getType() {
        return TechBasicRecipeTypes.TANNING_RACK.get();
    }

    static RecipeSerializer<TanningRackRecipe> serializer() {
        MapCodec<TanningRackRecipe> codec = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(SingleIngredientRecipe::ingredient),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(SingleIngredientRecipe::result),
            ItemStack.STRICT_CODEC.optionalFieldOf("rain_failure").forGetter(TanningRackRecipe::rainFailure),
            Codec.INT.fieldOf("time").forGetter(TanningRackRecipe::time)
        ).apply(instance, TanningRackRecipe::new));
        StreamCodec<RegistryFriendlyByteBuf, TanningRackRecipe> streamCodec = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, SingleIngredientRecipe::ingredient,
            ItemStack.STREAM_CODEC, SingleIngredientRecipe::result,
            ByteBufCodecs.optional(ItemStack.STREAM_CODEC), TanningRackRecipe::rainFailure,
            ByteBufCodecs.VAR_INT, TanningRackRecipe::time,
            TanningRackRecipe::new);
        return new SimpleRecipeSerializer<>(codec, streamCodec);
    }
}
