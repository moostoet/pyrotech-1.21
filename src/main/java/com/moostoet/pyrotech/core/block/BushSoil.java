package com.moostoet.pyrotech.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

/** A plant that knows which soil it grows on; its seeds ask before planting. */
public interface BushSoil {

    /** The 1.12 {@code isValidBlock} test: is {@code soil}, at {@code soilPos}, ground this plant accepts. */
    boolean isValidSoil(LevelReader level, BlockPos soilPos, BlockState soil);
}
