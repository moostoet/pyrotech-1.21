package com.moostoet.pyrotech.core.event;

import com.moostoet.pyrotech.core.CoreConfig;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.animal.Sheep;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/** The wool tweak: a killed sheep drops no wool while {@code PREVENT_WOOL_ON_SHEEP_DEATH} is on. */
public final class SheepDropsHandler {

    private SheepDropsHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof Sheep && CoreConfig.COMMON.preventWoolOnSheepDeath.get()) {
            event.getDrops().removeIf(drop -> drop.getItem().is(ItemTags.WOOL));
        }
    }
}
