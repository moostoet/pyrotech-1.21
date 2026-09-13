package com.moostoet.pyrotech.library.interaction;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A block entity that takes sneak plus scroll: up puts one of the held item into the slot
 * under the cursor, down takes one out. The client sends the hit through core's scroll
 * payload; the block entity decides what the hit means.
 */
public interface ScrollInteractable {

    void scroll(ServerPlayer player, BlockHitResult hit, boolean up);
}
