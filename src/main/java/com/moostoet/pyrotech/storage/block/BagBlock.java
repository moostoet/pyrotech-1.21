package com.moostoet.pyrotech.storage.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.moostoet.pyrotech.storage.block.entity.BagBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The rock bag and the durable rock bag: a placed bag of up to {@code capacity} items from
 * its tag, fed from the top while open, and opened or closed by any other click. The open
 * state is the {@code type} property, which the item carries in vanilla's {@code block_state}
 * component (storage sign-off, item 5).
 */
public final class BagBlock extends BaseEntityBlock {

    public static final MapCodec<BagBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.INT.fieldOf("capacity").forGetter(BagBlock::capacity),
        TagKey.codec(Registries.ITEM).fieldOf("allowed").forGetter(BagBlock::allowed),
        propertiesCodec()
    ).apply(instance, BagBlock::new));

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<Type> TYPE = EnumProperty.create("type", Type.class);
    /** The item model predicate that switches to the open model. */
    public static final ResourceLocation OPEN_PREDICATE = ResourceLocation.fromNamespaceAndPath("pyrotech", "open");

    private static final VoxelShape NORTH_SOUTH_SHAPE = Block.box(3, 0, 5, 13, 10, 11);
    private static final VoxelShape EAST_WEST_SHAPE = Block.box(5, 0, 3, 11, 10, 13);

    private final int capacity;
    private final TagKey<Item> allowed;

    public BagBlock(int capacity, TagKey<Item> allowed, Properties properties) {
        super(properties);
        this.capacity = capacity;
        this.allowed = allowed;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TYPE, Type.CLOSED));
    }

    public int capacity() {
        return this.capacity;
    }

    public TagKey<Item> allowed() {
        return this.allowed;
    }

    public static boolean isOpen(BlockState state) {
        return state.getValue(TYPE) == Type.OPEN;
    }

    /** Whether a bag item is open: its {@code block_state} component says {@code type=open}. */
    public static boolean isOpen(ItemStack stack) {
        BlockItemStateProperties properties = stack.get(DataComponents.BLOCK_STATE);
        return properties != null && properties.get(TYPE) == Type.OPEN;
    }

    /** Writes the open state on a bag item; a closed bag carries no component, like a fresh one. */
    public static void setOpen(ItemStack stack, boolean open) {
        if (open) {
            stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(TYPE, Type.OPEN));
        } else {
            stack.remove(DataComponents.BLOCK_STATE);
        }
    }

    @Override
    protected MapCodec<BagBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TYPE);
    }

    /** Facing only; the item's {@code block_state} component sets {@code type} right after. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.X ? EAST_WEST_SHAPE : NORTH_SOUTH_SHAPE;
    }

    /** 1.12 order: the input slot on the top while open, then the toggle on any face. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                             InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (hand != InteractionHand.MAIN_HAND || !(level.getBlockEntity(pos) instanceof BagBlockEntity bag)) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        if (!bag.insert(player, stack, hit)) {
            bag.toggleOpen();
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof BagBlockEntity bag)) {
            return InteractionResult.PASS;
        }
        if (!bag.extract(player, hit)) {
            bag.toggleOpen();
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Pick-block hands out the bag with its contents and its open state, as the loot table does. */
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        ItemStack stack = super.getCloneItemStack(level, pos, state);
        if (level.getBlockEntity(pos) instanceof BagBlockEntity bag) {
            stack.applyComponents(bag.collectComponents());
        }
        setOpen(stack, isOpen(state));
        return stack;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BagBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    public enum Type implements StringRepresentable {
        CLOSED("closed"),
        OPEN("open");

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
