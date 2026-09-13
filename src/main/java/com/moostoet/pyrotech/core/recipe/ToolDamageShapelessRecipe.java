package com.moostoet.pyrotech.core.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.moostoet.pyrotech.core.item.CraftingRemainders;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.List;

/**
 * A shapeless recipe with one tool in the grid that is damaged instead of consumed: the
 * 1.12 marshmallow stick, chopping block, hunter's knife, and shearing recipes as one
 * serializer, {@code pyrotech:tool_damage_shapeless} (hunting sign-off, item 3). The
 * {@code tool} ingredient is one more grid input beside {@code ingredients}; every stack
 * in the grid it matches takes {@code tool_damage} and vanishes when that breaks it.
 */
public final class ToolDamageShapelessRecipe extends ShapelessRecipe {

    private final ItemStack result;
    private final List<Ingredient> inputs;
    private final Ingredient tool;
    private final int toolDamage;

    public ToolDamageShapelessRecipe(String group, CraftingBookCategory category, ItemStack result,
                                     List<Ingredient> inputs, Ingredient tool, int toolDamage) {
        super(group, category, result, withTool(inputs, tool));
        this.result = result;
        this.inputs = inputs;
        this.tool = tool;
        this.toolDamage = toolDamage;
    }

    private static NonNullList<Ingredient> withTool(List<Ingredient> inputs, Ingredient tool) {
        NonNullList<Ingredient> all = NonNullList.createWithCapacity(inputs.size() + 1);
        all.addAll(inputs);
        all.add(tool);
        return all;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return CoreRecipeSerializers.TOOL_DAMAGE_SHAPELESS.get();
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int i = 0; i < remaining.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (this.tool.test(stack)) {
                remaining.set(i, CraftingRemainders.damaged(stack, this.toolDamage));
            } else if (stack.hasCraftingRemainingItem()) {
                remaining.set(i, stack.getCraftingRemainingItem());
            }
        }
        return remaining;
    }

    public static final class Serializer implements RecipeSerializer<ToolDamageShapelessRecipe> {

        private static final MapCodec<ToolDamageShapelessRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(ShapelessRecipe::getGroup),
            CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(ShapelessRecipe::category),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
            Ingredient.LIST_CODEC_NONEMPTY.fieldOf("ingredients").forGetter(recipe -> recipe.inputs),
            Ingredient.CODEC_NONEMPTY.fieldOf("tool").forGetter(recipe -> recipe.tool),
            Codec.INT.optionalFieldOf("tool_damage", 1).forGetter(recipe -> recipe.toolDamage)
        ).apply(instance, ToolDamageShapelessRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, ToolDamageShapelessRecipe> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ShapelessRecipe::getGroup,
            CraftingBookCategory.STREAM_CODEC, ShapelessRecipe::category,
            ItemStack.STREAM_CODEC, recipe -> recipe.result,
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), recipe -> recipe.inputs,
            Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.tool,
            ByteBufCodecs.VAR_INT, recipe -> recipe.toolDamage,
            ToolDamageShapelessRecipe::new);

        @Override
        public MapCodec<ToolDamageShapelessRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ToolDamageShapelessRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
