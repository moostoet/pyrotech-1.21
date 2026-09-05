package com.moostoet.pyrotech.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.event.EventHooks;

/**
 * The 1.12 {@code BlockBushBase}: an eight-stage bush that is a soft plant up to age 3 and a
 * woody block from age 4, in sound, hardness, collision, push reaction, and replaceability.
 * The position-seeded sixteenth offset is vanilla's {@code XZ} offset capped at one
 * sixteenth; the 1.12 position-seeded facing is the four rotations in the generated
 * blockstate, which the model bakery picks by position.
 */
public abstract class BerryBushBlock extends Block implements BushSoil {

    public static final int MAX_AGE = 7;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_7;

    private static final VoxelShape[] SHAPES = {
        Block.box(7, 0, 7, 9, 3, 9),
        Block.box(7, 0, 7, 9, 6, 9),
        Block.box(7, 0, 7, 9, 7, 9),
        Block.box(7, 0, 7, 9, 8, 9),
        Block.box(4, 6, 4, 12, 10, 12),
        Block.box(3, 5, 3, 13, 11, 13),
        Block.box(2, 4, 2, 14, 12, 14),
        Block.box(2, 4, 2, 14, 12, 14)
    };

    protected BerryBushBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    public static int age(BlockState state) {
        return state.getValue(AGE);
    }

    public static boolean isMaxAge(BlockState state) {
        return age(state) >= MAX_AGE;
    }

    public BlockState withAge(int age) {
        return this.defaultBlockState().setValue(AGE, age);
    }

    /** True while the bush is a soft plant: ages 0 to 3. */
    protected static boolean isSoft(BlockState state) {
        return age(state) < 4;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    // -- Shape ---------------------------------------------------------------

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Vec3 offset = state.getOffset(level, pos);
        return SHAPES[age(state)].move(offset.x, offset.y, offset.z);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return isSoft(state) ? Shapes.empty() : this.getShape(state, level, pos, context);
    }

    @Override
    protected float getMaxHorizontalOffset() {
        return 0.0625F;
    }

    // -- Age-dependent material ---------------------------------------------

    @Override
    protected SoundType getSoundType(BlockState state) {
        return isSoft(state) ? SoundType.GRASS : SoundType.WOOD;
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return isSoft(state) ? PushReaction.DESTROY : PushReaction.BLOCK;
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext useContext) {
        return isSoft(state);
    }

    /** 1.12 hardness: 0.1 below age 3, the block's own 1 from then on. */
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        float hardness = age(state) < 3 ? 0.1F : 1.0F;
        int divisor = EventHooks.doPlayerHarvestCheck(player, state, level, pos) ? 30 : 100;
        return player.getDigSpeed(state, pos) / hardness / divisor;
    }

    // -- Survival ------------------------------------------------------------

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState soil = level.getBlockState(below);
        return this.isValidSoil(level, below, soil) && soil.isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        this.checkAndDrop(state, level, pos);
    }

    /** Pops the bush with its drops when the ground under it went away. Returns true if it did. */
    protected boolean checkAndDrop(BlockState state, Level level, BlockPos pos) {
        if (!level.isClientSide && !state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
            return true;
        }
        return false;
    }

    // -- Effects -------------------------------------------------------------

    /** The 1.12 particle packets: {@code count} particles spread over half a block around the centre. */
    protected static void sendParticles(ServerLevel level, BlockPos pos, ParticleOptions particle, int count) {
        level.sendParticles(particle, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, count, 0.5, 0.5, 0.5, 0);
    }
}
