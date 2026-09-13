package com.moostoet.pyrotech.storage;

import com.moostoet.pyrotech.core.CoreModule;
import com.moostoet.pyrotech.storage.block.TankBlock;
import com.moostoet.pyrotech.storage.event.BagPickupHandler;
import com.moostoet.pyrotech.storage.item.TankBlockItem;
import com.moostoet.pyrotech.storage.recipe.StorageRecipeSerializers;
import net.minecraft.core.Direction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * The storage module: the stash, shelf, crate, and wood rack, the two rock bags, and the
 * stone and refractory tanks and faucets. Registers on the mod bus once, from the mod
 * constructor.
 */
public final class StorageModule {

    private StorageModule() {
    }

    public static void register(IEventBus modEventBus, ModContainer modContainer) {
        StorageBlocks.BLOCKS.register(modEventBus);
        StorageBlocks.ITEMS.register(modEventBus);
        StorageBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        StorageComponents.COMPONENTS.register(modEventBus);
        StorageRecipeSerializers.RECIPE_SERIALIZERS.register(modEventBus);
        modEventBus.addListener(StorageModule::registerCapabilities);
        modEventBus.addListener(StorageModule::addToTab);
        NeoForge.EVENT_BUS.register(BagPickupHandler.class);
        modContainer.registerConfig(ModConfig.Type.COMMON, StorageConfig.COMMON_SPEC, "pyrotech-storage.toml");
    }

    /** Every inventory is automatable, the bag from below only, as 1.12 had it with {@code ALLOW_AUTOMATION} on. */
    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, StorageBlockEntities.STASH.get(), (stash, side) -> stash.handler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, StorageBlockEntities.SHELF.get(), (shelf, side) -> shelf.handler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, StorageBlockEntities.CRATE.get(), (crate, side) -> crate.handler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, StorageBlockEntities.WOOD_RACK.get(), (rack, side) -> rack.handler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, StorageBlockEntities.BAG.get(),
            (bag, side) -> side == Direction.DOWN ? bag.handler() : null);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, StorageBlockEntities.TANK.get(), (tank, side) -> tank.groupHandler());
        for (DeferredItem<TankBlockItem> item : StorageBlocks.TANK_ITEMS) {
            int capacity = ((TankBlock) item.get().getBlock()).capacity();
            event.registerItem(Capabilities.FluidHandler.ITEM,
                (stack, context) -> new FluidHandlerItemStack(StorageComponents.TANK_FLUID, stack, capacity), item.get());
        }
    }

    private static void addToTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CoreModule.TAB.getKey())) {
            StorageBlocks.addToTab(event);
        }
    }
}
