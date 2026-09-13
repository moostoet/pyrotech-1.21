package com.moostoet.pyrotech.tech.basic.client;

import com.moostoet.pyrotech.tech.basic.block.entity.AnvilBlockEntity;
import com.moostoet.pyrotech.tech.basic.network.AnvilHitPayload;
import com.moostoet.pyrotech.tech.basic.recipe.ExtendedAnvilRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;

/** The crack particles of an anvil hit, and the extended recipe's own hit hook. */
public final class AnvilHitParticles {

    private AnvilHitParticles() {
    }

    public static void spawn(AnvilHitPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !(level.getBlockEntity(payload.pos()) instanceof AnvilBlockEntity anvil)) {
            return;
        }
        Vec3 hit = payload.hit();
        BlockParticleOption particle = new BlockParticleOption(ParticleTypes.BLOCK, anvil.getBlockState());
        for (int i = 0; i < 8; i++) {
            level.addParticle(particle, hit.x, hit.y, hit.z, 0, 0, 0);
        }
        anvil.clientRecipe().ifPresent(recipe -> {
            if (recipe instanceof ExtendedAnvilRecipe extended) {
                extended.onAnvilHitClient(level, anvil, hit);
            }
        });
    }
}
