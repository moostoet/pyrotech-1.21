package com.moostoet.pyrotech.hunting.entity;

import com.moostoet.pyrotech.core.CoreBlocks;
import com.moostoet.pyrotech.hunting.HuntingTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldgenRandom;

/**
 * The animated mud: vanilla's slime that lands in mud rocks and dies into more. Splitting,
 * size, sounds, and goals are the slime's; the landing hook and the removal place the mud,
 * and the landing particles are mud rock bits.
 */
public final class MudEntity extends Slime {

    public MudEntity(EntityType<? extends MudEntity> type, Level level) {
        super(type, level);
    }

    /**
     * {@code Slime.checkSlimeSpawnRules} with the biome tag swapped for hunting's (sign-off,
     * item 1): the surface rule between y 50 and 70 on a moon roll and a light roll in the
     * tagged biomes, and the slime chunk rule below y 40 anywhere.
     */
    public static boolean checkMudSpawnRules(EntityType<MudEntity> type, ServerLevelAccessor level, MobSpawnType spawnType,
                                             BlockPos pos, RandomSource random) {
        if (MobSpawnType.isSpawner(spawnType)) {
            return checkMobSpawnRules(type, level, spawnType, pos, random);
        }
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        if (level.getBiome(pos).is(HuntingTags.Biomes.MUD_SPAWN_BIOMES)
            && pos.getY() > 50
            && pos.getY() < 70
            && random.nextFloat() < 0.5F
            && random.nextFloat() < level.getMoonBrightness()
            && level.getMaxLocalRawBrightness(pos) <= random.nextInt(8)) {
            return checkMobSpawnRules(type, level, spawnType, pos, random);
        }
        if (!(level instanceof WorldGenLevel worldGenLevel)) {
            return false;
        }
        ChunkPos chunk = new ChunkPos(pos);
        boolean slimeChunk = WorldgenRandom.seedSlimeChunk(chunk.x, chunk.z, worldGenLevel.getSeed(), 987234911L).nextInt(10) == 0;
        return random.nextInt(10) == 0 && slimeChunk && pos.getY() < 40 && checkMobSpawnRules(type, level, spawnType, pos, random);
    }

    @Override
    protected ParticleOptions getParticleType() {
        return new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(CoreBlocks.ROCK_MUD.get()));
    }

    /** The landing hook: the server places mud, and the client still gets the vanilla particle burst. */
    @Override
    protected boolean spawnCustomParticles() {
        if (!this.level().isClientSide) {
            this.spawnMud(this.blockPosition());
        }
        return false;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide && (reason == RemovalReason.KILLED || reason == RemovalReason.DISCARDED)) {
            this.spawnMud(this.blockPosition());
        }
        super.remove(reason);
    }

    /**
     * The 1.12 {@code spawnMud}: a sphere of one to size blocks around the mud; each air or
     * replaceable dry position over a sturdy top gets up to size half-chance rolls to become
     * a mud rock, a mud rock becomes a layer, and a layer turns dirt under it into mud a
     * quarter of the time.
     */
    private void spawnMud(BlockPos center) {
        Level level = this.level();
        int size = this.getSize();
        int range = 1 + this.random.nextInt(size);
        int rangeSquared = range * range;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-range, -range, -range), center.offset(range, range, range))) {
            if (pos.distSqr(center) > rangeSquared) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && !(state.canBeReplaced() && state.getFluidState().isEmpty())) {
                continue;
            }
            BlockPos below = pos.below();
            BlockState belowState = level.getBlockState(below);
            for (int i = 0; i < size; i++) {
                if (belowState.isFaceSturdy(level, below, Direction.UP) && this.random.nextFloat() < 0.5f) {
                    if (state.is(CoreBlocks.MUD_LAYER.get())) {
                        if (this.random.nextFloat() < 0.25f && belowState.is(BlockTags.DIRT)) {
                            level.setBlock(below, CoreBlocks.MUD.get().defaultBlockState(), Block.UPDATE_ALL);
                        }
                    } else if (state.is(CoreBlocks.ROCK_MUD.get())) {
                        level.setBlock(pos.immutable(), CoreBlocks.MUD_LAYER.get().defaultBlockState(), Block.UPDATE_ALL);
                    } else {
                        level.setBlock(pos.immutable(), CoreBlocks.ROCK_MUD.get().defaultBlockState(), Block.UPDATE_ALL);
                    }
                    break;
                }
            }
        }
    }
}
