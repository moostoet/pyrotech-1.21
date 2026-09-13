package com.moostoet.pyrotech.storage.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.moostoet.pyrotech.storage.block.entity.ShelfBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The shelf and the durable shelf: nine slots in a three by three grid on the front, each
 * holding {@code maxStacks} stacks. The shelf sits against the back or the front of its
 * block, chosen from where the player clicked the wall.
 */
public final class ShelfBlock extends BaseEntityBlock {

    public static final MapCodec<ShelfBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.INT.fieldOf("max_stacks").forGetter(ShelfBlock::maxStacks),
        propertiesCodec()
    ).apply(instance, ShelfBlock::new));

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<Type> TYPE = EnumProperty.create("type", Type.class);

    private static final VoxelShape SOUTH_HALF = Block.box(0, 0, 10, 16, 16, 16);
    private static final VoxelShape NORTH_HALF = Block.box(0, 0, 0, 16, 16, 6);
    private static final VoxelShape WEST_HALF = Block.box(0, 0, 0, 6, 16, 16);
    private static final VoxelShape EAST_HALF = Block.box(10, 0, 0, 16, 16, 16);

    private final int maxStacks;

    public ShelfBlock(int maxStacks, Properties properties) {
        super(properties);
        this.maxStacks = maxStacks;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TYPE, Type.BACK));
    }

    public int maxStacks() {
        return this.maxStacks;
    }

    @Override
    protected MapCodec<ShelfBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TYPE);
    }

    /**
     * Against the wall the player faces, the shelf sits at the back. Against a side wall,
     * the half of the wall face the player clicked picks the back or the front.
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction playerFacing = context.getHorizontalDirection();
        Direction face = context.getClickedFace();
        Vec3 hit = context.getClickLocation();
        double hitX = Mth.frac(hit.x);
        double hitZ = Mth.frac(hit.z);
        Type type = Type.BACK;
        if (face != playerFacing.getOpposite() && face != playerFacing) {
            boolean forward = switch (playerFacing) {
                case SOUTH -> hitZ < 0.5;
                case NORTH -> hitZ > 0.5;
                case EAST -> hitX < 0.5;
                case WEST -> hitX > 0.5;
                default -> false;
            };
            if (forward) {
                type = Type.FORWARD;
            }
        }
        return this.defaultBlockState().setValue(FACING, playerFacing.getOpposite()).setValue(TYPE, type);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean back = state.getValue(TYPE) == Type.BACK;
        return switch (state.getValue(FACING)) {
            case SOUTH -> back ? NORTH_HALF : SOUTH_HALF;
            case EAST -> back ? WEST_HALF : EAST_HALF;
            case WEST -> back ? EAST_HALF : WEST_HALF;
            default -> back ? SOUTH_HALF : NORTH_HALF;
        };
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                             InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (hand == InteractionHand.MAIN_HAND
            && level.getBlockEntity(pos) instanceof ShelfBlockEntity shelf
            && shelf.insert(player, stack, hit)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof ShelfBlockEntity shelf && shelf.extract(player, hit)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ShelfBlockEntity shelf) {
            shelf.dropContents();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShelfBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** Which half of its block the shelf sits in: the back, away from its facing, or the front. */
    public enum Type implements StringRepresentable {
        BACK("back"),
        FORWARD("forward");

        private final String name;

        Type(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}
