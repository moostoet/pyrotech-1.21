package com.moostoet.pyrotech.hunting;

import com.moostoet.pyrotech.Pyrotech;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** What hunting's items carry. */
public final class HuntingComponents {

    public static final DeferredRegister.DataComponents COMPONENTS =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Pyrotech.MOD_ID);

    /** The ticks a scraped hide has soaked, written every ten seconds so a picked-up hide keeps its progress. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SOAK_TICKS =
        COMPONENTS.registerComponentType("soak_ticks",
            builder -> builder.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    private HuntingComponents() {
    }
}
