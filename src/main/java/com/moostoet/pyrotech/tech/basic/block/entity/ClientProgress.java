package com.moostoet.pyrotech.tech.basic.block.entity;

import com.moostoet.pyrotech.core.CoreConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * Athenaeum's {@code spawnProgressParticlesClient} for a block entity's own client ticker:
 * a few happy villager particles in a box around a point, behind the client flag.
 */
public final class ClientProgress {

    private static final double DRIFT = 0.02;

    private ClientProgress() {
    }

    public static void particles(Level level, int count, double x, double y, double z, double rangeX, double rangeY, double rangeZ) {
        if (!level.isClientSide || !CoreConfig.CLIENT.showRecipeProgressionParticles.get()) {
            return;
        }
        RandomSource random = level.random;
        for (int i = 0; i < count; i++) {
            level.addParticle(ParticleTypes.HAPPY_VILLAGER,
                x + (random.nextFloat() * 2 - 1) * rangeX,
                y + (random.nextFloat() * 2 - 1) * rangeY,
                z + (random.nextFloat() * 2 - 1) * rangeZ,
                random.nextGaussian() * DRIFT, random.nextGaussian() * DRIFT, random.nextGaussian() * DRIFT);
        }
    }
}
