package com.moostoet.pyrotech.core.client;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.core.network.ScrollInteractionPayload;
import com.moostoet.pyrotech.library.interaction.ScrollInteractable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Athenaeum's mouse wheel handler: a sneaking player scrolling with the cursor on a
 * scroll-interactable block sends the hit to the server instead of switching hotbar slots.
 */
@EventBusSubscriber(modid = Pyrotech.MOD_ID, value = Dist.CLIENT)
public final class ScrollInteractionHandler {

    private ScrollInteractionHandler() {
    }

    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !player.isShiftKeyDown() || event.getScrollDeltaY() == 0) {
            return;
        }
        if (!(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK
            || !(player.level().getBlockEntity(hit.getBlockPos()) instanceof ScrollInteractable)) {
            return;
        }
        PacketDistributor.sendToServer(new ScrollInteractionPayload(hit, event.getScrollDeltaY() > 0));
        event.setCanceled(true);
    }
}
