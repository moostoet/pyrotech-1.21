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
 * An explicit campfire recipe, checked before the derived smelting-with-food list
 * (tech/basic sign-off, item 4). The Patchouli book and datapacks use it; the cook time
 * is the recipe's own, not the campfire's.
 */
public final class CampfireRecipe extends SingleIngredientRecipe {

    private final int ticks;

    public CampfireRecipe(Ingredient ingredient, ItemStack result, int ticks) {
        super(ingredient, result);
        this.ticks = ticks;
    }

    public int ticks() {
        return this.ticks;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TechBasicRecipeSerializers.CAMPFIRE.get();
    }

    @Override
    public RecipeType<?> getType() {
        return TechBasicRecipeTypes.CAMPFIRE.get();
    }

    static RecipeSerializer<CampfireRecipe> serializer() {
        MapCodec<CampfireRecipe> codec = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(SingleIngredientRecipe::ingredient),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(SingleIngredientRecipe::result),
            Codec.INT.fieldOf("ticks").forGetter(CampfireRecipe::ticks)
        ).apply(instance, CampfireRecipe::new));
        StreamCodec<RegistryFriendlyByteBuf, CampfireRecipe> streamCodec = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, SingleIngredientRecipe::ingredient,
            ItemStack.STREAM_CODEC, SingleIngredientRecipe::result,
            ByteBufCodecs.VAR_INT, CampfireRecipe::ticks,
            CampfireRecipe::new);
        return new SimpleRecipeSerializer<>(codec, streamCodec);
    }
}
