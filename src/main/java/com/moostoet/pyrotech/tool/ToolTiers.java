package com.moostoet.pyrotech.tool;

import com.moostoet.pyrotech.core.CoreItems;
import com.moostoet.pyrotech.core.Material;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.SimpleTier;

import java.util.function.Supplier;

/**
 * The eight tool tiers. 1.12 had five materials plus config maps for durability and
 * harvest level; a 1.21 tier owns both, so each distinct durability is its own tier with
 * the 1.12 defaults baked in (tool sign-off, item 1). The crude tier is vanilla stone at
 * the speed the 1.12 crude tools halved. The incorrect-for-drops tag is also the tool
 * level core reads (recipe architecture sign-off, item 2): crude 0, obsidian 2, the rest 1.
 */
public final class ToolTiers {

    public static final Tier CRUDE = tier(BlockTags.INCORRECT_FOR_WOODEN_TOOL, 32, 2.0f, 1.0f, 5,
        () -> Ingredient.of(ItemTags.STONE_TOOL_MATERIALS));
    public static final Tier BONE = tier(BlockTags.INCORRECT_FOR_STONE_TOOL, 150, 3.8f, 1.0f, 5, () -> Ingredient.of(Items.BONE));
    public static final Tier BONE_DURABLE = tier(BlockTags.INCORRECT_FOR_STONE_TOOL, 600, 3.8f, 1.0f, 5, () -> Ingredient.of(Items.BONE));
    public static final Tier FLINT = tier(BlockTags.INCORRECT_FOR_STONE_TOOL, 150, 3.8f, 1.0f, 5, () -> Ingredient.of(Items.FLINT));
    public static final Tier FLINT_DURABLE = tier(BlockTags.INCORRECT_FOR_STONE_TOOL, 600, 3.8f, 1.0f, 5, () -> Ingredient.of(Items.FLINT));
    public static final Tier REDSTONE = tier(BlockTags.INCORRECT_FOR_STONE_TOOL, 200, 2.8f, 1.0f, 9,
        () -> Ingredient.of(CoreItems.material(Material.DENSE_REDSTONE).get()));
    public static final Tier QUARTZ = tier(BlockTags.INCORRECT_FOR_STONE_TOOL, 350, 3.2f, 1.0f, 2,
        () -> Ingredient.of(CoreItems.material(Material.DENSE_QUARTZ).get()));
    /** 1.12 gave obsidian no repair item. */
    public static final Tier OBSIDIAN = tier(BlockTags.INCORRECT_FOR_IRON_TOOL, 1400, 6.0f, 2.0f, 18, () -> Ingredient.of());

    private ToolTiers() {
    }

    private static Tier tier(TagKey<Block> incorrectForDrops, int uses, float speed, float attackDamageBonus,
                             int enchantmentValue, Supplier<Ingredient> repairIngredient) {
        return new SimpleTier(incorrectForDrops, uses, speed, attackDamageBonus, enchantmentValue, repairIngredient);
    }
}
