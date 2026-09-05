package com.moostoet.pyrotech.core.event;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Counts, on the server, how many ticks each player has stood on the same block position.
 * Tech/basic's campfire resting effect reads it.
 */
public final class PlayerMovementTracker {

    private static final Object2IntMap<UUID> TICKS_SINCE_LAST_MOVE = new Object2IntOpenHashMap<>();
    private static final Map<UUID, BlockPos> LAST_KNOWN_POSITION = new HashMap<>();

    private PlayerMovementTracker() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        UUID id = player.getUUID();
        BlockPos position = player.blockPosition();
        if (position.equals(LAST_KNOWN_POSITION.get(id))) {
            TICKS_SINCE_LAST_MOVE.put(id, TICKS_SINCE_LAST_MOVE.getInt(id) + 1);
        } else {
            TICKS_SINCE_LAST_MOVE.put(id, 0);
        }
        LAST_KNOWN_POSITION.put(id, position);
    }

    public static int ticksSinceLastMove(Player player) {
        return TICKS_SINCE_LAST_MOVE.getInt(player.getUUID());
    }
}
