package com.moostoet.pyrotech.tech.basic.event;

import com.moostoet.pyrotech.tech.basic.TechBasicConfig;
import com.moostoet.pyrotech.tech.basic.TechBasicEffects;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Comfort: a player by a campfire can eat when full, and every meal feeds a little more.
 * The 1.12 {@code CampfireComfortEffectEventHandler}.
 */
public final class ComfortHandler {

    private ComfortHandler() {
    }

    /** {@code Item#use} refuses food to a full player; with comfort, the use starts anyway. */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food == null || food.canAlwaysEat() || player.canEat(false) || !player.hasEffect(TechBasicEffects.COMFORT)) {
            return;
        }
        player.startUsingItem(event.getHand());
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onFinishUsing(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide || !player.hasEffect(TechBasicEffects.COMFORT)) {
            return;
        }
        FoodProperties food = event.getItem().get(DataComponents.FOOD);
        if (food == null || food.nutrition() <= 0) {
            return;
        }
        float saturationModifier = food.saturation() / (2f * food.nutrition());
        int bonusFood = (int) (food.nutrition() * TechBasicConfig.SERVER.comfortHungerModifier.get());
        float bonusSaturation = (float) (saturationModifier * TechBasicConfig.SERVER.comfortSaturationModifier.get());
        player.getFoodData().eat(bonusFood, bonusSaturation);
    }
}
