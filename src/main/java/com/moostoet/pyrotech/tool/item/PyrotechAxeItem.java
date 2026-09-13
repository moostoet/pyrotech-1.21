package com.moostoet.pyrotech.tool.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/** A Pyrotech axe: vanilla's, with the durability tooltip and an optional active behaviour. */
public class PyrotechAxeItem extends AxeItem implements ActiveTool {

    @Nullable
    private final ActiveBehaviour active;

    public PyrotechAxeItem(Tier tier, @Nullable ActiveBehaviour active, Properties properties) {
        super(tier, properties);
        this.active = active;
    }

    @Override
    @Nullable
    public ActiveBehaviour activeBehaviour() {
        return this.active;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return ActiveTools.destroySpeed(this.active, stack, super.getDestroySpeed(stack, state), true);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        ActiveTools.inventoryTick(this.active, stack, level, entity);
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
        return ActiveTools.damageItem(this.active, stack, amount, entity);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ActiveTools.appendHoverText(this.active, stack, tooltip, ActiveBehaviour.ToolKind.DIGGER);
    }
}
