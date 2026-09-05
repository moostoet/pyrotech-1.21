package com.moostoet.pyrotech.core;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.core.advancement.PickupModItemTrigger;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Core's advancement triggers. */
public final class CoreTriggers {

    public static final DeferredRegister<CriterionTrigger<?>> TRIGGER_TYPES =
        DeferredRegister.create(Registries.TRIGGER_TYPE, Pyrotech.MOD_ID);

    public static final DeferredHolder<CriterionTrigger<?>, PickupModItemTrigger> PICKUP_MOD_ITEM =
        TRIGGER_TYPES.register("pickup_mod_item", PickupModItemTrigger::new);

    private CoreTriggers() {
    }
}
