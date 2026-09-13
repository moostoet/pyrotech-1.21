package com.moostoet.pyrotech;

import com.moostoet.pyrotech.bucket.BucketModule;
import com.moostoet.pyrotech.core.CoreModule;
import com.moostoet.pyrotech.hunting.HuntingModule;
import com.moostoet.pyrotech.storage.StorageModule;
import com.moostoet.pyrotech.tech.basic.TechBasicModule;
import com.moostoet.pyrotech.tool.ToolModule;
import com.moostoet.pyrotech.worldgen.WorldgenModule;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(Pyrotech.MOD_ID)
public final class Pyrotech {

    public static final String MOD_ID = "pyrotech";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Pyrotech(IEventBus modEventBus, ModContainer modContainer) {
        CoreModule.register(modEventBus, modContainer);
        ToolModule.register(modEventBus);
        WorldgenModule.register(modEventBus, modContainer);
        BucketModule.register(modEventBus, modContainer);
        StorageModule.register(modEventBus, modContainer);
        HuntingModule.register(modEventBus, modContainer);
        TechBasicModule.register(modEventBus, modContainer);
    }
}
