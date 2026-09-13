package com.moostoet.pyrotech.tech.basic.event;

import com.moostoet.pyrotech.core.event.PlayerMovementTracker;
import com.moostoet.pyrotech.tech.basic.TechBasicEffects;
import com.moostoet.pyrotech.tech.basic.effect.RestingEffect;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Resting: moving, taking a hit, or landing one sets the rest back to its first level,
 * and a hit that spends the well-rested absorption ends well rested. The 1.12
 * {@code CampfireRestingEffectEventHandler}.
 */
public final class RestingHandler {

    private RestingHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide && player.hasEffect(TechBasicEffects.RESTING) && PlayerMovementTracker.ticksSinceLastMove(player) == 0) {
            RestingEffect.reset(player);
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof Player victim && !victim.level().isClientSide) {
            RestingEffect.reset(victim);
            if (victim.hasEffect(TechBasicEffects.WELL_RESTED) && victim.getAbsorptionAmount() <= 0) {
                victim.removeEffect(TechBasicEffects.WELL_RESTED);
            }
        }
        if (event.getSource().getEntity() instanceof Player attacker && !attacker.level().isClientSide) {
            RestingEffect.reset(attacker);
        }
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide) {
            RestingEffect.reset(player);
        }
    }
}
