package com.moostoet.pyrotech.core.block;

import com.moostoet.pyrotech.core.CoreConfig;
import com.moostoet.pyrotech.core.block.entity.MulchedFarmlandBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.TriState;
import org.jetbrains.annotations.Nullable;

/**
 * Mulched farmland. On half its random ticks it bone-meals the crop above and spends a
 * charge; out of charges it becomes wet vanilla farmland. It never dries, grows crops
 * only, and turns to dirt under a solid block or, when the tweak allows, under a heavy
 * landing.
 */
public final class MulchedFarmlandBlock extends Block implements EntityBlock {

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 15, 16);

    public MulchedFarmlandBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MulchedFarmlandBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    /** The 1.12 face test: no seam against farmland, a dirt path, or more mulched farmland. */
    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacentState, Direction direction) {
        if (direction.getAxis().isHorizontal()) {
            return adjacentState.is(Blocks.FARMLAND) || adjacentState.is(Blocks.DIRT_PATH) || adjacentState.is(this);
        }
        return super.skipRendering(state, adjacentState, direction);
    }

    @Override
    public TriState canSustainPlant(BlockState state, BlockGetter level, BlockPos soilPosition, Direction facing, BlockState plant) {
        if (plant.is(BlockTags.CROPS) || plant.getBlock() instanceof CropBlock) {
            return TriState.TRUE;
        }
        return super.canSustainPlant(state, level, soilPosition, facing, plant);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() > 0.5) {
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof MulchedFarmlandBlockEntity farmland)) {
            return;
        }
        BlockPos above = pos.above();
        BlockState crop = level.getBlockState(above);
        if (crop.getBlock() instanceof BonemealableBlock growable
            && growable.isValidBonemealTarget(level, above, crop)
            && growable.isBonemealSuccess(level, random, above, crop)) {
            growable.performBonemeal(level, random, above, crop);
            if (!CoreConfig.COMMON.mulchedFarmlandUnlimitedCharges.get()) {
                farmland.decrementRemainingCharges();
            }
            level.levelEvent(2005, above, 4);
        }
        if (farmland.getRemainingCharges() == 0) {
            level.setBlock(pos, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE), Block.UPDATE_ALL);
        }
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        if (!level.isClientSide
            && CoreConfig.COMMON.mulchedFarmlandAllowTrample.get()
            && CommonHooks.onFarmlandTrample(level, pos, Blocks.DIRT.defaultBlockState(), fallDistance, entity)) {
            FarmBlock.turnToDirt(entity, state, level, pos);
        }
        super.fallOn(level, state, pos, entity, fallDistance);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        this.turnToDirtUnderSolid(state, level, pos);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        this.turnToDirtUnderSolid(state, level, pos);
    }

    private void turnToDirtUnderSolid(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockState(pos.above()).isSolid()) {
            FarmBlock.turnToDirt(null, state, level, pos);
        }
    }
}
