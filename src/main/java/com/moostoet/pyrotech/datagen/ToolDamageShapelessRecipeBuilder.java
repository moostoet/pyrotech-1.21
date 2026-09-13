package com.moostoet.pyrotech.datagen;

import com.moostoet.pyrotech.core.recipe.ToolDamageShapelessRecipe;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** The datagen builder for {@link ToolDamageShapelessRecipe}, shaped like vanilla's shapeless builder. */
public final class ToolDamageShapelessRecipeBuilder implements RecipeBuilder {

    private final RecipeCategory category;
    private final ItemStack result;
    private final Ingredient tool;
    private final int toolDamage;
    private final List<Ingredient> ingredients = new ArrayList<>();
    private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();
    @Nullable
    private String group;

    private ToolDamageShapelessRecipeBuilder(RecipeCategory category, ItemStack result, Ingredient tool, int toolDamage) {
        this.category = category;
        this.result = result;
        this.tool = tool;
        this.toolDamage = toolDamage;
    }

    public static ToolDamageShapelessRecipeBuilder toolDamageShapeless(RecipeCategory category, ItemLike result, int count,
                                                                       Ingredient tool, int toolDamage) {
        return new ToolDamageShapelessRecipeBuilder(category, new ItemStack(result, count), tool, toolDamage);
    }

    public ToolDamageShapelessRecipeBuilder requires(TagKey<Item> tag) {
        return this.requires(Ingredient.of(tag));
    }

    public ToolDamageShapelessRecipeBuilder requires(ItemLike item) {
        return this.requires(Ingredient.of(item));
    }

    public ToolDamageShapelessRecipeBuilder requires(Ingredient ingredient) {
        this.ingredients.add(ingredient);
        return this;
    }

    @Override
    public ToolDamageShapelessRecipeBuilder unlockedBy(String name, Criterion<?> criterion) {
        this.criteria.put(name, criterion);
        return this;
    }

    @Override
    public ToolDamageShapelessRecipeBuilder group(@Nullable String groupName) {
        this.group = groupName;
        return this;
    }

    @Override
    public Item getResult() {
        return this.result.getItem();
    }

    @Override
    public void save(RecipeOutput recipeOutput, ResourceLocation id) {
        if (this.criteria.isEmpty()) {
            throw new IllegalStateException("No way of obtaining recipe " + id);
        }
        Advancement.Builder advancement = recipeOutput.advancement()
            .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id))
            .rewards(AdvancementRewards.Builder.recipe(id))
            .requirements(AdvancementRequirements.Strategy.OR);
        this.criteria.forEach(advancement::addCriterion);
        ToolDamageShapelessRecipe recipe = new ToolDamageShapelessRecipe(
            Objects.requireNonNullElse(this.group, ""),
            RecipeBuilder.determineBookCategory(this.category),
            this.result,
            List.copyOf(this.ingredients),
            this.tool,
            this.toolDamage);
        recipeOutput.accept(id, recipe, advancement.build(id.withPrefix("recipes/" + this.category.getFolderName() + "/")));
    }
}
