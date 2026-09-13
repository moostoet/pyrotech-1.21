package com.moostoet.pyrotech.hunting.client;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.hunting.HuntingBlockEntities;
import com.moostoet.pyrotech.hunting.HuntingEntities;
import com.moostoet.pyrotech.hunting.HuntingFluids;
import com.moostoet.pyrotech.hunting.item.SpearItem;
import com.moostoet.pyrotech.library.fluid.PyrotechFluids;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Hunting's client registrations: the mud's model layer and renderer, the spear, arrow,
 * and soaking hide renderers, the stuck spears layer on every living renderer, the
 * butcher's block renderer, the spears' {@code charging} predicate, and tannin's textures.
 */
@EventBusSubscriber(modid = Pyrotech.MOD_ID, value = Dist.CLIENT)
public final class HuntingClient {

    private static final ResourceLocation CHARGING = ResourceLocation.withDefaultNamespace("charging");

    private HuntingClient() {
    }

    /** The 1.12 {@code ModelMud} is vanilla's outer slime body, box for box. */
    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MudRenderer.LAYER, SlimeModel::createOuterBodyLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(HuntingEntities.MUD.get(), MudRenderer::new);
        event.registerEntityRenderer(HuntingEntities.SPEAR.get(), SpearRenderer::new);
        event.registerEntityRenderer(HuntingEntities.FLINT_ARROW.get(), PyrotechArrowRenderer::new);
        event.registerEntityRenderer(HuntingEntities.BONE_ARROW.get(), PyrotechArrowRenderer::new);
        event.registerEntityRenderer(HuntingEntities.HIDE_SCRAPED_ITEM.get(), ItemEntityRenderer::new);
        event.registerBlockEntityRenderer(HuntingBlockEntities.BUTCHERS_BLOCK.get(), ButchersBlockRenderer::new);
    }

    /** The 1.12 join-world hook added the spear layer to every living renderer; this does it once, up front. */
    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        ItemRenderer items = event.getContext().getItemRenderer();
        for (EntityType<?> type : event.getEntityTypes()) {
            EntityRenderer<?> renderer = event.<Entity, EntityRenderer<Entity>>getRenderer(type);
            if (renderer instanceof LivingEntityRenderer<?, ?> living) {
                addSpearLayer(living, items);
            }
        }
        for (PlayerSkin.Model skin : event.getSkins()) {
            EntityRenderer<? extends Player> renderer = event.getSkin(skin);
            if (renderer instanceof LivingEntityRenderer<?, ?> living) {
                addSpearLayer(living, items);
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void addSpearLayer(LivingEntityRenderer<?, ?> renderer, ItemRenderer items) {
        ((LivingEntityRenderer) renderer).addLayer(new StuckSpearsLayer<>(renderer, items));
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for (DeferredItem<SpearItem> spear : com.moostoet.pyrotech.hunting.HuntingItems.SPEARS) {
                ItemProperties.register(spear.get(), CHARGING, (ClampedItemPropertyFunction) (stack, level, entity, seed) ->
                    entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0f : 0.0f);
            }
        });
        PyrotechFluids.Entry tannin = HuntingFluids.TANNIN;
        ItemBlockRenderTypes.setRenderLayer(tannin.source().get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(tannin.flowing().get(), RenderType.translucent());
    }

    /** The migrated 1.12 still and flowing tannin textures. */
    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        PyrotechFluids.Entry tannin = HuntingFluids.TANNIN;
        ResourceLocation still = tannin.stillTexture();
        ResourceLocation flowing = tannin.flowingTexture();
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return still;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return flowing;
            }
        }, tannin.type().get());
    }
}
