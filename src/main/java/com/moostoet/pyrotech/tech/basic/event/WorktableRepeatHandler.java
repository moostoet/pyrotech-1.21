package com.moostoet.pyrotech.tech.basic.event;

import com.moostoet.pyrotech.core.PyrotechTags;
import com.moostoet.pyrotech.tech.basic.TechBasicConfig;
import com.moostoet.pyrotech.tech.basic.block.WorktableBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * The 1.12 {@code RecipeRepeat} listener: vanilla skips a block's use for a sneaking
 * player holding an item, so a sneaking hammer would never reach the worktable. This
 * sends it through when recipe repeat is on.
 */
public final class WorktableRepeatHandler {

    private WorktableRepeatHandler() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!TechBasicConfig.COMMON.allowRecipeRepeat.get() || !event.getEntity().isShiftKeyDown()
            || !event.getItemStack().is(PyrotechTags.Items.HAMMERS)
            || !(event.getLevel().getBlockState(event.getPos()).getBlock() instanceof WorktableBlock)) {
            return;
        }
        event.setUseBlock(TriState.TRUE);
        event.setUseItem(TriState.FALSE);
    }
}
