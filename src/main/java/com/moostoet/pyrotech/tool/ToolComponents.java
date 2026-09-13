package com.moostoet.pyrotech.tool;

import com.moostoet.pyrotech.Pyrotech;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The active-tool state that 1.12 kept as NBT under a {@code Pyrotech} compound. */
public final class ToolComponents {

    public static final DeferredRegister.DataComponents COMPONENTS =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Pyrotech.MOD_ID);

    /**
     * The game time at which a redstone tool's ten seconds end (tool sign-off, item 3).
     * Absent while the tool is not active.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> REDSTONE_ACTIVE_UNTIL =
        COMPONENTS.registerComponentType("redstone_active_until",
            builder -> builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));

    /** Present, and true, while a quartz tool is in the Nether. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> QUARTZ_ACTIVE =
        COMPONENTS.registerComponentType("quartz_active",
            builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    private ToolComponents() {
    }
}
