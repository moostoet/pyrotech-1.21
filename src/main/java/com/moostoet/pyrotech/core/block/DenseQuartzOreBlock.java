package com.moostoet.pyrotech.core.block;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Dense quartz. The drops are the block's loot table; the experience is the 1.12 range for each size. */
public final class DenseQuartzOreBlock extends DenseOreBlock {

    public DenseQuartzOreBlock(VoxelShape shape, UniformInt experience, Properties properties) {
        super(shape, experience, "block.pyrotech.dense_quartz_ore", properties);
    }
}
