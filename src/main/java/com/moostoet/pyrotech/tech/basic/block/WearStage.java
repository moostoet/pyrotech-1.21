package com.moostoet.pyrotech.tech.basic.block;

import com.moostoet.pyrotech.Pyrotech;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * A worn anvil or chopping block as an item (tech/basic sign-off, item 8): the {@code damage}
 * property rides vanilla's {@code block_state} component, which the loot table copies from
 * the block, placement applies to the new block, and the item model reads for its override.
 */
public final class WearStage {

    /** The item model predicate: the stage, 0 for a fresh block. */
    public static final ResourceLocation PREDICATE = ResourceLocation.fromNamespaceAndPath(Pyrotech.MOD_ID, "damage");

    private WearStage() {
    }

    public static int of(ItemStack stack, IntegerProperty damage) {
        BlockItemStateProperties properties = stack.get(DataComponents.BLOCK_STATE);
        if (properties == null) {
            return 0;
        }
        Integer value = properties.get(damage);
        return value == null ? 0 : value;
    }

    /** Writes the stage on the item; a fresh block carries no component, so it stacks with a new one. */
    public static ItemStack with(ItemStack stack, IntegerProperty damage, int value) {
        if (value > 0) {
            stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(damage, value));
        } else {
            stack.remove(DataComponents.BLOCK_STATE);
        }
        return stack;
    }
}
