package com.moostoet.pyrotech.storage.client;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.storage.StorageBlockEntities;
import com.moostoet.pyrotech.storage.StorageBlocks;
import com.moostoet.pyrotech.storage.block.BagBlock;
import com.moostoet.pyrotech.storage.item.BagItem;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.registries.DeferredItem;

/** Storage's client registrations: the seven block entity renderers and the bag's open model predicate. */
@EventBusSubscriber(modid = Pyrotech.MOD_ID, value = Dist.CLIENT)
public final class StorageClient {

    private StorageClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(StorageBlockEntities.STASH.get(), StashRenderer::new);
        event.registerBlockEntityRenderer(StorageBlockEntities.SHELF.get(), ShelfRenderer::new);
        event.registerBlockEntityRenderer(StorageBlockEntities.CRATE.get(), CrateRenderer::new);
        event.registerBlockEntityRenderer(StorageBlockEntities.WOOD_RACK.get(), WoodRackRenderer::new);
        event.registerBlockEntityRenderer(StorageBlockEntities.BAG.get(), BagRenderer::new);
        event.registerBlockEntityRenderer(StorageBlockEntities.TANK.get(), TankRenderer::new);
        event.registerBlockEntityRenderer(StorageBlockEntities.FAUCET.get(), FaucetRenderer::new);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for (DeferredItem<BagItem> bag : StorageBlocks.BAG_ITEMS) {
                ItemProperties.register(bag.get(), BagBlock.OPEN_PREDICATE,
                    (ClampedItemPropertyFunction) (stack, level, entity, seed) -> BagItem.isOpen(stack) ? 1 : 0);
            }
        });
    }
}
