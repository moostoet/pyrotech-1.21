package com.moostoet.pyrotech.tech.basic.item;

import com.moostoet.pyrotech.tech.basic.TechBasicComponents;
import com.moostoet.pyrotech.tech.basic.TechBasicItems;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** The 1.12 {@code ItemMarshmallow} statics: the speed effect, the roasted potency, and the shared cooldown. */
public final class Marshmallows {

    public static final int COOLDOWN_TICKS = 10;
    public static final int PLAIN_SPEED_TICKS = 100;
    public static final int MAX_PLAIN_SPEED_TICKS = 100;
    public static final int ROASTED_SPEED_TICKS = 500;
    public static final int MAX_ROASTED_SPEED_TICKS = 6000;
    public static final int BURNED_SLOW_TICKS = 100;
    public static final int POTENCY_DURATION_TICKS = 600;

    private Marshmallows() {
    }

    /** Adds {@code duration} to an existing effect of the kind up to {@code max}, or starts it, as 1.12 stacked speed. */
    public static void applyEffect(LivingEntity entity, Holder<MobEffect> effect, int duration, int max, boolean stack) {
        MobEffectInstance existing = entity.getEffect(effect);
        int total = duration;
        if (existing != null && stack) {
            total = Math.min(max, existing.getDuration() + duration);
        } else if (existing != null) {
            total = Math.max(existing.getDuration(), duration);
        }
        entity.addEffect(new MobEffectInstance(effect, total, 0, false, false));
    }

    /** Two right after roasting, down to one after thirty seconds. */
    public static double potency(Level level, long roastedAt) {
        if (roastedAt <= 0) {
            return 1;
        }
        long elapsed = level.getGameTime() - roastedAt;
        return Math.max(0, 1 - elapsed / (double) POTENCY_DURATION_TICKS) + 1;
    }

    public static long roastedAt(ItemStack stack) {
        Long roastedAt = stack.get(TechBasicComponents.ROASTED_AT.get());
        return roastedAt == null ? 0 : roastedAt;
    }

    public static void cooldown(Player player) {
        player.getCooldowns().addCooldown(TechBasicItems.MARSHMALLOW.get(), COOLDOWN_TICKS);
        player.getCooldowns().addCooldown(TechBasicItems.MARSHMALLOW_ROASTED.get(), COOLDOWN_TICKS);
        player.getCooldowns().addCooldown(TechBasicItems.MARSHMALLOW_BURNED.get(), COOLDOWN_TICKS);
        player.getCooldowns().addCooldown(TechBasicItems.MARSHMALLOW_STICK.get(), COOLDOWN_TICKS);
        player.getCooldowns().addCooldown(TechBasicItems.MARSHMALLOW_STICK_EMPTY.get(), COOLDOWN_TICKS);
    }
}
