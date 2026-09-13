package com.moostoet.pyrotech.tool.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;

/** The crude and durable shields: vanilla's blocking, with nothing to repair them by (1.12 set no repair item). */
public class PyrotechShieldItem extends ShieldItem {

    public PyrotechShieldItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return false;
    }
}
