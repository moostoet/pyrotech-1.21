package com.moostoet.pyrotech.tech.basic.block.entity;

import com.moostoet.pyrotech.core.network.NoHungerPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * The hunger gate the chopping block, compacting bin, anvils, and worktables share: a
 * player below three hunger cannot work them, and their client shows the no-hunger icon.
 */
public final class HungerGate {

    public static final int MINIMUM_HUNGER = 3;

    private HungerGate() {
    }

    /** True when the player is too hungry to work, after telling their client so. */
    public static boolean blocks(Player player) {
        if (player.getFoodData().getFoodLevel() >= MINIMUM_HUNGER) {
            return false;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            NoHungerPayload.send(serverPlayer);
        }
        return true;
    }
}
