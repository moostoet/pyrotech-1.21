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
import java.util.function.Supplier;

/**
 * A compacting bin recipe: {@code amount} of the ingredient make one result. The shovel
 * uses per tool level may come with the recipe, else the 1.12 default applies (recipe
 * architecture sign-off, item 4). Machine's mechanical compacting bin reuses the class
 * with its own type.
 */
public final class CompactingBinRecipe extends SingleIngredientRecipe {

    public static final List<Integer> DEFAULT_TOOL_USES = List.of(4, 3, 2, 1);

    private final int amount;
    private final List<Integer> toolUses;
    private final Supplier<RecipeType<CompactingBinRecipe>> type;
    private final Supplier<RecipeSerializer<CompactingBinRecipe>> serializer;

    public CompactingBinRecipe(Ingredient ingredient, ItemStack result, int amount, List<Integer> toolUses,
                               Supplier<RecipeType<CompactingBinRecipe>> type, Supplier<RecipeSerializer<CompactingBinRecipe>> serializer) {
        super(ingredient, result);
        this.amount = amount;
        this.toolUses = toolUses;
        this.type = type;
        this.serializer = serializer;
    }

    public int amount() {
        return this.amount;
    }

    public List<Integer> toolUses() {
        return this.toolUses;
    }

    public int toolUses(int toolLevel) {
        return ChoppingBlockRecipe.atLevel(this.toolUses, toolLevel);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return this.serializer.get();
    }

    @Override
    public RecipeType<?> getType() {
        return this.type.get();
    }

    public static RecipeSerializer<CompactingBinRecipe> serializer(Supplier<RecipeType<CompactingBinRecipe>> type,
                                                                   Supplier<RecipeSerializer<CompactingBinRecipe>> self) {
        MapCodec<CompactingBinRecipe> codec = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(SingleIngredientRecipe::ingredient),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(SingleIngredientRecipe::result),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("amount").forGetter(CompactingBinRecipe::amount),
            Codec.INT.listOf(1, Integer.MAX_VALUE).optionalFieldOf("tool_uses", DEFAULT_TOOL_USES).forGetter(CompactingBinRecipe::toolUses)
        ).apply(instance, (ingredient, result, amount, toolUses) -> new CompactingBinRecipe(ingredient, result, amount, toolUses, type, self)));
        StreamCodec<RegistryFriendlyByteBuf, CompactingBinRecipe> streamCodec = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, SingleIngredientRecipe::ingredient,
            ItemStack.STREAM_CODEC, SingleIngredientRecipe::result,
            ByteBufCodecs.VAR_INT, CompactingBinRecipe::amount,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), CompactingBinRecipe::toolUses,
            (ingredient, result, amount, toolUses) -> new CompactingBinRecipe(ingredient, result, amount, toolUses, type, self));
        return new SimpleRecipeSerializer<>(codec, streamCodec);
    }
}
