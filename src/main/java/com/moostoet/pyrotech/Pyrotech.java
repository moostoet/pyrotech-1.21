package com.moostoet.pyrotech;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(Pyrotech.MOD_ID)
public final class Pyrotech {

    public static final String MOD_ID = "pyrotech";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Pyrotech(IEventBus modEventBus) {
        // Module init hooks are wired here as modules are ported.

        // Throwaway prototype for wayfinder ticket 8. Delete with the prototype package.
        com.moostoet.pyrotech.prototype.campfire.PrototypeCampfire.register(modEventBus);
    }
}
