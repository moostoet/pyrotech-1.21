package com.moostoet.pyrotech.hunting.event;

import com.moostoet.pyrotech.hunting.HuntingAttachments;
import com.moostoet.pyrotech.hunting.entity.StuckSpears;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/** The 1.12 {@code LivingDeathEventHandler}: the spears stuck in a dying entity fall out. */
public final class StuckSpearsHandler {

    private StuckSpearsHandler() {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) {
            return;
        }
        StuckSpears spears = StuckSpears.of(entity);
        if (spears.isEmpty()) {
            return;
        }
        for (ItemStack spear : spears.spears()) {
            Containers.dropItemStack(entity.level(), entity.getX(), entity.getY(), entity.getZ(), spear.copy());
        }
        entity.removeData(HuntingAttachments.STUCK_SPEARS);
    }
}
