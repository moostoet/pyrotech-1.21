package com.moostoet.pyrotech.tech.basic.client;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.core.CoreConfig;
import com.moostoet.pyrotech.tech.basic.recipe.CompostValues;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** The 1.12 "Compost Value: n" line, second in the tooltip, behind core's client flag. */
@EventBusSubscriber(modid = Pyrotech.MOD_ID, value = Dist.CLIENT)
public final class CompostValueTooltipHandler {

    private CompostValueTooltipHandler() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (!CoreConfig.CLIENT.showCompostValueInTooltips.get() || Minecraft.getInstance().level == null) {
            return;
        }
        CompostValues.of(Minecraft.getInstance().level, event.getItemStack()).ifPresent(entry -> {
            Component line = Component.translatable("gui.pyrotech.tooltip.compost.value", entry.value()).withStyle(ChatFormatting.GRAY);
            event.getToolTip().add(Math.min(1, event.getToolTip().size()), line);
        });
    }
}
