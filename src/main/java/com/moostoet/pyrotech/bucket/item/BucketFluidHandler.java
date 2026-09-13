package com.moostoet.pyrotech.bucket.item;

import com.moostoet.pyrotech.bucket.BucketComponents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;

/**
 * A Pyrotech bucket's item fluid handler: one whole bucket in or out, nothing partial, as
 * the 1.12 wrapper had it. A real drain spends one use and the bucket is gone once that
 * was the last one (bucket sign-off, item 7); a simulated one costs nothing.
 */
public final class BucketFluidHandler extends FluidHandlerItemStack {

    private final PyrotechBucketItem bucket;

    public BucketFluidHandler(ItemStack container) {
        super(BucketComponents.FLUID, container, FluidType.BUCKET_VOLUME);
        this.bucket = (PyrotechBucketItem) container.getItem();
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return resource.getAmount() < this.capacity ? 0 : super.fill(resource, action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return maxDrain < this.capacity ? FluidStack.EMPTY : super.drain(maxDrain, action);
    }

    @Override
    protected void setContainerToEmpty() {
        if (!this.bucket.spendUse(this.container)) {
            this.container = ItemStack.EMPTY;
        }
    }
}
