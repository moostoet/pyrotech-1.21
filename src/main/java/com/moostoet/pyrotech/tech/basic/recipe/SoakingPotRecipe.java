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

import java.util.Optional;

/**
 * A soaking pot recipe: an item soaks in {@code amount} of a fluid for {@code time} ticks,
 * over a lit campfire when the recipe asks for one. The fluid matches by kind, as the 1.12
 * {@code isFluidEqual} did; the amount is what each item drains when it finishes.
 */
public final class SoakingPotRecipe implements Recipe<SoakingPotRecipeInput> {

    private final Ingredient ingredient;
    private final SizedFluidIngredient fluid;
    private final ItemStack result;
    private final boolean campfireRequired;
    private final int time;

    public SoakingPotRecipe(Ingredient ingredient, SizedFluidIngredient fluid, ItemStack result, boolean campfireRequired, int time) {
        this.ingredient = ingredient;
        this.fluid = fluid;
        this.result = result;
        this.campfireRequired = campfireRequired;
        this.time = time;
    }

    public static Optional<RecipeHolder<SoakingPotRecipe>> find(Level level, ItemStack stack, FluidStack fluid) {
        if (stack.isEmpty() || fluid.isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager().getRecipeFor(TechBasicRecipeTypes.SOAKING_POT.get(), new SoakingPotRecipeInput(stack, fluid), level);
    }

    public Ingredient ingredient() {
        return this.ingredient;
    }

    public SizedFluidIngredient fluid() {
        return this.fluid;
    }

    public ItemStack result() {
        return this.result;
    }

    public boolean campfireRequired() {
        return this.campfireRequired;
    }

    public int time() {
        return this.time;
    }

    @Override
    public boolean matches(SoakingPotRecipeInput input, Level level) {
        return this.ingredient.test(input.item()) && this.fluid.ingredient().test(input.fluid());
    }

    @Override
    public ItemStack assemble(SoakingPotRecipeInput input, HolderLookup.Provider registries) {
        return this.result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, this.ingredient);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TechBasicRecipeSerializers.SOAKING_POT.get();
    }

    @Override
    public RecipeType<?> getType() {
        return TechBasicRecipeTypes.SOAKING_POT.get();
    }

    static RecipeSerializer<SoakingPotRecipe> serializer() {
        MapCodec<SoakingPotRecipe> codec = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(SoakingPotRecipe::ingredient),
            SizedFluidIngredient.FLAT_CODEC.fieldOf("fluid").forGetter(SoakingPotRecipe::fluid),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(SoakingPotRecipe::result),
            Codec.BOOL.optionalFieldOf("campfire_required", false).forGetter(SoakingPotRecipe::campfireRequired),
            Codec.INT.fieldOf("time").forGetter(SoakingPotRecipe::time)
        ).apply(instance, SoakingPotRecipe::new));
        StreamCodec<RegistryFriendlyByteBuf, SoakingPotRecipe> streamCodec = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, SoakingPotRecipe::ingredient,
            SizedFluidIngredient.STREAM_CODEC, SoakingPotRecipe::fluid,
            ItemStack.STREAM_CODEC, SoakingPotRecipe::result,
            ByteBufCodecs.BOOL, SoakingPotRecipe::campfireRequired,
            ByteBufCodecs.VAR_INT, SoakingPotRecipe::time,
            SoakingPotRecipe::new);
        return new SimpleRecipeSerializer<>(codec, streamCodec);
    }
}
