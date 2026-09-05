package com.moostoet.pyrotech.core;

import com.moostoet.pyrotech.Pyrotech;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CreativeModeTab;
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
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;
import java.util.function.Supplier;

/**
 * Core's fluids: liquid clay and the three wines. Each is a fluid type, a source and a
 * flowing fluid, a liquid block, and a bucket. 1.12 built them on {@code Material.WATER},
 * so they behave as water does: they put out fire, wet farmland, carry boats, and reset
 * fall distance. Density and viscosity are the 1.12 values, and the flow tick rate is
 * 1.12's viscosity over 200.
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

    private static Entry fluid(String name, int density, int viscosity) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Pyrotech.MOD_ID, name);
        DeferredHolder<FluidType, FluidType> type = FLUID_TYPES.register(name, () -> new FluidType(FluidType.Properties.create()
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
        FLUIDS.register(name, () -> new BaseFlowingFluid.Source(properties.get()));
        FLUIDS.register("flowing_" + name, () -> new BaseFlowingFluid.Flowing(properties.get()));
        CoreBlocks.BLOCKS.register(name, () -> new LiquidBlock(source.get(), liquidProperties()));
        CoreItems.ITEMS.register(name + "_bucket", () -> new BucketItem(source.get(),
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

    static void addToTab(CreativeModeTab.Output output) {
        for (Entry entry : ALL) {
            output.accept(entry.bucket().get());
        }
    }
}
