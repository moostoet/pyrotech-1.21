package com.moostoet.pyrotech.tech.basic.block.entity;

import com.moostoet.pyrotech.tech.basic.TechBasicConfig;
import com.moostoet.pyrotech.tech.basic.TechBasicTags;
import com.moostoet.pyrotech.tech.basic.recipe.CampfireRecipe;
import com.moostoet.pyrotech.tech.basic.recipe.TechBasicRecipeTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * The campfire cook list: an explicit {@code pyrotech:campfire} recipe first, else the
 * furnace recipe whose result is a food and not blacklisted, cooked in the campfire's own
 * time (tech/basic sign-off, item 4). The vanilla campfire's own recipes play no part
 * (progression skips sign-off, item 2).
 */
public final class CampfireCookList {

    private CampfireCookList() {
    }

    /** What the campfire makes of an item and how long it takes. */
    public record Cook(ItemStack result, int ticks) {
    }

    public static Optional<Cook> find(Level level, ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        SingleRecipeInput input = new SingleRecipeInput(stack);
        Optional<RecipeHolder<CampfireRecipe>> explicit = level.getRecipeManager().getRecipeFor(TechBasicRecipeTypes.CAMPFIRE.get(), input, level);
        if (explicit.isPresent()) {
            CampfireRecipe recipe = explicit.get().value();
            return Optional.of(new Cook(recipe.result().copy(), recipe.ticks()));
        }
        Optional<RecipeHolder<SmeltingRecipe>> smelting = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, level);
        if (smelting.isEmpty()) {
            return Optional.empty();
        }
        ItemStack result = smelting.get().value().getResultItem(level.registryAccess());
        if (!result.has(DataComponents.FOOD) || result.is(TechBasicTags.Items.CAMPFIRE_BLACKLIST)) {
            return Optional.empty();
        }
        return Optional.of(new Cook(result.copy(), TechBasicConfig.SERVER.campfireCookTimeTicks.get()));
    }
}
