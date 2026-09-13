package com.moostoet.pyrotech.tech.basic.network;

import com.moostoet.pyrotech.Pyrotech;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/** The 1.12 {@code SCPacketParticleAnvilHit}: where a hammer struck an anvil, for the crack particles. */
public record AnvilHitPayload(BlockPos pos, double x, double y, double z) implements CustomPacketPayload {

    public static final Type<AnvilHitPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Pyrotech.MOD_ID, "anvil_hit"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AnvilHitPayload> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, AnvilHitPayload::pos,
        ByteBufCodecs.DOUBLE, AnvilHitPayload::x,
        ByteBufCodecs.DOUBLE, AnvilHitPayload::y,
        ByteBufCodecs.DOUBLE, AnvilHitPayload::z,
        AnvilHitPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public Vec3 hit() {
        return new Vec3(this.x, this.y, this.z);
    }

    public static void send(ServerLevel level, BlockPos pos, Vec3 hit) {
        PacketDistributor.sendToPlayersTrackingChunk(level, new net.minecraft.world.level.ChunkPos(pos), new AnvilHitPayload(pos, hit.x, hit.y, hit.z));
    }
}
