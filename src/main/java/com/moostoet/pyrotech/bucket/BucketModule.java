package com.moostoet.pyrotech.bucket;

import com.moostoet.pyrotech.bucket.item.BucketFluidHandler;
import com.moostoet.pyrotech.bucket.item.PyrotechBucketItem;
import com.moostoet.pyrotech.core.CoreModule;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * The bucket module: the wood, clay, stone, and refractory buckets and the two unfired
 * ones. Registers on the mod bus once, from the mod constructor, which is also where
 * NeoForge's milk fluid has to be switched on (bucket sign-off, item 1).
 */
public final class BucketModule {

    private BucketModule() {
    }

    public static void register(IEventBus modEventBus, ModContainer modContainer) {
        NeoForgeMod.enableMilkFluid();
        BucketItems.ITEMS.register(modEventBus);
        BucketComponents.COMPONENTS.register(modEventBus);
        modEventBus.addListener(BucketModule::registerCapabilities);
        modEventBus.addListener(BucketModule::addToTab);
        modContainer.registerConfig(ModConfig.Type.COMMON, BucketConfig.COMMON_SPEC, "pyrotech-bucket.toml");
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        for (DeferredItem<PyrotechBucketItem> bucket : BucketItems.BUCKETS) {
            event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> new BucketFluidHandler(stack), bucket.get());
        }
    }

    private static void addToTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CoreModule.TAB.getKey())) {
            BucketItems.addToTab(event);
        }
    }
}
