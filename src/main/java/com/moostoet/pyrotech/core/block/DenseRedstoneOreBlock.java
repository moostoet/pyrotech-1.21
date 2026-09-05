package com.moostoet.pyrotech.core.block;

import com.moostoet.pyrotech.core.CoreSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RedStoneOreBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Dense redstone. Touching, walking on, hitting, or clicking it lights it up with a crackle
 * and a puff of redstone; lit, it glows, sparks, and goes dark again on a random tick.
 * 1.12 used two blocks per size; this is one block with the vanilla {@code lit} property,
 * the way the slabs became one block with a type. Proximity repair is the 1.12 tool config
 * default for the size, baked in (tool sign-off, item 2).
 */
public final class DenseRedstoneOreBlock extends DenseOreBlock {

    public static final BooleanProperty LIT = RedStoneOreBlock.LIT;

    private final int particleCount;
    private final int proximityRepairAmount;

    public DenseRedstoneOreBlock(VoxelShape shape, UniformInt experience, int particleCount, int proximityRepairAmount, Properties properties) {
        super(shape, experience, "block.pyrotech.dense_redstone_ore", properties);
        this.particleCount = particleCount;
        this.proximityRepairAmount = proximityRepairAmount;
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    /** How much durability a redstone tool near this ore recovers: 1, 2, or 3 by size. */
    public int getProximityRepairAmount() {
        return this.proximityRepairAmount;
    }

    // -- Activation ----------------------------------------------------------

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        this.activate(state, level, pos);
        super.attack(state, level, pos, player);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        this.activate(state, level, pos);
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        this.activate(state, level, pos);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        this.activate(state, level, pos);
        return InteractionResult.PASS;
    }

    private void activate(BlockState state, Level level, BlockPos pos) {
        if (level.isClientSide) {
            this.spawnParticles(level, pos);
            return;
        }
        if (!state.getValue(LIT)) {
            level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
            playSound(level, pos);
        } else if (level.random.nextFloat() < 0.25) {
            playSound(level, pos);
        }
    }

    private static void playSound(Level level, BlockPos pos) {
        level.playSound(null, pos, CoreSounds.randomDenseRedstoneOreActivate(level.random), SoundSource.BLOCKS, 1, 1);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(LIT);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) {
            level.setBlock(pos, state.setValue(LIT, false), Block.UPDATE_ALL);
        }
    }

    // -- Particles -----------------------------------------------------------

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) {
            this.spawnParticles(level, pos);
        }
    }

    /** The 1.12 loop: the first six particles go to the open faces, the rest anywhere inside. */
    private void spawnParticles(Level level, BlockPos pos) {
        RandomSource random = level.random;
        for (int i = 0; i < this.particleCount; i++) {
            double x = pos.getX() + random.nextFloat();
            double y = pos.getY() + random.nextFloat();
            double z = pos.getZ() + random.nextFloat();
            if (i == 0 && !isSolidRender(level, pos.above())) {
                y = pos.getY() + 1.0625;
            }
            if (i == 1 && !isSolidRender(level, pos.below())) {
                y = pos.getY() - 0.0625;
            }
            if (i == 2 && !isSolidRender(level, pos.south())) {
                z = pos.getZ() + 1.0625;
            }
            if (i == 3 && !isSolidRender(level, pos.north())) {
                z = pos.getZ() - 0.0625;
            }
            if (i == 4 && !isSolidRender(level, pos.east())) {
                x = pos.getX() + 1.0625;
            }
            if (i == 5 && !isSolidRender(level, pos.west())) {
                x = pos.getX() - 0.0625;
            }
            if (x < pos.getX() || x > pos.getX() + 1 || y < 0 || y > pos.getY() + 1 || z < pos.getZ() || z > pos.getZ() + 1) {
                level.addParticle(DustParticleOptions.REDSTONE, x, y, z, 0, 0, 0);
            }
        }
    }

    private static boolean isSolidRender(Level level, BlockPos pos) {
        return level.getBlockState(pos).isSolidRender(level, pos);
    }
}
