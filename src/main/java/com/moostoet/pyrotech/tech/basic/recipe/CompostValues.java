package com.moostoet.pyrotech.tech.basic.recipe;

import com.moostoet.pyrotech.core.CoreItems;
import com.moostoet.pyrotech.tech.basic.TechBasicConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * What an item composts into: its explicit recipe, else, while the toggle is on, the
 * 1.12 rule that values any food by its hunger and saturation between 1 and 8 and
 * outputs four mulch (tech/basic sign-off, item 3).
 */
public final class CompostValues {

    public static final int MULCH_PER_RECIPE = 4;
    private static final int MIN_FOOD_VALUE = 1;
    private static final int MAX_FOOD_VALUE = 8;

    private CompostValues() {
    }

    /** The compost value an item adds and the result it makes. */
    public record Entry(int value, ItemStack result) {

        public boolean sameResult(ItemStack other) {
            return ItemStack.isSameItemSameComponents(this.result, other);
        }
    }

    public static Optional<Entry> of(Level level, ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        Optional<RecipeHolder<CompostBinRecipe>> recipe = level.getRecipeManager()
            .getRecipeFor(TechBasicRecipeTypes.COMPOST_BIN.get(), new SingleRecipeInput(stack), level);
        if (recipe.isPresent()) {
            return Optional.of(new Entry(recipe.get().value().value(), recipe.get().value().result()));
        }
        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food == null || !TechBasicConfig.COMMON.autoCreateRecipesFromFood.get()) {
            return Optional.empty();
        }
        return Optional.of(new Entry(foodValue(food), new ItemStack(CoreItems.MULCH.get(), MULCH_PER_RECIPE)));
    }

    /**
     * The 1.12 {@code calculateCompostValue}: the hunger and the saturation, each as a
     * share of a full stomach, averaged and stretched over the range. A 1.21 food already
     * carries its saturation as hunger times modifier times two.
     */
    public static int foodValue(FoodProperties food) {
        float hunger = Mth.clamp(food.nutrition() / 20f, 0, 1);
        float saturation = Mth.clamp(food.saturation() / 20f, 0, 1);
        float scalar = (saturation + hunger) * 0.5f;
        return Mth.clamp((int) ((MAX_FOOD_VALUE - MIN_FOOD_VALUE) * scalar + MIN_FOOD_VALUE), MIN_FOOD_VALUE, MAX_FOOD_VALUE);
    }
}
