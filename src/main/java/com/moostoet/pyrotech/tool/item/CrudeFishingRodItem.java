package com.moostoet.pyrotech.tool.item;

import com.moostoet.pyrotech.core.item.DurabilityTooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * The crude fishing rod: sixteen uses, no enchanting, and a chance to snap on every
 * retrieve that grows with its wear, up to 65 percent at the last use. 1.12 rolled that in
 * its right-click; the damage hook is where the retrieve lands now.
 */
public class CrudeFishingRodItem extends FishingRodItem {

    private static final float BREAK_CHANCE = 0.65f;

    public CrudeFishingRodItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getEnchantmentValue() {
        return 0;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
        float breakChance = stack.getDamageValue() / (float) stack.getMaxDamage() * BREAK_CHANCE;
        if (entity != null && entity.getRandom().nextFloat() < breakChance) {
            return stack.getMaxDamage() + 1;
        }
        return amount;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        DurabilityTooltip.appendFull(stack, tooltip);
    }
}
