package com.moostoet.pyrotech.core.client;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.core.CoreConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

/**
 * The burn-time tooltip, the 1.12 {@code TooltipEventHandler.BurnTime}: a fuel shows its
 * furnace burn time as minutes and seconds under its name, behind the
 * {@code SHOW_BURN_TIME_IN_TOOLTIPS} client flag. The time comes from the furnace fuels data map.
 */
@EventBusSubscriber(modid = Pyrotech.MOD_ID, value = Dist.CLIENT)
public final class BurnTimeTooltipHandler {

    private BurnTimeTooltipHandler() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (!CoreConfig.CLIENT.showBurnTimeInTooltips.get()) {
            return;
        }
        int burnTime = event.getItemStack().getBurnTime(RecipeType.SMELTING);
        if (burnTime <= 0) {
            return;
        }
        List<Component> lines = event.getToolTip();
        Component line = Component.translatable("gui.pyrotech.tooltip.burn.time", ticksToHms(burnTime));
        lines.add(lines.size() > 1 ? 1 : lines.size(), line);
    }

    private static String ticksToHms(int ticks) {
        int totalSeconds = ticks / 20;
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        return hours > 0
            ? String.format("%02d:%02d:%02d", hours, minutes, seconds)
            : String.format("%02d:%02d", minutes, seconds);
    }
}
