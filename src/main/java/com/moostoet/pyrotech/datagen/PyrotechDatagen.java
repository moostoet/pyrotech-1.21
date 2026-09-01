package com.moostoet.pyrotech.datagen;

import com.moostoet.pyrotech.Pyrotech;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = Pyrotech.MOD_ID)
public final class PyrotechDatagen {

    private PyrotechDatagen() {
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        // Recipe and loot table providers are added here as modules are ported.
    }
}
