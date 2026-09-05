package com.moostoet.pyrotech.core.client;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.core.CoreBlocks;
import com.moostoet.pyrotech.core.CoreEntities;
import com.moostoet.pyrotech.core.CoreFluids;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GrassColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** Core's client registrations: the entity renderers, the grass clump's tint, and the fluid textures. */
@EventBusSubscriber(modid = Pyrotech.MOD_ID, value = Dist.CLIENT)
public final class CoreClient {

    private CoreClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(CoreEntities.ROCK.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(CoreEntities.ROCK_GRASS.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(CoreEntities.ROCK_NETHERRACK.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(CoreEntities.PYROBERRY_COCKTAIL.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(CoreEntities.BOOK.get(), ItemEntityRenderer::new);
    }

    /** The migrated 1.12 still and flowing textures for each fluid. */
    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        for (CoreFluids.Entry fluid : CoreFluids.ALL) {
            ResourceLocation still = fluid.stillTexture();
            ResourceLocation flowing = fluid.flowingTexture();
            event.registerFluidType(new IClientFluidTypeExtensions() {
                @Override
                public ResourceLocation getStillTexture() {
                    return still;
                }

                @Override
                public ResourceLocation getFlowingTexture() {
                    return flowing;
                }
            }, fluid.type().get());
        }
    }

    /** The fluid textures carry alpha, so the fluids draw on the translucent layer as water does. */
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        for (CoreFluids.Entry fluid : CoreFluids.ALL) {
            ItemBlockRenderTypes.setRenderLayer(fluid.source().get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(fluid.flowing().get(), RenderType.translucent());
        }
    }

    /**
     * The grass clump takes the biome's grass colour. 1.12's handler looked the biome colour
     * up and then returned the default one; the lookup is what it meant.
     */
    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> level != null && pos != null
                ? BiomeColors.getAverageGrassColor(level, pos)
                : GrassColor.getDefaultColor(),
            CoreBlocks.ROCK_GRASS.get());
    }
}
