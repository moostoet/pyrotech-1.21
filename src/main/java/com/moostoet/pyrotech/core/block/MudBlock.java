package com.moostoet.pyrotech.core.block;

import com.moostoet.pyrotech.core.PyrotechTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The mud block: a full block with a collision box a sixteenth short, so whatever stands
 * in it is inside it and slowed to two fifths. A mud walker is sped up instead.
 */
public final class MudBlock extends Block {

    private static final VoxelShape COLLISION = Block.box(0, 0, 0, 16, 14, 16);

    public MudBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        slow(entity);
    }

    static void slow(Entity entity) {
        double factor = entity.getType().is(PyrotechTags.EntityTypes.MUD_WALKERS) ? 1.05 : 0.4;
        entity.setDeltaMovement(entity.getDeltaMovement().multiply(factor, 1, factor));
    }
}
