package com.moostoet.pyrotech.core.item;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A berry wine. Drinking it adds a minute of its effect on top of whatever remains, and
 * past two minutes the drinker is also sick with nausea for half the excess. The bottle
 * comes back through the food's container. Durations are the 1.12 config defaults, now
 * constants (core sign-off, item 3).
 */
public final class WineItem extends Item {

    private static final int EFFECT_DURATION_TICKS = 60 * 20;
    private static final int SICK_THRESHOLD_TICKS = 2 * 60 * 20;

    private final Holder<MobEffect> effect;
    private final boolean ignites;

    public WineItem(Holder<MobEffect> effect, boolean ignites, Properties properties) {
        super(properties);
        this.effect = effect;
        this.ignites = ignites;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (this.ignites) {
            entity.igniteForSeconds(5);
        }
        if (!level.isClientSide) {
            MobEffectInstance active = entity.getEffect(this.effect);
            int duration = EFFECT_DURATION_TICKS + (active != null ? active.getDuration() : 0);
            if (duration > SICK_THRESHOLD_TICKS) {
                entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, (duration - SICK_THRESHOLD_TICKS) / 2, 1));
            }
            entity.addEffect(new MobEffectInstance(this.effect, duration));
        }
        return result;
    }
}
