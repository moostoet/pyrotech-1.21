package com.moostoet.pyrotech.core.event;

import com.moostoet.pyrotech.core.CoreConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import org.jetbrains.annotations.Nullable;

/**
 * The village tweaks: the first-load chunk scan (village tweaks sign-off, items 1 to 5).
 * A new chunk loses its vanilla crafting tables under {@code REMOVE_VANILLA_CRAFTING_TABLE};
 * under {@code REPLACE_VANILLA_FURNACE} its furnaces, blast furnaces, and smokers become
 * cobblestone and its campfires air. Chunks generated before Pyrotech joined are left alone.
 * Writes skip neighbour and shape updates so the scan never touches a neighbouring chunk.
 */
public final class ChunkScanHandler {

    private static final int WRITE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private ChunkScanHandler() {
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!event.isNewChunk() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        boolean removeCraftingTable = CoreConfig.COMMON.removeVanillaCraftingTable.get();
        boolean replaceFurnace = CoreConfig.COMMON.replaceVanillaFurnace.get();
        if (!removeCraftingTable && !replaceFurnace) {
            return;
        }
        ChunkAccess chunk = event.getChunk();
        ChunkPos chunkPos = chunk.getPos();
        LevelChunkSection[] sections = chunk.getSections();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int index = 0; index < sections.length; index++) {
            LevelChunkSection section = sections[index];
            if (section.hasOnlyAir()
                || !section.maybeHas(state -> replacement(state, removeCraftingTable, replaceFurnace) != null)) {
                continue;
            }
            int minY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(index));
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState replacement = replacement(section.getBlockState(x, y, z), removeCraftingTable, replaceFurnace);
                        if (replacement != null) {
                            pos.set(chunkPos.getBlockX(x), minY + y, chunkPos.getBlockZ(z));
                            level.setBlock(pos, replacement, WRITE_FLAGS);
                        }
                    }
                }
            }
        }
    }

    @Nullable
    private static BlockState replacement(BlockState state, boolean removeCraftingTable, boolean replaceFurnace) {
        if (removeCraftingTable && state.is(Blocks.CRAFTING_TABLE)) {
            return Blocks.AIR.defaultBlockState();
        }
        if (replaceFurnace) {
            if (state.is(Blocks.FURNACE) || state.is(Blocks.BLAST_FURNACE) || state.is(Blocks.SMOKER)) {
                return Blocks.COBBLESTONE.defaultBlockState();
            }
            if (state.is(Blocks.CAMPFIRE)) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        return null;
    }
}
