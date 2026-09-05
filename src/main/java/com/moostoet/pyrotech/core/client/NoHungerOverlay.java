package com.moostoet.pyrotech.core.client;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.core.network.NoHungerIndicator;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * The no-hunger icon, the 1.12 {@code NoHungerEventHandler}: an 18 by 18 icon over the
 * crosshair for two seconds after the server sends {@code NoHungerPayload}.
 */
@EventBusSubscriber(modid = Pyrotech.MOD_ID, value = Dist.CLIENT)
public final class NoHungerOverlay {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Pyrotech.MOD_ID, "textures/gui/no_hunger.png");
    private static final int ICON_SIZE = 18;
    private static final int TEXTURE_SIZE = 32;

    private NoHungerOverlay() {
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(Pyrotech.MOD_ID, "no_hunger"), NoHungerOverlay::render);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        NoHungerIndicator.tick();
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!NoHungerIndicator.isVisible()) {
            return;
        }
        int x = graphics.guiWidth() / 2 - ICON_SIZE / 2;
        int y = graphics.guiHeight() / 2 - ICON_SIZE / 2;
        graphics.blit(TEXTURE, x, y, 0, 0, ICON_SIZE, ICON_SIZE, TEXTURE_SIZE, TEXTURE_SIZE);
    }
}
