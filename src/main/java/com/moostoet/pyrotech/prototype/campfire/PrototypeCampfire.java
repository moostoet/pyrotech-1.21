package com.moostoet.pyrotech.prototype.campfire;

import com.moostoet.pyrotech.Pyrotech;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class PrototypeCampfire {

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Pyrotech.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Pyrotech.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Pyrotech.MOD_ID);

    public static final DeferredBlock<CampfireBlock> CAMPFIRE = BLOCKS.registerBlock("campfire",
        CampfireBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD)
            .strength(0.5f)
            .sound(SoundType.WOOD)
            .noOcclusion());

    public static final DeferredItem<BlockItem> CAMPFIRE_ITEM = ITEMS.registerSimpleBlockItem(CAMPFIRE);

    public static final Supplier<BlockEntityType<CampfireBlockEntity>> CAMPFIRE_BLOCK_ENTITY =
        BLOCK_ENTITIES.register("campfire",
            () -> BlockEntityType.Builder.of(CampfireBlockEntity::new, CAMPFIRE.get()).build(null));

    private PrototypeCampfire() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        modEventBus.addListener(PrototypeCampfire::registerPayloads);
        modEventBus.addListener(PrototypeCampfire::registerCapabilities);
        modEventBus.addListener(PrototypeCampfire::addToCreativeTab);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(
            CampfireScrollPayload.TYPE,
            CampfireScrollPayload.STREAM_CODEC,
            CampfireScrollPayload::handle);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CAMPFIRE_BLOCK_ENTITY.get(),
            (campfire, side) -> {
                if (side == Direction.UP) {
                    return campfire.getInput();
                }
                if (side == Direction.DOWN) {
                    return campfire.getOutput();
                }
                return campfire.getFuel();
            });
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(CAMPFIRE_ITEM.get());
        }
    }
}
