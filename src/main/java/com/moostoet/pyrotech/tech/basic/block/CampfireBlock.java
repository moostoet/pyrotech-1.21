package com.moostoet.pyrotech.tech.basic.block;

import com.mojang.serialization.MapCodec;
import com.moostoet.pyrotech.core.PyrotechTags;
import com.moostoet.pyrotech.core.ignition.IgnitableWithIgniterItem;
import com.moostoet.pyrotech.library.fluid.Dousing;
import com.moostoet.pyrotech.tech.basic.TechBasicBlockEntities;
import com.moostoet.pyrotech.tech.basic.block.entity.CampfireBlockEntity;
import com.moostoet.pyrotech.tech.basic.block.entity.SoakingPotBlockEntity;
import com.moostoet.pyrotech.tech.basic.item.MarshmallowStickItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
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
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;

/**
 * The campfire block. Tinder places it unlit; flint and steel, a fire charge, or an
 * igniter lights it; water, rain, or a full ash pile puts it out. Its own drops come from
 * the block entity, never a loot table, since they depend on what it holds and how it
 * went out.
 */
public final class CampfireBlock extends BaseEntityBlock implements IgnitableWithIgniterItem {

    public static final MapCodec<CampfireBlock> CODEC = simpleCodec(CampfireBlock::new);
    public static final EnumProperty<CampfireVariant> VARIANT = EnumProperty.create("variant", CampfireVariant.class);
    public static final IntegerProperty ASH = IntegerProperty.create("ash", 0, CampfireBlockEntity.MAX_ASH);

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 5, 16);

    public CampfireBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(VARIANT, CampfireVariant.NORMAL).setValue(ASH, 0));
    }

    @Override
    protected MapCodec<CampfireBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT, ASH);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    // -- Fire ----------------------------------------------------------------

    @Override
    public boolean hasDynamicLightEmission(BlockState state) {
        return true;
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        AuxiliaryLightManager lights = level.getAuxLightManager(pos);
        return lights == null ? 0 : lights.getLightAt(pos);
    }

    @Override
    public boolean isFireSource(BlockState state, LevelReader level, BlockPos pos, Direction direction) {
        return state.getValue(VARIANT) == CampfireVariant.LIT;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (state.getValue(VARIANT) == CampfireVariant.LIT && !entity.fireImmune() && entity instanceof LivingEntity living
            && !living.isSteppingCarefully()) {
            entity.hurt(level.damageSources().hotFloor(), 1);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void igniteWithIgniterItem(Level level, BlockPos pos, BlockState state, Direction facing) {
        if (level.getBlockEntity(pos) instanceof CampfireBlockEntity campfire) {
            campfire.ignite();
        }
    }

    // -- Interaction ---------------------------------------------------------

    /**
     * The 1.12 interaction order: igniters and marshmallow sticks use themselves, a dead
     * fire takes nothing, water douses, food goes in from above, a shovel scoops ash,
     * flint and steel or a fire charge light, and a log joins the pile.
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                             InteractionHand hand, BlockHitResult hit) {
        if (stack.is(PyrotechTags.Items.IGNITERS) || stack.getItem() instanceof MarshmallowStickItem) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        if (!(level.getBlockEntity(pos) instanceof CampfireBlockEntity campfire) || campfire.isDead()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (campfire.isLit() && Dousing.tryDouse(player, hand)) {
            if (!level.isClientSide) {
                campfire.douse();
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1, 1);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (hit.getDirection() == Direction.UP && !player.isShiftKeyDown() && stack.has(DataComponents.FOOD)
            && campfire.insertFood(player, stack)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.canPerformAction(ItemAbilities.SHOVEL_DIG) && campfire.shovelAsh(player, stack, hand)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (state.getValue(VARIANT) == CampfireVariant.NORMAL
            && (stack.canPerformAction(ItemAbilities.FIRESTARTER_LIGHT) || stack.is(Items.FIRE_CHARGE))) {
            if (!level.isClientSide) {
                campfire.ignite();
                if (stack.is(Items.FIRE_CHARGE)) {
                    level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1, 1);
                    if (!player.isCreative()) {
                        stack.shrink(1);
                    }
                } else {
                    level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1, 1);
                    stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (campfire.addLog(player, stack)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** An empty hand takes the food from above, else the top log. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CampfireBlockEntity campfire) || campfire.isDead()) {
            return InteractionResult.PASS;
        }
        if (hit.getDirection() == Direction.UP && campfire.extractFood(player)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (campfire.removeLog(player, true)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    // -- Lifecycle -----------------------------------------------------------

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CampfireBlockEntity campfire) {
            campfire.dropContents();
            if (level.getBlockEntity(pos.above()) instanceof SoakingPotBlockEntity pot) {
                pot.dropContents();
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CampfireBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != TechBasicBlockEntities.CAMPFIRE.get()) {
            return null;
        }
        return level.isClientSide
            ? createTickerHelper(type, TechBasicBlockEntities.CAMPFIRE.get(), CampfireBlockEntity::clientTick)
            : createTickerHelper(type, TechBasicBlockEntities.CAMPFIRE.get(), CampfireBlockEntity::serverTick);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
