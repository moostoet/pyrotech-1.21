package com.moostoet.pyrotech.tool.item;

import com.moostoet.pyrotech.core.item.DurabilityTooltip;
import com.moostoet.pyrotech.tool.client.ShiftKey;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** The overrides the five tool classes share, for a tool with or without an active behaviour. */
final class ActiveTools {

    /** The 1.12 tooltip colours: gold text with yellow highlights. */
    static final String INFO = ChatFormatting.GOLD.toString();
    static final String HIGHLIGHT = ChatFormatting.YELLOW.toString();

    private ActiveTools() {
    }

    /**
     * The 1.12 diggers scaled their efficiency, which only counts on blocks they are made
     * for; the swords scaled whatever the speed was.
     */
    static float destroySpeed(@Nullable ActiveBehaviour behaviour, ItemStack stack, float speed, boolean effectiveOnly) {
        if (behaviour == null || !behaviour.isActive(stack) || (effectiveOnly && speed <= 1.0f)) {
            return speed;
        }
        return speed * behaviour.speedScalar();
    }

    static void inventoryTick(@Nullable ActiveBehaviour behaviour, ItemStack stack, Level level, Entity holder) {
        if (behaviour != null) {
            behaviour.inventoryTick(stack, level, holder);
        }
    }

    static int damageItem(@Nullable ActiveBehaviour behaviour, ItemStack stack, int amount, @Nullable LivingEntity holder) {
        return behaviour == null ? amount : behaviour.damageItem(stack, amount, holder);
    }

    static void appendHoverText(@Nullable ActiveBehaviour behaviour, ItemStack stack, List<Component> tooltip, ActiveBehaviour.ToolKind kind) {
        if (behaviour != null) {
            if (FMLEnvironment.dist == Dist.CLIENT && ShiftKey.isDown()) {
                behaviour.appendTooltip(tooltip, kind);
            } else {
                tooltip.add(Component.translatable("gui.pyrotech.tooltip.extended.shift", INFO, ChatFormatting.GRAY.toString())
                    .withStyle(ChatFormatting.GRAY));
            }
        }
        DurabilityTooltip.appendFull(stack, tooltip);
    }

    static Component line(String key, Object... args) {
        return Component.translatable(key, args).withStyle(ChatFormatting.GOLD);
    }

    static void notifyChanged(ItemStack stack) {
        if (stack.getItem() instanceof ActiveTool tool) {
            tool.onActiveChanged(stack);
        }
    }
}
