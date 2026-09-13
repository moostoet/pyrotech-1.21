package com.moostoet.pyrotech.hunting;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.library.fluid.PyrotechFluids;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Tannin: the 1.12 fluid with its density and viscosity, and the bucket the sign-off gave it (item 4). */
public final class HuntingFluids {

    public static final DeferredRegister<FluidType> FLUID_TYPES =
        DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Pyrotech.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, Pyrotech.MOD_ID);

    public static final PyrotechFluids.Entry TANNIN =
        PyrotechFluids.register("tannin", 1000, 1000, FLUID_TYPES, FLUIDS, HuntingBlocks.BLOCKS, HuntingItems.ITEMS);

    private HuntingFluids() {
    }
}
