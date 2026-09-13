package com.moostoet.pyrotech.core;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.library.fluid.PyrotechFluids;
import com.moostoet.pyrotech.library.fluid.PyrotechFluids.Entry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;

/**
 * Core's fluids: liquid clay and the three wines, each a {@link PyrotechFluids} entry with
 * its 1.12 density and viscosity.
 */
public final class CoreFluids {

    public static final DeferredRegister<FluidType> FLUID_TYPES =
        DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Pyrotech.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, Pyrotech.MOD_ID);

    public static final Entry LIQUID_CLAY = fluid("liquid_clay", 6000, 12000);
    public static final Entry PYROBERRY_WINE = fluid("pyroberry_wine", 1000, 1000);
    public static final Entry GLOAMBERRY_WINE = fluid("gloamberry_wine", 1000, 1000);
    public static final Entry FRECKLEBERRY_WINE = fluid("freckleberry_wine", 1000, 1000);

    public static final List<Entry> ALL = List.of(LIQUID_CLAY, PYROBERRY_WINE, GLOAMBERRY_WINE, FRECKLEBERRY_WINE);

    private CoreFluids() {
    }

    private static Entry fluid(String name, int density, int viscosity) {
        return PyrotechFluids.register(name, density, viscosity, FLUID_TYPES, FLUIDS, CoreBlocks.BLOCKS, CoreItems.ITEMS);
    }

    static void addToTab(CreativeModeTab.Output output) {
        for (Entry entry : ALL) {
            output.accept(entry.bucket().get());
        }
    }
}
