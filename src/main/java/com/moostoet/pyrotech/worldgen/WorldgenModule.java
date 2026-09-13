package com.moostoet.pyrotech.worldgen;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;

/**
 * The worldgen module: the eleven 1.12 chunk generators as worldgen data, three custom
 * feature types, and the toggles. Registers on the mod bus once, from the mod constructor.
 */
public final class WorldgenModule {

    private WorldgenModule() {
    }

    public static void register(IEventBus modEventBus, ModContainer modContainer) {
        WorldgenFeatures.FEATURES.register(modEventBus);
        WorldgenConditions.CONDITION_CODECS.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, WorldgenConfig.COMMON_SPEC, "pyrotech-worldgen.toml");
    }
}
