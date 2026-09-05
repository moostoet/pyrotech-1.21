package com.moostoet.pyrotech.core.item;

import com.moostoet.pyrotech.core.CoreConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.List;

/**
 * The full-durability tooltip line every durable Pyrotech tool shows, behind the
 * {@code SHOW_DURABILITY_TOOLTIPS} client flag (tool sign-off, item 4). Items call it from
 * {@code appendHoverText}; it does nothing on a dedicated server, where no client config exists.
 */
public final class DurabilityTooltip {

    private DurabilityTooltip() {
    }

    public static void appendFull(ItemStack stack, List<Component> tooltip) {
        if (FMLEnvironment.dist != Dist.CLIENT || !CoreConfig.CLIENT.showDurabilityTooltips.get()) {
            return;
        }
        if (stack.getDamageValue() == 0) {
            tooltip.add(Component.translatable("gui.pyrotech.tooltip.durability.full", stack.getMaxDamage()));
        }
    }
}
