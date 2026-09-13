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

/**
 * A compost bin recipe: the compost value an item adds and what the bin makes of it
 * (tech/basic sign-off, item 3). A food without a recipe gets one at runtime from its
 * hunger and saturation; see {@link CompostValues}.
 */
public final class CompostBinRecipe extends SingleIngredientRecipe {

    private final int value;

    public CompostBinRecipe(Ingredient ingredient, int value, ItemStack result) {
        super(ingredient, result);
        this.value = value;
    }

    public int value() {
        return this.value;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TechBasicRecipeSerializers.COMPOST_BIN.get();
    }

    @Override
    public RecipeType<?> getType() {
        return TechBasicRecipeTypes.COMPOST_BIN.get();
    }

    static RecipeSerializer<CompostBinRecipe> serializer() {
        MapCodec<CompostBinRecipe> codec = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(SingleIngredientRecipe::ingredient),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("value").forGetter(CompostBinRecipe::value),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(SingleIngredientRecipe::result)
        ).apply(instance, CompostBinRecipe::new));
        StreamCodec<RegistryFriendlyByteBuf, CompostBinRecipe> streamCodec = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, SingleIngredientRecipe::ingredient,
            ByteBufCodecs.VAR_INT, CompostBinRecipe::value,
            ItemStack.STREAM_CODEC, SingleIngredientRecipe::result,
            CompostBinRecipe::new);
        return new SimpleRecipeSerializer<>(codec, streamCodec);
    }
}
