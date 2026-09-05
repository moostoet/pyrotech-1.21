package com.moostoet.pyrotech.core;

import com.moostoet.pyrotech.Pyrotech;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.datamaps.DataMapType;

/**
 * The tool level: the whole number, 0 to 4, that a tool brings to Pyrotech's machines.
 * An item's level comes from the {@code pyrotech:tool_levels} data map when it has an
 * entry, and from its vanilla tier otherwise (recipe architecture sign-off, item 2).
 */
public final class ToolLevels {

    public static final DataMapType<Item, Integer> TOOL_LEVELS = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(Pyrotech.MOD_ID, "tool_levels"),
            Registries.ITEM,
            ExtraCodecs.NON_NEGATIVE_INT)
        .synced(ExtraCodecs.NON_NEGATIVE_INT, false)
        .build();

    private ToolLevels() {
    }

    public static int of(ItemStack stack) {
        Integer level = stack.getItemHolder().getData(TOOL_LEVELS);
        if (level != null) {
            return level;
        }
        if (stack.getItem() instanceof TieredItem tiered) {
            return of(tiered.getTier());
        }
        return 0;
    }

    /** Crude and wood 0; stone and gold 1; iron 2; diamond 3; netherite 4. */
    public static int of(Tier tier) {
        TagKey<Block> incorrect = tier.getIncorrectBlocksForDrops();
        if (incorrect == BlockTags.INCORRECT_FOR_NETHERITE_TOOL) {
            return 4;
        }
        if (incorrect == BlockTags.INCORRECT_FOR_DIAMOND_TOOL) {
            return 3;
        }
        if (incorrect == BlockTags.INCORRECT_FOR_IRON_TOOL) {
            return 2;
        }
        if (incorrect == BlockTags.INCORRECT_FOR_STONE_TOOL || incorrect == BlockTags.INCORRECT_FOR_GOLD_TOOL) {
            return 1;
        }
        return 0;
    }
}
