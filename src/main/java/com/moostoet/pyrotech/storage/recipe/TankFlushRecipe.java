package com.moostoet.pyrotech.storage.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
 * The 1.12 core {@code TankFlushRecipe}, rehomed: a tank alone in the grid comes out empty.
 * It stays a custom recipe because a plain shapeless one would hand the drained tank back
 * through the crafting remainder and duplicate it.
 */
public final class TankFlushRecipe implements CraftingRecipe {

    private final Ingredient tank;

    public TankFlushRecipe(Ingredient tank) {
        this.tank = tank;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return input.ingredientCount() == 1 && this.tank.test(input.items().stream().filter(stack -> !stack.isEmpty()).findFirst().orElse(ItemStack.EMPTY));
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        for (ItemStack stack : input.items()) {
            if (this.tank.test(stack)) {
                return new ItemStack(stack.getItem());
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        return NonNullList.withSize(input.size(), ItemStack.EMPTY);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        ItemStack[] tanks = this.tank.getItems();
        return tanks.length == 0 ? ItemStack.EMPTY : new ItemStack(tanks[0].getItem());
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, this.tank);
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return StorageRecipeSerializers.TANK_FLUSH.get();
    }

    public static final class Serializer implements RecipeSerializer<TankFlushRecipe> {

        private static final MapCodec<TankFlushRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("tank").forGetter(recipe -> recipe.tank)
        ).apply(instance, TankFlushRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, TankFlushRecipe> STREAM_CODEC =
            Ingredient.CONTENTS_STREAM_CODEC.map(TankFlushRecipe::new, recipe -> recipe.tank);

        @Override
        public MapCodec<TankFlushRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, TankFlushRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
