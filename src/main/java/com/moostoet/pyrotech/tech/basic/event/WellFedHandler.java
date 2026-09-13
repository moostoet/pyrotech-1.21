package com.moostoet.pyrotech.tech.basic.event;

import com.moostoet.pyrotech.tech.basic.TechBasicConfig;
import com.moostoet.pyrotech.tech.basic.TechBasicEffects;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Well fed: eating to full saturation by a campfire grants it, and while it lasts the
 * player tires half as fast. The 1.12 {@code CampfireWellFedEffectEventHandler}.
 */
public final class WellFedHandler {

    public static final int DURATION_TICKS = 6000;
    private static final float FULL_SATURATION = 20;
    private static final Map<UUID, Float> LAST_EXHAUSTION = new HashMap<>();

    private WellFedHandler() {
    }

    @SubscribeEvent
    public static void onFinishUsing(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide
            || !TechBasicConfig.COMMON.wellFedEnabled.get() || !player.hasEffect(TechBasicEffects.COMFORT)
            || !event.getItem().has(DataComponents.FOOD)) {
            return;
        }
        if (player.getFoodData().getSaturationLevel() >= FULL_SATURATION) {
            player.addEffect(new MobEffectInstance(TechBasicEffects.WELL_FED, DURATION_TICKS, 0, true, true));
        }
    }

    /** Any exhaustion gained since the last tick is halved, as the 1.12 reflection hack did. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        FoodData food = player.getFoodData();
        float exhaustion = food.getExhaustionLevel();
        if (player.hasEffect(TechBasicEffects.WELL_FED)) {
            float last = LAST_EXHAUSTION.getOrDefault(player.getUUID(), exhaustion);
            float gained = exhaustion - last;
            if (gained > 0) {
                exhaustion = last + (float) (gained * TechBasicConfig.SERVER.wellFedExhaustionModifier.get());
                food.setExhaustion(exhaustion);
            }
        }
        LAST_EXHAUSTION.put(player.getUUID(), exhaustion);
    }
}
