package com.moostoet.pyrotech.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import java.util.EnumMap;
import java.util.Map;

/**
 * Glass that carries one boolean per face saying whether the neighbour on that side is the
 * same block, so the model can pick a connected texture. The 1.12 blocks recomputed the six
 * booleans in {@code getActualState} on every render; here they live in the block state and
 * are kept current by placement and neighbour updates.
 */
public class ConnectedGlassBlock extends Block {

    public static final BooleanProperty CONNECTED_DOWN = BooleanProperty.create("connected_down");
    public static final BooleanProperty CONNECTED_UP = BooleanProperty.create("connected_up");
    public static final BooleanProperty CONNECTED_NORTH = BooleanProperty.create("connected_north");
    public static final BooleanProperty CONNECTED_SOUTH = BooleanProperty.create("connected_south");
    public static final BooleanProperty CONNECTED_WEST = BooleanProperty.create("connected_west");
    public static final BooleanProperty CONNECTED_EAST = BooleanProperty.create("connected_east");

    private static final Map<Direction, BooleanProperty> BY_DIRECTION = new EnumMap<>(Map.of(
        Direction.DOWN, CONNECTED_DOWN,
        Direction.UP, CONNECTED_UP,
        Direction.NORTH, CONNECTED_NORTH,
        Direction.SOUTH, CONNECTED_SOUTH,
        Direction.WEST, CONNECTED_WEST,
        Direction.EAST, CONNECTED_EAST));

    /**
     * Whether a face shared with another block of the same kind is dropped from the mesh.
     * 1.12's slag glass overrode {@code shouldSideBeRendered} to do this and the refractory
     * glass did not, so the flag carries that difference forward.
     */
    private final boolean cullSharedFaces;

    public ConnectedGlassBlock(boolean cullSharedFaces, Properties properties) {
        super(properties);
        this.cullSharedFaces = cullSharedFaces;
        BlockState state = this.stateDefinition.any();
        for (BooleanProperty property : BY_DIRECTION.values()) {
            state = state.setValue(property, Boolean.FALSE);
        }
        this.registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CONNECTED_DOWN, CONNECTED_UP, CONNECTED_NORTH, CONNECTED_SOUTH, CONNECTED_WEST, CONNECTED_EAST);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockGetter level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = this.defaultBlockState();
        for (Map.Entry<Direction, BooleanProperty> entry : BY_DIRECTION.entrySet()) {
            state = state.setValue(entry.getValue(), this.connectsTo(level.getBlockState(pos.relative(entry.getKey()))));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(BY_DIRECTION.get(direction), this.connectsTo(neighborState));
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacentState, Direction direction) {
        if (this.cullSharedFaces && adjacentState.is(this)) {
            return true;
        }
        return super.skipRendering(state, adjacentState, direction);
    }

    /** The 1.12 {@code canConnect}: the neighbour is the very same block. */
    private boolean connectsTo(BlockState neighbor) {
        return neighbor.is(this);
    }
}
