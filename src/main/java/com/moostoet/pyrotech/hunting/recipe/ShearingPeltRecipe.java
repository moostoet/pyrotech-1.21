package com.moostoet.pyrotech.hunting.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.moostoet.pyrotech.core.item.CraftingRemainders;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * The 1.12 {@code ShearingPeltRecipe} and its llama twin as one serializer: a pelt and a
 * pair of shears give the wool, the shears take one damage, and the pelt's slot keeps the
 * sheared hide. The hide is a field because the same pelts are consumed whole by scraping.
 */
public final class ShearingPeltRecipe implements CraftingRecipe {

    private final Ingredient pelt;
    private final Ingredient tool;
    private final ItemStack hide;
    private final ItemStack result;

    public ShearingPeltRecipe(Ingredient pelt, Ingredient tool, ItemStack hide, ItemStack result) {
        this.pelt = pelt;
        this.tool = tool;
        this.hide = hide;
        this.result = result;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.ingredientCount() != 2) {
            return false;
        }
        boolean peltFound = false;
        boolean toolFound = false;
        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) {
                continue;
            }
            if (!peltFound && this.pelt.test(stack)) {
                peltFound = true;
            } else if (!toolFound && this.tool.test(stack)) {
                toolFound = true;
            } else {
                return false;
            }
        }
        return peltFound && toolFound;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return this.result.copy();
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int i = 0; i < remaining.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (this.tool.test(stack)) {
                remaining.set(i, CraftingRemainders.damaged(stack, 1));
            } else if (this.pelt.test(stack)) {
                remaining.set(i, this.hide.copy());
            } else if (stack.hasCraftingRemainingItem()) {
                remaining.set(i, stack.getCraftingRemainingItem());
            }
        }
        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, this.pelt, this.tool);
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return HuntingRecipeSerializers.SHEARING_PELT.get();
    }

    public static final class Serializer implements RecipeSerializer<ShearingPeltRecipe> {

        private static final MapCodec<ShearingPeltRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("pelt").forGetter(recipe -> recipe.pelt),
            Ingredient.CODEC_NONEMPTY.fieldOf("tool").forGetter(recipe -> recipe.tool),
            ItemStack.STRICT_CODEC.fieldOf("hide").forGetter(recipe -> recipe.hide),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.result)
        ).apply(instance, ShearingPeltRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, ShearingPeltRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.pelt,
            Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.tool,
            ItemStack.STREAM_CODEC, recipe -> recipe.hide,
            ItemStack.STREAM_CODEC, recipe -> recipe.result,
            ShearingPeltRecipe::new);

        @Override
        public MapCodec<ShearingPeltRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ShearingPeltRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
