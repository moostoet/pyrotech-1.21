package com.moostoet.pyrotech.library.fluid;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.function.Consumer;

/**
 * A tank that may refuse to hold a hot fluid: the shared 1.12 {@code Tank.fill} override of
 * the storage tanks and faucets, the barrel, the soaking pot, and the tar collectors. A
 * fluid at 450 or more is hot. A tank that does not hold hot fluids still takes the fill,
 * then hands the fluid to {@code onHotFluid}, whose owner breaks the block and spills it.
 * The check runs only on a real fill, never on a probe (storage sign-off, item 6).
 */
public class HotFluidTank extends FluidTank {

    public static final int HOT_TEMPERATURE = 450;

    private final boolean holdsHotFluids;
    private final Consumer<FluidStack> onHotFluid;

    public HotFluidTank(int capacity, boolean holdsHotFluids, Consumer<FluidStack> onHotFluid) {
        super(capacity);
        this.holdsHotFluids = holdsHotFluids;
        this.onHotFluid = onHotFluid;
    }

    public static boolean isHot(FluidStack stack) {
        return !stack.isEmpty() && stack.getFluidType().getTemperature(stack) >= HOT_TEMPERATURE;
    }

    public boolean holdsHotFluids() {
        return this.holdsHotFluids;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        int filled = super.fill(resource, action);
        if (action.execute() && !this.holdsHotFluids && isHot(resource)) {
            this.onHotFluid.accept(resource);
        }
        return filled;
    }
}
