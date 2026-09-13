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

import java.util.List;
import java.util.Optional;

/**
 * An anvil recipe: the item, the hammer or pickaxe, the hits, and the tiers it runs on.
 * The {@code pyrotech:anvil} type takes a second serializer from bloomery for the bloom,
 * so the anvil block entity only ever sees this interface and {@link ExtendedAnvilRecipe}.
 */
public class AnvilRecipe implements Recipe<AnvilRecipeInput> {

    private final Ingredient ingredient;
    private final ItemStack result;
    private final int hits;
    private final AnvilToolType tool;
    private final List<AnvilTier> tiers;

    public AnvilRecipe(Ingredient ingredient, ItemStack result, int hits, AnvilToolType tool, List<AnvilTier> tiers) {
        this.ingredient = ingredient;
        this.result = result;
        this.hits = hits;
        this.tool = tool;
        this.tiers = tiers;
    }

    public Ingredient ingredient() {
        return this.ingredient;
    }

    public ItemStack result() {
        return this.result;
    }

    public int hits() {
        return this.hits;
    }

    public AnvilToolType tool() {
        return this.tool;
    }

    public List<AnvilTier> tiers() {
        return this.tiers;
    }

    /** True when any anvil recipe of the tier takes the item, whatever the tool. */
    public static boolean hasRecipe(Level level, ItemStack stack, AnvilTier tier) {
        return find(level, stack, tier, null).isPresent();
    }

    public static Optional<RecipeHolder<AnvilRecipe>> find(Level level, ItemStack stack, AnvilTier tier, AnvilToolType tool) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager().getRecipeFor(TechBasicRecipeTypes.ANVIL.get(), new AnvilRecipeInput(stack, tier, tool), level);
    }

    @Override
    public boolean matches(AnvilRecipeInput input, Level level) {
        return this.tiers.contains(input.tier())
            && this.ingredient.test(input.item())
            && (input.tool() == null || input.tool() == this.tool);
    }

    @Override
    public ItemStack assemble(AnvilRecipeInput input, HolderLookup.Provider registries) {
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
        return TechBasicRecipeSerializers.ANVIL.get();
    }

    @Override
    public RecipeType<?> getType() {
        return TechBasicRecipeTypes.ANVIL.get();
    }

    static RecipeSerializer<AnvilRecipe> serializer() {
        MapCodec<AnvilRecipe> codec = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(AnvilRecipe::ingredient),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(AnvilRecipe::result),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("hits").forGetter(AnvilRecipe::hits),
            AnvilToolType.CODEC.fieldOf("tool").forGetter(AnvilRecipe::tool),
            AnvilTier.CODEC.listOf(1, Integer.MAX_VALUE).fieldOf("tiers").forGetter(AnvilRecipe::tiers)
        ).apply(instance, AnvilRecipe::new));
        StreamCodec<RegistryFriendlyByteBuf, AnvilRecipe> streamCodec = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, AnvilRecipe::ingredient,
            ItemStack.STREAM_CODEC, AnvilRecipe::result,
            ByteBufCodecs.VAR_INT, AnvilRecipe::hits,
            AnvilToolType.STREAM_CODEC, AnvilRecipe::tool,
            AnvilTier.STREAM_CODEC.apply(ByteBufCodecs.list()), AnvilRecipe::tiers,
            AnvilRecipe::new);
        return new SimpleRecipeSerializer<>(codec, streamCodec);
    }
}
