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

import java.util.List;

/**
 * A chopping block recipe. The chops an axe needs and the planks it yields depend on the
 * axe's tool level; a recipe may carry its own tables, else the 1.12 defaults apply
 * (recipe architecture sign-off, item 4). A level past a table's end uses its last entry.
 */
public final class ChoppingBlockRecipe extends SingleIngredientRecipe {

    public static final List<Integer> DEFAULT_CHOPS = List.of(6, 4, 2, 2);
    public static final List<Integer> DEFAULT_QUANTITIES = List.of(1, 2, 3, 4);

    private final List<Integer> chops;
    private final List<Integer> quantities;

    public ChoppingBlockRecipe(Ingredient ingredient, ItemStack result, List<Integer> chops, List<Integer> quantities) {
        super(ingredient, result.copyWithCount(1));
        this.chops = chops;
        this.quantities = quantities;
    }

    public List<Integer> chops() {
        return this.chops;
    }

    public List<Integer> quantities() {
        return this.quantities;
    }

    public int chops(int toolLevel) {
        return atLevel(this.chops, toolLevel);
    }

    public int quantity(int toolLevel) {
        return atLevel(this.quantities, toolLevel);
    }

    /** The 1.12 {@code ArrayHelper.getOrLast}. */
    public static int atLevel(List<Integer> table, int toolLevel) {
        return table.get(Math.min(Math.max(toolLevel, 0), table.size() - 1));
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TechBasicRecipeSerializers.CHOPPING_BLOCK.get();
    }

    @Override
    public RecipeType<?> getType() {
        return TechBasicRecipeTypes.CHOPPING_BLOCK.get();
    }

    static RecipeSerializer<ChoppingBlockRecipe> serializer() {
        MapCodec<ChoppingBlockRecipe> codec = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(SingleIngredientRecipe::ingredient),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(SingleIngredientRecipe::result),
            Codec.INT.listOf(1, Integer.MAX_VALUE).optionalFieldOf("chops", DEFAULT_CHOPS).forGetter(ChoppingBlockRecipe::chops),
            Codec.INT.listOf(1, Integer.MAX_VALUE).optionalFieldOf("quantities", DEFAULT_QUANTITIES).forGetter(ChoppingBlockRecipe::quantities)
        ).apply(instance, ChoppingBlockRecipe::new));
        StreamCodec<RegistryFriendlyByteBuf, ChoppingBlockRecipe> streamCodec = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, SingleIngredientRecipe::ingredient,
            ItemStack.STREAM_CODEC, SingleIngredientRecipe::result,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), ChoppingBlockRecipe::chops,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), ChoppingBlockRecipe::quantities,
            ChoppingBlockRecipe::new);
        return new SimpleRecipeSerializer<>(codec, streamCodec);
    }
}
