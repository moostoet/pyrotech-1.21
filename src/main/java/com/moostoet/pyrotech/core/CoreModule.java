package com.moostoet.pyrotech.core;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.core.event.StrawBedHandler;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

import java.util.function.Supplier;

/**
 * The core module: everything the other modules build on. Registers on the mod bus
 * once, from the mod constructor.
 */
public final class CoreModule {

    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Pyrotech.MOD_ID);

    /**
     * The one Pyrotech creative tab. The 1.12 icon was the campfire, which tech/basic
     * brings; until then the crude hammer stands in.
     */
    public static final Supplier<CreativeModeTab> TAB = CREATIVE_TABS.register("pyrotech", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.pyrotech"))
        .icon(() -> new ItemStack(CoreItems.CRUDE_HAMMER.get()))
        .displayItems((parameters, output) -> {
            CoreBlocks.addToTab(output);
            CoreItems.addToTab(output);
        })
        .build());

    private CoreModule() {
    }

    public static void register(IEventBus modEventBus, ModContainer modContainer) {
        CoreBlocks.BLOCKS.register(modEventBus);
        CoreItems.ITEMS.register(modEventBus);
        CoreFluids.FLUID_TYPES.register(modEventBus);
        CoreFluids.FLUIDS.register(modEventBus);
        CoreEntities.ENTITY_TYPES.register(modEventBus);
        CoreBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        CoreSounds.SOUND_EVENTS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
        NeoForge.EVENT_BUS.register(StrawBedHandler.class);
        modEventBus.addListener(CoreModule::registerDataMapTypes);
        modContainer.registerConfig(ModConfig.Type.COMMON, CoreConfig.COMMON_SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, CoreConfig.CLIENT_SPEC);
    }

    private static void registerDataMapTypes(RegisterDataMapTypesEvent event) {
        event.register(ToolLevels.TOOL_LEVELS);
    }
}
