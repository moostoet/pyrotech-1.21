package com.moostoet.pyrotech.library.fluid;

import com.moostoet.pyrotech.Pyrotech;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * A Pyrotech fluid: a fluid type, a source and a flowing fluid, a liquid block, and a
 * bucket, registered into the owning unit's registers. 1.12 built every module fluid on
 * {@code Material.WATER}, so each behaves as water does: it puts out fire, wets farmland,
 * carries boats, and resets fall distance. Density and viscosity are the 1.12 values, and
 * the flow tick rate is 1.12's viscosity over 200.
 */
public final class PyrotechFluids {

    private PyrotechFluids() {
    }

    /** One registered fluid and everything that hangs off it. The textures are the migrated 1.12 ones. */
    public record Entry(String name,
                        DeferredHolder<FluidType, FluidType> type,
                        DeferredHolder<Fluid, BaseFlowingFluid.Source> source,
                        DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing,
                        DeferredBlock<LiquidBlock> block,
                        DeferredItem<BucketItem> bucket) {

        public ResourceLocation stillTexture() {
            return ResourceLocation.fromNamespaceAndPath(Pyrotech.MOD_ID, "block/fluid_" + this.name + "_still");
        }

        public ResourceLocation flowingTexture() {
            return ResourceLocation.fromNamespaceAndPath(Pyrotech.MOD_ID, "block/fluid_" + this.name + "_flow");
        }
    }

    public static Entry register(String name, int density, int viscosity, DeferredRegister<FluidType> types,
                                 DeferredRegister<Fluid> fluids, DeferredRegister.Blocks blocks, DeferredRegister.Items items) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Pyrotech.MOD_ID, name);
        DeferredHolder<FluidType, FluidType> type = types.register(name, () -> new FluidType(FluidType.Properties.create()
            .density(density)
            .viscosity(viscosity)
            .fallDistanceModifier(0)
            .canExtinguish(true)
            .canHydrate(true)
            .supportsBoating(true)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));
        DeferredHolder<Fluid, BaseFlowingFluid.Source> source = DeferredHolder.create(Registries.FLUID, id);
        DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing = DeferredHolder.create(Registries.FLUID, id.withPrefix("flowing_"));
        DeferredBlock<LiquidBlock> block = DeferredBlock.createBlock(id);
        DeferredItem<BucketItem> bucket = DeferredItem.createItem(id.withSuffix("_bucket"));
        Supplier<BaseFlowingFluid.Properties> properties = () -> new BaseFlowingFluid.Properties(type, source, flowing)
            .block(block)
            .bucket(bucket)
            .tickRate(viscosity / 200);
        fluids.register(name, () -> new BaseFlowingFluid.Source(properties.get()));
        fluids.register("flowing_" + name, () -> new BaseFlowingFluid.Flowing(properties.get()));
        blocks.register(name, () -> new LiquidBlock(source.get(), liquidProperties()));
        items.register(name + "_bucket", () -> new BucketItem(source.get(),
            new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
        return new Entry(name, type, source, flowing, block, bucket);
    }

    /** Vanilla water's block properties; 1.12's {@code Material.WATER} carried the same map colour. */
    private static BlockBehaviour.Properties liquidProperties() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.WATER)
            .replaceable()
            .noCollission()
            .strength(100)
            .pushReaction(PushReaction.DESTROY)
            .noLootTable()
            .liquid()
            .sound(SoundType.EMPTY);
    }
}
