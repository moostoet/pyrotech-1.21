package com.moostoet.pyrotech.prototype.campfire;

import com.moostoet.pyrotech.Pyrotech;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PrototypeCampfireClient {

    private PrototypeCampfireClient() {
    }

    @EventBusSubscriber(modid = Pyrotech.MOD_ID, value = Dist.CLIENT)
    public static final class ModEvents {

        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(PrototypeCampfire.CAMPFIRE_BLOCK_ENTITY.get(),
                context -> new CampfireRenderer());
        }
    }

    @EventBusSubscriber(modid = Pyrotech.MOD_ID, value = Dist.CLIENT)
    public static final class GameEvents {

        @SubscribeEvent
        public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null
                || minecraft.level == null
                || !minecraft.player.isShiftKeyDown()
                || event.getScrollDeltaY() == 0) {
                return;
            }
            if (!(minecraft.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK
                || !(minecraft.level.getBlockState(hit.getBlockPos()).getBlock() instanceof CampfireBlock)) {
                return;
            }
            PacketDistributor.sendToServer(new CampfireScrollPayload(hit.getBlockPos(), event.getScrollDeltaY() > 0));
            event.setCanceled(true);
        }
    }
}
