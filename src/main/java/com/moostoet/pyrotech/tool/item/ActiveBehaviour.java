package com.moostoet.pyrotech.tool.item;

import com.moostoet.pyrotech.Pyrotech;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * What makes a redstone or quartz tool an active tool: when it is active, how much faster
 * it mines and how much harder its sword hits, what happens to it each inventory tick and
 * when it takes damage, and the lines behind shift in its tooltip. One instance per kind;
 * the 1.12 delegates and marker interfaces as one shape.
 */
public interface ActiveBehaviour {

    /** The model predicate that swaps in the {@code _active} texture. */
    ResourceLocation MODEL_PREDICATE = ResourceLocation.fromNamespaceAndPath(Pyrotech.MOD_ID, "active");

    boolean isActive(ItemStack stack);

    float speedScalar();

    float swordDamageScalar();

    void inventoryTick(ItemStack stack, Level level, Entity holder);

    /** The damage the tool takes when {@code amount} is asked of it. */
    default int damageItem(ItemStack stack, int amount, @Nullable LivingEntity holder) {
        return amount;
    }

    void appendTooltip(List<Component> tooltip, ToolKind kind);

    /** Which last tooltip line a tool shows: its block speed, its sword damage, or its hoe area. */
    enum ToolKind {
        DIGGER,
        SWORD,
        HOE
    }
}
