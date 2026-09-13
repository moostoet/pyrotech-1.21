package com.moostoet.pyrotech.tool.item;

import com.moostoet.pyrotech.tool.ToolComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A quartz tool is active whenever it is in the Nether: it mines three times as fast and
 * the sword hits three times as hard (tool sign-off, items 2 and 10).
 */
public final class QuartzBehaviour implements ActiveBehaviour {

    public static final QuartzBehaviour INSTANCE = new QuartzBehaviour();

    static final float SPEED_SCALAR = 3.0f;
    static final float SWORD_DAMAGE_SCALAR = 3.0f;

    private QuartzBehaviour() {
    }

    @Override
    public boolean isActive(ItemStack stack) {
        return stack.getOrDefault(ToolComponents.QUARTZ_ACTIVE.get(), false);
    }

    @Override
    public float speedScalar() {
        return SPEED_SCALAR;
    }

    @Override
    public float swordDamageScalar() {
        return SWORD_DAMAGE_SCALAR;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity holder) {
        if (level.isClientSide) {
            return;
        }
        boolean nether = level.dimension() == Level.NETHER;
        if (nether == this.isActive(stack)) {
            return;
        }
        if (nether) {
            stack.set(ToolComponents.QUARTZ_ACTIVE.get(), true);
        } else {
            stack.remove(ToolComponents.QUARTZ_ACTIVE.get());
        }
        ActiveTools.notifyChanged(stack);
    }

    @Override
    public void appendTooltip(List<Component> tooltip, ToolKind kind) {
        tooltip.add(switch (kind) {
            case DIGGER -> ActiveTools.line("gui.pyrotech.tooltip.quartz.active.efficiency",
                ActiveTools.HIGHLIGHT, (int) (SPEED_SCALAR * 100), ActiveTools.INFO, ActiveTools.HIGHLIGHT, ActiveTools.INFO);
            case SWORD -> ActiveTools.line("gui.pyrotech.tooltip.quartz.active.damage",
                ActiveTools.HIGHLIGHT, (int) (SWORD_DAMAGE_SCALAR * 100), ActiveTools.INFO, ActiveTools.HIGHLIGHT, ActiveTools.INFO);
            case HOE -> ActiveTools.line("gui.pyrotech.tooltip.quartz.active.hoe",
                ActiveTools.HIGHLIGHT, ActiveTools.INFO, ActiveTools.HIGHLIGHT, ActiveTools.INFO);
        });
    }
}
