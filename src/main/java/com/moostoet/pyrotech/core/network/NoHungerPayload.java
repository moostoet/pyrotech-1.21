package com.moostoet.pyrotech.core.network;

import com.moostoet.pyrotech.Pyrotech;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The 1.12 {@code SCPacketNoHunger}: tells a client its player is too hungry to work a
 * block, and the client shows the no-hunger icon for two seconds. Tech/basic's worktable,
 * chopping block, compacting bin, and anvil, the bloom, and hunting's carcass send it.
 */
public record NoHungerPayload() implements CustomPacketPayload {

    public static final NoHungerPayload INSTANCE = new NoHungerPayload();
    public static final Type<NoHungerPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Pyrotech.MOD_ID, "no_hunger"));
    public static final StreamCodec<ByteBuf, NoHungerPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void send(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, INSTANCE);
    }

    static void handle(NoHungerPayload payload, IPayloadContext context) {
        NoHungerIndicator.show();
    }
}
