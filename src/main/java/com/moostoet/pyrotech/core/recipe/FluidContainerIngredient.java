package com.moostoet.pyrotech.core.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.common.util.NeoForgeExtraCodecs;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;

import java.util.stream.Stream;

/**
 * A crafting ingredient matched by the fluid an item holds, not by the item: the 1.12
 * {@code pyrotech:fluid} ingredient. Any item whose fluid handler can give at least the
 * amount of the fluid matches, so a vanilla bucket, a Pyrotech bucket, or anything else
 * that carries the fluid serves the recipe. The item's own crafting remainder is what
 * stays in the grid. The displayed stack is the fluid's bucket.
 */
public record FluidContainerIngredient(Holder<Fluid> fluid, int amount) implements ICustomIngredient {

    public static final MapCodec<FluidContainerIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        BuiltInRegistries.FLUID.holderByNameCodec().fieldOf("fluid").forGetter(FluidContainerIngredient::fluid),
        NeoForgeExtraCodecs.optionalFieldAlwaysWrite(ExtraCodecs.POSITIVE_INT, "amount", FluidType.BUCKET_VOLUME)
            .forGetter(FluidContainerIngredient::amount)
    ).apply(instance, FluidContainerIngredient::new));

    /** One bucket of the fluid. */
    public static Ingredient of(Fluid fluid) {
        return new FluidContainerIngredient(fluid.builtInRegistryHolder(), FluidType.BUCKET_VOLUME).toVanilla();
    }

    @Override
    public boolean test(ItemStack stack) {
        return FluidUtil.getFluidContained(stack)
            .filter(contained -> contained.is(this.fluid) && contained.getAmount() >= this.amount)
            .isPresent();
    }

    @Override
    public Stream<ItemStack> getItems() {
        ItemStack bucket = FluidUtil.getFilledBucket(new FluidStack(this.fluid, this.amount));
        return bucket.isEmpty() ? Stream.empty() : Stream.of(bucket);
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return CoreIngredientTypes.FLUID_CONTAINER.get();
    }
}
