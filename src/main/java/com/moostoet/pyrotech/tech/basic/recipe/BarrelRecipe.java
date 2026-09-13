package com.moostoet.pyrotech.tech.basic.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.util.List;
import java.util.Optional;

/**
 * A barrel recipe: four items in a fluid become a new fluid after {@code time} sealed
 * ticks. The items match in any order, each ingredient once, as the 1.12 bitmask did;
 * the fluid matches by kind. The barrel takes an item only when some recipe of its fluid
 * lists it.
 */
public final class BarrelRecipe implements Recipe<BarrelRecipeInput> {

    public static final int SLOTS = 4;

    private final List<Ingredient> ingredients;
    private final SizedFluidIngredient fluid;
    private final FluidStack result;
    private final int time;

    public BarrelRecipe(List<Ingredient> ingredients, SizedFluidIngredient fluid, FluidStack result, int time) {
        this.ingredients = ingredients;
        this.fluid = fluid;
        this.result = result;
        this.time = time;
    }

    public static Optional<RecipeHolder<BarrelRecipe>> find(Level level, List<ItemStack> items, FluidStack fluid) {
        if (fluid.isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager().getRecipeFor(TechBasicRecipeTypes.BARREL.get(), new BarrelRecipeInput(items, fluid), level);
    }

    /** The 1.12 {@code isValidItem}: some recipe of this fluid lists the item. */
    public static boolean isValidItem(Level level, ItemStack stack, FluidStack fluid) {
        if (stack.isEmpty() || fluid.isEmpty()) {
            return false;
        }
        for (RecipeHolder<BarrelRecipe> holder : level.getRecipeManager().getAllRecipesFor(TechBasicRecipeTypes.BARREL.get())) {
            BarrelRecipe recipe = holder.value();
            if (recipe.fluid.ingredient().test(fluid) && recipe.ingredients.stream().anyMatch(ingredient -> ingredient.test(stack))) {
                return true;
            }
        }
        return false;
    }

    public List<Ingredient> ingredients() {
        return this.ingredients;
    }

    public SizedFluidIngredient fluid() {
        return this.fluid;
    }

    public FluidStack result() {
        return this.result;
    }

    public int time() {
        return this.time;
    }

    @Override
    public boolean matches(BarrelRecipeInput input, Level level) {
        if (!this.fluid.ingredient().test(input.fluid())) {
            return false;
        }
        int used = 0;
        int matched = 0;
        outer:
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            for (int j = 0; j < this.ingredients.size(); j++) {
                if ((used & (1 << j)) != 0) {
                    continue;
                }
                if (this.ingredients.get(j).test(stack)) {
                    used |= 1 << j;
                    matched++;
                    continue outer;
                }
            }
            return false;
        }
        return matched == this.ingredients.size();
    }

    @Override
    public ItemStack assemble(BarrelRecipeInput input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.addAll(this.ingredients);
        return list;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TechBasicRecipeSerializers.BARREL.get();
    }

    @Override
    public RecipeType<?> getType() {
        return TechBasicRecipeTypes.BARREL.get();
    }

    static RecipeSerializer<BarrelRecipe> serializer() {
        MapCodec<BarrelRecipe> codec = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.listOf(1, SLOTS).fieldOf("ingredients").forGetter(BarrelRecipe::ingredients),
            SizedFluidIngredient.FLAT_CODEC.fieldOf("fluid").forGetter(BarrelRecipe::fluid),
            FluidStack.CODEC.fieldOf("result").forGetter(BarrelRecipe::result),
            Codec.INT.fieldOf("time").forGetter(BarrelRecipe::time)
        ).apply(instance, BarrelRecipe::new));
        StreamCodec<RegistryFriendlyByteBuf, BarrelRecipe> streamCodec = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), BarrelRecipe::ingredients,
            SizedFluidIngredient.STREAM_CODEC, BarrelRecipe::fluid,
            FluidStack.STREAM_CODEC, BarrelRecipe::result,
            ByteBufCodecs.VAR_INT, BarrelRecipe::time,
            BarrelRecipe::new);
        return new SimpleRecipeSerializer<>(codec, streamCodec);
    }
}
