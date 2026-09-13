package com.moostoet.pyrotech.tech.basic.block;

import com.mojang.serialization.MapCodec;
import com.moostoet.pyrotech.core.CoreBlocks;
import com.moostoet.pyrotech.core.PyrotechTags;
import com.moostoet.pyrotech.core.ignition.IgnitableAdjacentFire;
import com.moostoet.pyrotech.core.ignition.IgnitableAdjacentIgniterBlock;
import com.moostoet.pyrotech.tech.basic.TechBasicBlockEntities;
import com.moostoet.pyrotech.tech.basic.block.entity.PitKilnBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.ItemAbilities;
import org.jetbrains.annotations.Nullable;

/**
 * The pit kiln: an empty pit takes the ware and a layer of thatch, three logs go on
 * top, and fire on the logs starts the burn. The burn needs solid, unburnable walls and
 * floor; a broken wall for five seconds ruins the ware. Rain puts it out.
 */
public final class PitKilnBlock extends BaseEntityBlock implements IgnitableAdjacentFire, IgnitableAdjacentIgniterBlock {

    public static final MapCodec<PitKilnBlock> CODEC = simpleCodec(PitKilnBlock::new);
    public static final EnumProperty<PitKilnVariant> VARIANT = EnumProperty.create("variant", PitKilnVariant.class);

    private static final VoxelShape EMPTY_SHAPE = Block.box(0, 0, 0, 16, 3, 16);
    private static final VoxelShape THATCH_COLLISION = Block.box(0, 0, 0, 16, 10, 16);

    public PitKilnBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(VARIANT, PitKilnVariant.EMPTY));
    }

    @Override
    protected MapCodec<PitKilnBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(VARIANT) == PitKilnVariant.EMPTY ? EMPTY_SHAPE : Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(VARIANT) == PitKilnVariant.THATCH ? THATCH_COLLISION : this.getShape(state, level, pos, context);
    }

    @Override
    public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
        return switch (state.getValue(VARIANT)) {
            case EMPTY -> SoundType.GRAVEL;
            case THATCH -> SoundType.GRASS;
            case COMPLETE -> SoundType.SAND;
            default -> SoundType.WOOD;
        };
    }

    @Override
    public boolean isFireSource(BlockState state, LevelReader level, BlockPos pos, Direction direction) {
        return state.getValue(VARIANT) == PitKilnVariant.ACTIVE;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof PitKilnBlockEntity kiln) {
            kiln.requestStructureValidation();
        }
    }

    // -- Ignition ------------------------------------------------------------

    /** Only fire on the logs lights the kiln. */
    @Override
    public void igniteWithAdjacentFire(Level level, BlockPos pos, BlockState state, Direction facing) {
        if (facing == Direction.UP && state.getValue(VARIANT) == PitKilnVariant.WOOD
            && level.getBlockEntity(pos) instanceof PitKilnBlockEntity kiln) {
            kiln.activate();
        }
    }

    /** An igniter block beside a loaded kiln lights fire on the logs; the fire then lights the kiln. */
    @Override
    public void igniteWithAdjacentIgniterBlock(Level level, BlockPos pos, BlockState state, Direction facing) {
        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);
        if (state.getValue(VARIANT) == PitKilnVariant.WOOD && !aboveState.liquid()
            && (aboveState.isAir() || aboveState.canBeReplaced())) {
            level.setBlock(above, BaseFireBlock.getState(level, above), Block.UPDATE_ALL);
        }
    }

    // -- Interaction ---------------------------------------------------------

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                             InteractionHand hand, BlockHitResult hit) {
        if (stack.is(PyrotechTags.Items.IGNITERS) || stack.canPerformAction(ItemAbilities.FIRESTARTER_LIGHT)) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        if (!(level.getBlockEntity(pos) instanceof PitKilnBlockEntity kiln)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        PitKilnVariant variant = state.getValue(VARIANT);
        if (variant == PitKilnVariant.EMPTY && stack.is(CoreBlocks.THATCH.get().asItem())) {
            if (!level.isClientSide) {
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
                level.setBlock(pos, state.setValue(VARIANT, PitKilnVariant.THATCH), Block.UPDATE_ALL);
                level.playSound(null, pos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1, 1);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if ((variant == PitKilnVariant.THATCH || variant == PitKilnVariant.WOOD) && kiln.insertLog(player, stack, hit)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (variant == PitKilnVariant.EMPTY && kiln.insertInput(player, stack)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof PitKilnBlockEntity kiln)) {
            return InteractionResult.PASS;
        }
        PitKilnVariant variant = state.getValue(VARIANT);
        if ((variant == PitKilnVariant.THATCH || variant == PitKilnVariant.WOOD) && kiln.extractLog(player, hit)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (variant == PitKilnVariant.EMPTY && kiln.extractAll(player)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    // -- Lifecycle -----------------------------------------------------------

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof PitKilnBlockEntity kiln) {
            kiln.dropContents();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PitKilnBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, TechBasicBlockEntities.KILN_PIT.get(), PitKilnBlockEntity::serverTick);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
