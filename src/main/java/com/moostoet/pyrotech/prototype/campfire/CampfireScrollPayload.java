package com.moostoet.pyrotech.prototype.campfire;

import com.moostoet.pyrotech.Pyrotech;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Replaces athenaeum's CSPacketInteractionMouseWheel: sneak-scrolling on the
 * campfire adds or removes a fuel log.
 */
public record CampfireScrollPayload(BlockPos pos, boolean up) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CampfireScrollPayload> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Pyrotech.MOD_ID, "prototype_campfire_scroll"));

    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, CampfireScrollPayload> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC, CampfireScrollPayload::pos,
            ByteBufCodecs.BOOL, CampfireScrollPayload::up,
            CampfireScrollPayload::new);

    @Override
    public CustomPacketPayload.Type<CampfireScrollPayload> type() {
        return TYPE;
    }

    public static void handle(CampfireScrollPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player.distanceToSqr(payload.pos.getCenter()) > 64) {
                return;
            }
            if (!(player.level().getBlockEntity(payload.pos) instanceof CampfireBlockEntity campfire)) {
                return;
            }
            if (player.level().getBlockState(payload.pos).getValue(CampfireBlock.VARIANT) == CampfireVariant.ASH) {
                return;
            }
            if (payload.up) {
                campfire.addLogFrom(player, player.getMainHandItem());
            } else {
                campfire.removeLogTo(player);
            }
        });
    }
}
