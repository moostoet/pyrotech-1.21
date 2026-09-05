package com.moostoet.pyrotech.core;

import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * What counts as the wall of a refractory burn. 1.12 kept a hand-written list in
 * {@code modules/tech/refractory} {@code RegistryInitializer#initializeRefractoryBlocks};
 * here the list is the {@code #pyrotech:refractory} block tag, so a datapack can extend it.
 *
 * <p>The one entry a tag cannot express is the refractory brick slab: 1.12 listed only the
 * separate {@code refractory_brick_slab_double} block, so a half slab is not refractory
 * while a full one is. In 1.21 both are the same block at different {@link SlabType}s, so
 * the state test lives here.
 */
public final class RefractoryBlocks {

    private RefractoryBlocks() {
    }

    public static boolean isRefractory(BlockState state) {
        if (state.is(CoreBlocks.REFRACTORY_BRICK_SLAB.get())) {
            return state.getValue(SlabBlock.TYPE) == SlabType.DOUBLE;
        }
        return state.is(PyrotechTags.Blocks.REFRACTORY);
    }
}
