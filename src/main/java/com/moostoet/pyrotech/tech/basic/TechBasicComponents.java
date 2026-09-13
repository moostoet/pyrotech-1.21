package com.moostoet.pyrotech.tech.basic;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.tech.basic.item.MarshmallowType;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * What the marshmallow items and the sealed barrel carry: the 1.12 stack tags as data
 * components. A stick
 * without a type holds a plain marshmallow; a stick without a roast-by time is not
 * roasting; an item without a roasted-at time was never roasted.
 */
public final class TechBasicComponents {

    public static final DeferredRegister.DataComponents COMPONENTS =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Pyrotech.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MarshmallowType>> MARSHMALLOW_TYPE =
        COMPONENTS.registerComponentType("marshmallow_type",
            builder -> builder.persistent(MarshmallowType.CODEC).networkSynchronized(MarshmallowType.STREAM_CODEC));
    /** The game time at which the marshmallow on the stick is done; set while the stick is held at a campfire. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> ROAST_BY =
        COMPONENTS.registerComponentType("roast_by",
            builder -> builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));
    /** The game time at which a marshmallow was roasted or burned; the roasted one's potency decays from it. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> ROASTED_AT =
        COMPONENTS.registerComponentType("roasted_at",
            builder -> builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));

    /** The fluid a sealed barrel carries as an item. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> BARREL_FLUID =
        COMPONENTS.registerComponentType("barrel_fluid",
            builder -> builder.persistent(SimpleFluidContent.CODEC).networkSynchronized(SimpleFluidContent.STREAM_CODEC));

    private TechBasicComponents() {
    }
}
