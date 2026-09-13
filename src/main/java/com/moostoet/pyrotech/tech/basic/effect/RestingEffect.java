package com.moostoet.pyrotech.tech.basic.effect;

import com.moostoet.pyrotech.tech.basic.TechBasicAttachments;
import com.moostoet.pyrotech.tech.basic.TechBasicConfig;
import com.moostoet.pyrotech.tech.basic.TechBasicEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Resting: a player standing by a lit campfire at night. Every 100 ticks it heals half a
 * heart, every 200 it rises a level to at most three, and at the third level it grants
 * well rested, and focused when comfort, well rested, and well fed are all present. The
 * ticks it has run live in the player's attachment, since an infinite instance has no
 * duration to count down from (tech/basic sign-off, item 2).
 */
public final class RestingEffect extends CampfireEffect {

    public static final int MAX_AMPLIFIER = 2;
    public static final int REGEN_INTERVAL_TICKS = 100;
    public static final int LEVEL_UP_INTERVAL_TICKS = 200;
    private static final float REGEN_HALF_HEARTS = 1;
    private static final int WELL_RESTED_DURATION_TICKS = 5 * 60 * 20;

    public RestingEffect() {
        super(false);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide || !(entity instanceof Player player)) {
            return true;
        }
        CampfireEffectData data = TechBasicAttachments.get(player);
        int ticks = data.restingTicks() + 1;
        data = data.withRestingTicks(ticks);
        if (ticks % REGEN_INTERVAL_TICKS == 0 && player.getHealth() < player.getMaxHealth()) {
            player.heal(REGEN_HALF_HEARTS);
        }
        if (ticks % LEVEL_UP_INTERVAL_TICKS == 0) {
            if (amplifier < MAX_AMPLIFIER) {
                addTo(player, amplifier + 1);
                data = data.withRestingTicks(0);
            } else {
                if (TechBasicConfig.COMMON.wellRestedEnabled.get()) {
                    player.addEffect(new MobEffectInstance(TechBasicEffects.WELL_RESTED, WELL_RESTED_DURATION_TICKS, 0, false, true));
                }
                data = data.withRestingTicks(0);
                if (TechBasicConfig.COMMON.focusedEnabled.get()
                    && player.hasEffect(TechBasicEffects.COMFORT)
                    && player.hasEffect(TechBasicEffects.WELL_RESTED)
                    && player.hasEffect(TechBasicEffects.WELL_FED)) {
                    player.addEffect(new MobEffectInstance(TechBasicEffects.FOCUSED, MobEffectInstance.INFINITE_DURATION, 0, false, true));
                    double maximum = TechBasicConfig.SERVER.focusedMaximumAccumulatedBonus.get();
                    double bonus = Math.max(0, TechBasicConfig.SERVER.focusedAccumulatedBonus.get());
                    data = data.withRemainingBonus(Math.min(maximum, data.remainingBonus() + bonus));
                }
            }
        }
        TechBasicAttachments.set(player, data);
        return true;
    }

    /** The 1.12 {@code PotionResting.addEffect}: an ambient, visible, endless instance at the given level. */
    public static void addTo(Player player, int amplifier) {
        player.addEffect(new MobEffectInstance(TechBasicEffects.RESTING, MobEffectInstance.INFINITE_DURATION, amplifier, true, true));
    }

    /** Back to level one with the counter zeroed, as the 1.12 re-add reset the duration. True when it was above level one. */
    public static boolean reset(Player player) {
        MobEffectInstance resting = player.getEffect(TechBasicEffects.RESTING);
        if (resting == null || resting.getAmplifier() == 0) {
            return false;
        }
        player.removeEffect(TechBasicEffects.RESTING);
        addTo(player, 0);
        TechBasicAttachments.set(player, TechBasicAttachments.get(player).withRestingTicks(0));
        return true;
    }
}
