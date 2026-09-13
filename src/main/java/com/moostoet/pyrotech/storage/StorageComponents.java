package com.moostoet.pyrotech.storage;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.storage.item.BagContents;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** What the tank and bag items carry: the 1.12 stack tags as data components, copied by the loot tables. */
public final class StorageComponents {

    public static final DeferredRegister.DataComponents COMPONENTS =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Pyrotech.MOD_ID);

    /** A tank's fluid, the component its item fluid handler reads and writes. Absent when empty. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> TANK_FLUID =
        COMPONENTS.registerComponentType("tank_fluid",
            builder -> builder.persistent(SimpleFluidContent.CODEC).networkSynchronized(SimpleFluidContent.STREAM_CODEC));

    /** A bag's stacks with their large counts (storage sign-off, item 1). The stream codec derives from the codec. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BagContents>> BAG_CONTENTS =
        COMPONENTS.registerComponentType("bag_contents", builder -> builder.persistent(BagContents.CODEC));

    private StorageComponents() {
    }
}
