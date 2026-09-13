package com.moostoet.pyrotech.tech.basic.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.moostoet.pyrotech.core.PyrotechTags;
import com.moostoet.pyrotech.tech.basic.TechBasicConfig;
import com.moostoet.pyrotech.tech.basic.block.entity.WorktableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The worktable and the stone worktable: a three by three crafting grid laid out on top,
 * a shelf of three below the front edge, and a hammer to beat the recipe together. The
 * stone table holds stacks, takes half the hits, and wears the hammer less.
 */
public final class WorktableBlock extends BaseEntityBlock {

    public static final MapCodec<WorktableBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.BOOL.fieldOf("stone").forGetter(WorktableBlock::isStone),
        propertiesCodec()
    ).apply(instance, WorktableBlock::new));

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private final boolean stone;

    public WorktableBlock(boolean stone, Properties properties) {
        super(properties);
        this.stone = stone;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public boolean isStone() {
        return this.stone;
    }

    public int gridStackLimit() {
        return this.stone ? 32 : 1;
    }

    public int shelfStackLimit() {
        return this.stone ? 64 : 1;
    }

    public int hitsPerCraft() {
        return this.stone ? 2 : 4;
    }

    public int toolDamagePerCraft() {
        return this.stone ? 1 : 2;
    }

    public int durability() {
        return this.stone ? 512 : 64;
    }

    public float exhaustionPerHit() {
        return this.stone ? 0.5f : 1;
    }

    public ModConfigSpec.BooleanValue usesDurability() {
        return this.stone ? TechBasicConfig.COMMON.stoneWorktableUsesDurability : TechBasicConfig.COMMON.worktableUsesDurability;
    }

    /** What a hit chips off: oak planks from the wooden table, andesite from the stone one. */
    public BlockState particleState() {
        return this.stone ? Blocks.ANDESITE.defaultBlockState() : Blocks.OAK_PLANKS.defaultBlockState();
    }

    @Override
    protected MapCodec<WorktableBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /**
     * On top, the 1.12 order: a hammer works the recipe (sneaking with one repeats it),
     * then the grid cell or shelf slot under the cursor takes one of the held item.
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                             InteractionHand hand, BlockHitResult hit) {
        if (hit.getDirection() != Direction.UP || !(level.getBlockEntity(pos) instanceof WorktableBlockEntity table)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.is(PyrotechTags.Items.HAMMERS)) {
            if (player.isShiftKeyDown()) {
                return table.repeatRecipe(player, stack, hand)
                    ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                    : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            return table.hammer(player, stack, hand, hit)
                ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (table.insert(player, stack, hit)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** An empty hand takes from the slot under the cursor; sneaking, it clears the grid. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (hit.getDirection() != Direction.UP || !(level.getBlockEntity(pos) instanceof WorktableBlockEntity table)) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown() && TechBasicConfig.COMMON.allowRecipeClear.get()) {
            return table.clearGrid(player) ? InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.PASS;
        }
        if (table.extract(player, hit)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof WorktableBlockEntity table) {
            table.dropContents();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WorktableBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
