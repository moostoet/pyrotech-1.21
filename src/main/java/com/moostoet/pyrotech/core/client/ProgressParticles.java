package com.moostoet.pyrotech.core.client;

import com.moostoet.pyrotech.core.CoreConfig;
import com.moostoet.pyrotech.core.network.ProgressParticlesPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;

/** Athenaeum's {@code spawnProgressParticlesClient}: happy villager particles in the 1.12 spread, behind the client flag. */
public final class ProgressParticles {

    private static final int DEFAULT_COUNT = 15;
    private static final double RANGE_X = 0.5;
    private static final double RANGE_Y = 0.15;
    private static final double RANGE_Z = 0.5;
    private static final double DRIFT = 0.02;

    private ProgressParticles() {
    }

    public static void spawn(ProgressParticlesPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !CoreConfig.CLIENT.showRecipeProgressionParticles.get()) {
            return;
        }
        RandomSource random = level.random;
        int count = payload.count() == 0 ? DEFAULT_COUNT : payload.count();
        for (int i = 0; i < count; i++) {
            level.addParticle(ParticleTypes.HAPPY_VILLAGER,
                payload.x() + (random.nextFloat() * 2 - 1) * RANGE_X,
                payload.y() + (random.nextFloat() * 2 - 1) * RANGE_Y,
                payload.z() + (random.nextFloat() * 2 - 1) * RANGE_Z,
                random.nextGaussian() * DRIFT, random.nextGaussian() * DRIFT, random.nextGaussian() * DRIFT);
        }
    }
}
