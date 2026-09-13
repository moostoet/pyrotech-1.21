package com.moostoet.pyrotech.bucket;

import com.moostoet.pyrotech.Pyrotech;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The bucket state that 1.12 kept as the {@code fluids} and {@code durability} NBT tags. */
public final class BucketComponents {

    public static final DeferredRegister.DataComponents COMPONENTS =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Pyrotech.MOD_ID);

    /** The held fluid, the component NeoForge's {@code FluidHandlerItemStack} reads and writes. Absent when empty. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> FLUID =
        COMPONENTS.registerComponentType("fluid",
            builder -> builder.persistent(SimpleFluidContent.CODEC).networkSynchronized(SimpleFluidContent.STREAM_CODEC));

    /**
     * The uses left (bucket sign-off, item 3). Absent while the bucket is at its tier's
     * maximum, so a fresh bucket and a fully used-up-and-back one stack together.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> USES =
        COMPONENTS.registerComponentType("uses",
            builder -> builder.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    private BucketComponents() {
    }
}
