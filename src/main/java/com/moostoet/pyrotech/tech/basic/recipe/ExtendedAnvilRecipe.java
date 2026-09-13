package com.moostoet.pyrotech.tech.basic.recipe;

import com.moostoet.pyrotech.tech.basic.block.entity.AnvilBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The five 1.12 {@code AnvilRecipe.IExtendedRecipe} hooks (recipe architecture sign-off,
 * item 3): a recipe on the {@code pyrotech:anvil} type that wants to wear the anvil its
 * own way, scale a hit, finish itself, draw its own hit particles, or handle the anvil
 * breaking under it. Bloomery's bloom recipe implements it; the anvil tests for it.
 */
public interface ExtendedAnvilRecipe {

    /** Wears the anvil on a hit, in place of the plain one-durability step. */
    void applyDamage(Level level, AnvilBlockEntity anvil);

    /** The progress one hit adds, given the plain increment. {@code player} is null for the trip hammer. */
    float modifiedProgressIncrement(float increment, BlockPos anvilPos, Vec3 hammerPos, ItemStack hammer, @Nullable Player player);

    /** Finishes the recipe: takes what it needs from the anvil and returns what the hit yields. */
    List<ItemStack> onRecipeCompleted(AnvilBlockEntity anvil, Level level, ItemStack tool);

    /** Client-side particles for a hit. */
    void onAnvilHitClient(Level level, AnvilBlockEntity anvil, Vec3 hit);

    /** The anvil wore out under this recipe's item. */
    void onAnvilDurabilityExpired(Level level, AnvilBlockEntity anvil, Vec3 hit);
}
