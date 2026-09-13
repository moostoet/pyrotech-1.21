package com.moostoet.pyrotech.library.fluid;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * A fluid handler that drains but never fills: the 1.12 {@code setCanFill(false)} on a
 * capability path, for a block whose own code still fills the tank behind it (the
 * refractory sign-off's tar drain).
 */
public record ReadOnlyFluidHandler(IFluidHandler handler) implements IFluidHandler {

    @Override
    public int getTanks() {
        return this.handler.getTanks();
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return this.handler.getFluidInTank(tank);
    }

    @Override
    public int getTankCapacity(int tank) {
        return this.handler.getTankCapacity(tank);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return false;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return 0;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return this.handler.drain(resource, action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return this.handler.drain(maxDrain, action);
    }
}
