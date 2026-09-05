package com.moostoet.pyrotech.core.event;

import com.moostoet.pyrotech.core.ignition.IgnitableAdjacentFire;
import com.moostoet.pyrotech.core.ignition.PyrotechIgnition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * The 1.12 core and refractory {@code NeighborNotifyEventHandler}s as one: when fire
 * notifies its neighbours, each notified ignitable block lights, and the ignition hook is
 * asked for each notified position.
 */
public final class FireAdjacencyHandler {

    private FireAdjacencyHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level) || !event.getState().is(BlockTags.FIRE)) {
            return;
        }
        BlockPos firePos = event.getPos();
        for (Direction side : event.getNotifiedSides()) {
            BlockPos pos = firePos.relative(side);
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof IgnitableAdjacentFire ignitable) {
                ignitable.igniteWithAdjacentFire(level, pos, state, side.getOpposite());
            }
            PyrotechIgnition.tryIgnite(level, pos);
        }
    }
}
