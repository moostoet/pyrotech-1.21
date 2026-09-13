package com.moostoet.pyrotech.tech.basic.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.common.EffectCure;

import java.util.Set;

/**
 * The 1.12 {@code PotionCampfireBase}: a beneficial effect with a black liquid colour and
 * no instant form. The subclasses that tick say so; the rest do nothing per tick.
 */
public class CampfireEffect extends MobEffect {

    private static final int BLACK = 0xFF000000;

    private final boolean curable;

    public CampfireEffect(boolean curable) {
        super(MobEffectCategory.BENEFICIAL, BLACK);
        this.curable = curable;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return false;
    }

    /** Comfort and resting had no curative items in 1.12; the other three keep the milk default. */
    @Override
    public void fillEffectCures(Set<EffectCure> cures, MobEffectInstance effectInstance) {
        if (this.curable) {
            super.fillEffectCures(cures, effectInstance);
        }
    }
}
