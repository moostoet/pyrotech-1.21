package com.moostoet.pyrotech.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;

/**
 * The 1.12 {@code SCPacketParticleCombust}: sixteen smoke, four large smoke, and sixteen
 * flame particles spread around a block's centre, sent to the players tracking it.
 */
public final class CombustParticles {

    private CombustParticles() {
    }

    public static void spawn(ServerLevel level, BlockPos pos, double range) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        level.sendParticles(ParticleTypes.SMOKE, x, y, z, 16, range, range, range, 0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 4, range, range, range, 0);
        level.sendParticles(ParticleTypes.FLAME, x, y, z, 16, range, range, range, 0);
    }
}
