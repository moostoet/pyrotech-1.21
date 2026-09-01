package com.moostoet.pyrotech.prototype.campfire;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.BooleanSupplier;

/**
 * Replaces athenaeum's interaction framework: useItemOn and useWithoutItem
 * dispatch by held item and hit face, in the same priority order as the 1.12
 * interaction array. The block entity owns the actual insert/extract logic.
 */
public class CampfireBlock extends BaseEntityBlock {

    public static final MapCodec<CampfireBlock> CODEC = simpleCodec(CampfireBlock::new);

    public static final EnumProperty<CampfireVariant> VARIANT = EnumProperty.create("variant", CampfireVariant.class);
    public static final IntegerProperty ASH = IntegerProperty.create("ash", 0, 8);

    private static final VoxelShape SHAPE_FULL = Block.box(0, 0, 0, 16, 6, 16);
    private static final VoxelShape SHAPE_TINDER = Block.box(4, 0, 4, 12, 5, 12);

    public CampfireBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(VARIANT, CampfireVariant.NORMAL)
            .setValue(ASH, 0));
    }

    @Override
    protected MapCodec<CampfireBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT, ASH);
    }

    // -----------------------------------------------------------------------
    // Interaction dispatch. 1.12 order: extinguish, food, shovel, igniter, log.
    // -----------------------------------------------------------------------

    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
                                             Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND
            || state.getValue(VARIANT) == CampfireVariant.ASH
            || !(level.getBlockEntity(pos) instanceof CampfireBlockEntity campfire)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (hit.getDirection() == Direction.UP && !player.isShiftKeyDown()) {
            if (runOnServer(level, () -> campfire.insertFood(player, held))) {
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }

        if (held.getItem() instanceof ShovelItem) {
            if (runOnServer(level, () -> campfire.shovelAsh(player))) {
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }

        if (held.is(Items.FLINT_AND_STEEL) || held.is(Items.FIRE_CHARGE)) {
            if (state.getValue(VARIANT) == CampfireVariant.NORMAL) {
                if (!level.isClientSide) {
                    campfire.ignite();
                    if (held.is(Items.FIRE_CHARGE)) {
                        held.shrink(1);
                        level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1, 1);
                    } else {
                        held.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
                        level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1, 1);
                    }
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }

        if (runOnServer(level, () -> campfire.addLogFrom(player, held))) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (state.getValue(VARIANT) == CampfireVariant.ASH
            || !(level.getBlockEntity(pos) instanceof CampfireBlockEntity campfire)) {
            return InteractionResult.PASS;
        }

        if (hit.getDirection() == Direction.UP) {
            if (runOnServer(level, () -> campfire.extractFoodTo(player))) {
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }

        if (runOnServer(level, () -> campfire.removeLogTo(player))) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }

    /**
     * The client cannot see the server-only checks inside the action, so it
     * optimistically reports success and lets the server packet correct it.
     */
    private static boolean runOnServer(Level level, BooleanSupplier action) {
        if (level.isClientSide) {
            return true;
        }
        return action.getAsBoolean();
    }

    // -----------------------------------------------------------------------
    // Light, shape, placement
    // -----------------------------------------------------------------------

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        if (state.getValue(VARIANT) == CampfireVariant.LIT
            && level.getBlockEntity(pos) instanceof CampfireBlockEntity campfire) {
            return campfire.getLightLevel();
        }
        return 0;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(VARIANT)) {
            case ASH -> Block.box(2, 0, 2, 14, Math.max(1, state.getValue(ASH)), 14);
            default -> (level.getBlockEntity(pos) instanceof CampfireBlockEntity campfire
                && campfire.getFuelCount() > 0) ? SHAPE_FULL : SHAPE_TINDER;
        };
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN && !this.canSurvive(state, level, pos)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    // -----------------------------------------------------------------------
    // Block entity wiring
    // -----------------------------------------------------------------------

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CampfireBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != PrototypeCampfire.CAMPFIRE_BLOCK_ENTITY.get()) {
            return null;
        }
        return (tickLevel, pos, tickState, blockEntity) ->
            CampfireBlockEntity.serverTick(tickLevel, pos, tickState, (CampfireBlockEntity) blockEntity);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())
            && level.getBlockEntity(pos) instanceof CampfireBlockEntity campfire) {
            campfire.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    // -----------------------------------------------------------------------
    // Flavor
    // -----------------------------------------------------------------------

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (state.getValue(VARIANT) == CampfireVariant.LIT
            && entity instanceof LivingEntity
            && !entity.fireImmune()) {
            entity.hurt(level.damageSources().hotFloor(), 1);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(VARIANT) != CampfireVariant.LIT) {
            return;
        }
        if (random.nextDouble() < 0.1) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.5f, 0.6f, false);
        }
        for (int i = 0; i < 3; i++) {
            double x = pos.getX() + 0.5 + (random.nextDouble() * 2.0 - 1.0) * 0.2;
            double y = pos.getY() + 0.25 + random.nextDouble() * 0.15;
            double z = pos.getZ() + 0.5 + (random.nextDouble() * 2.0 - 1.0) * 0.2;
            level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE, x, y, z, 0, 0, 0);
            level.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME, x, y, z, 0, 0, 0);
        }
    }

}
