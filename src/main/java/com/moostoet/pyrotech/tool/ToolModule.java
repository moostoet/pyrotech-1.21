package com.moostoet.pyrotech.tool;

import com.moostoet.pyrotech.core.CoreModule;
import com.moostoet.pyrotech.tool.recipe.ToolRecipeSerializers;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.level.block.DispenserBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/**
 * The tool module: the stone-age tools, the shears, the shields, the crude fishing rod,
 * and the repair kits. Registers on the mod bus once, from the mod constructor.
 */
public final class ToolModule {

    private ToolModule() {
    }

    public static void register(IEventBus modEventBus) {
        ToolItems.ITEMS.register(modEventBus);
        ToolComponents.COMPONENTS.register(modEventBus);
        ToolRecipeSerializers.RECIPE_SERIALIZERS.register(modEventBus);
        modEventBus.addListener(ToolModule::commonSetup);
        modEventBus.addListener(ToolModule::addToTab);
    }

    /** A dispenser equips the shields as it equips vanilla's; 1.12 registered that in the shield constructor. */
    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            DispenserBlock.registerBehavior(ToolItems.CRUDE_SHIELD.get(), ArmorItem.DISPENSE_ITEM_BEHAVIOR);
            DispenserBlock.registerBehavior(ToolItems.DURABLE_SHIELD.get(), ArmorItem.DISPENSE_ITEM_BEHAVIOR);
        });
    }

    private static void addToTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CoreModule.TAB.getKey())) {
            ToolItems.addToTab(event);
        }
    }
}
