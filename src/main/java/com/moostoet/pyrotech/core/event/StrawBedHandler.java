package com.moostoet.pyrotech.core.event;

import com.moostoet.pyrotech.core.CoreBlocks;
import com.moostoet.pyrotech.core.CoreConfig;
import com.moostoet.pyrotech.core.block.StrawBedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The 1.12 {@code StrawBedEventHandler}. A straw bed never becomes a spawn point, and a
 * bed that has been slept in is destroyed: at the next daytime when the daytime check is
 * on, or as soon as it is empty when the check is off. The used-bed list lives for the
 * server's life, as in 1.12; a bed used before a restart is left standing.
 */
public final class StrawBedHandler {

    private static final Map<ResourceKey<Level>, List<BlockPos>> USED_BEDS = new HashMap<>();

    private StrawBedHandler() {
    }

    /** Records a straw bed a player just fell asleep in. Called on the server with the head position. */
    public static void markUsed(Level level, BlockPos pos) {
        USED_BEDS.computeIfAbsent(level.dimension(), key -> new ArrayList<>()).add(pos.immutable());
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        Level level = event.getLevel();
        if (level.isClientSide) {
            return;
        }
        List<BlockPos> beds = USED_BEDS.get(level.dimension());
        if (beds == null || beds.isEmpty()) {
            return;
        }
        boolean daytimeCheck = CoreConfig.COMMON.strawBedDaytimeDestroyCheck.get();
        if (daytimeCheck && level.isDay()) {
            for (BlockPos pos : beds) {
                if (isStrawBed(level.getBlockState(pos))) {
                    level.destroyBlock(pos, false);
                }
            }
            beds.clear();
        } else if (!daytimeCheck) {
            beds.removeIf(pos -> {
                BlockState state = level.getBlockState(pos);
                if (!isStrawBed(state)) {
                    return true;
                }
                if (!state.getValue(StrawBedBlock.OCCUPIED)) {
                    level.destroyBlock(pos, false);
                    return true;
                }
                return false;
            });
        }
    }

    @SubscribeEvent
    public static void onSetSpawn(PlayerSetSpawnEvent event) {
        Level level = event.getEntity().level();
        BlockPos spawn = event.getNewSpawn();
        if (!level.isClientSide && spawn != null && isStrawBed(level.getBlockState(spawn))) {
            event.setCanceled(true);
        }
    }

    private static boolean isStrawBed(BlockState state) {
        return state.is(CoreBlocks.STRAW_BED.get());
    }
}
