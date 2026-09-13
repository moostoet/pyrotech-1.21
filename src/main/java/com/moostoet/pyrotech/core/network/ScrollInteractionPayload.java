package com.moostoet.pyrotech.core.network;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.library.interaction.ScrollInteractable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Athenaeum's {@code CSPacketInteractionMouseWheel}: the client sneaked and scrolled with
 * the cursor on a block whose block entity is a {@link ScrollInteractable}. Carries the hit
 * so the block entity can pick the slot.
 */
public record ScrollInteractionPayload(BlockPos pos, Direction face, double x, double y, double z, boolean up)
    implements CustomPacketPayload {

    public static final Type<ScrollInteractionPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Pyrotech.MOD_ID, "scroll_interaction"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ScrollInteractionPayload> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, ScrollInteractionPayload::pos,
        Direction.STREAM_CODEC, ScrollInteractionPayload::face,
        ByteBufCodecs.DOUBLE, ScrollInteractionPayload::x,
        ByteBufCodecs.DOUBLE, ScrollInteractionPayload::y,
        ByteBufCodecs.DOUBLE, ScrollInteractionPayload::z,
        ByteBufCodecs.BOOL, ScrollInteractionPayload::up,
        ScrollInteractionPayload::new);

    private static final double MAX_DISTANCE_SQUARED = 64;

    public ScrollInteractionPayload(BlockHitResult hit, boolean up) {
        this(hit.getBlockPos(), hit.getDirection(), hit.getLocation().x, hit.getLocation().y, hit.getLocation().z, up);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(ScrollInteractionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)
                || player.distanceToSqr(payload.pos.getCenter()) > MAX_DISTANCE_SQUARED
                || !(player.level().getBlockEntity(payload.pos) instanceof ScrollInteractable target)) {
                return;
            }
            target.scroll(player, new BlockHitResult(new Vec3(payload.x, payload.y, payload.z), payload.face, payload.pos, false), payload.up);
        });
    }
}
