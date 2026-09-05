package com.moostoet.pyrotech.core.client;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.core.CoreBlocks;
import com.moostoet.pyrotech.core.CoreEntities;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.level.GrassColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/** Core's client registrations: the thrown rock renderers and the grass clump's tint. */
@EventBusSubscriber(modid = Pyrotech.MOD_ID, value = Dist.CLIENT)
public final class CoreClient {

    private CoreClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(CoreEntities.ROCK.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(CoreEntities.ROCK_GRASS.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(CoreEntities.ROCK_NETHERRACK.get(), ThrownItemRenderer::new);
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
