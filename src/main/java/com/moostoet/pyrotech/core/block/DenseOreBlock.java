package com.moostoet.pyrotech.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A dense ore cluster on a cave floor, in one of three sizes. It needs a solid face
 * below, drops experience in the 1.12 range for its size, and shares one display name per
 * ore across the sizes, as 1.12 did.
 */
public abstract class DenseOreBlock extends Block {

    private final VoxelShape shape;
    private final UniformInt experience;
    private final String descriptionId;

    protected DenseOreBlock(VoxelShape shape, UniformInt experience, String descriptionId, Properties properties) {
        super(properties);
        this.shape = shape;
        this.experience = experience;
        this.descriptionId = descriptionId;
    }

    @Override
    public String getDescriptionId() {
        return this.descriptionId;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.shape;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    public int getExpDrop(BlockState state, LevelAccessor level, BlockPos pos, @Nullable BlockEntity blockEntity, @Nullable Entity breaker, ItemStack tool) {
        return this.experience.sample(level.getRandom());
    }
}
