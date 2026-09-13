package com.moostoet.pyrotech.core.network;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.core.client.ProgressParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The 1.12 {@code SCPacketParticleProgress}: a recipe or tool step went through, so the
 * clients tracking the chunk show the green progression particles there, unless their
 * {@code SHOW_RECIPE_PROGRESSION_PARTICLES} flag is off. Hunting's carcass, butcher's block,
 * and soaking hide send it; tech/basic's machines will too.
 */
public record ProgressParticlesPayload(double x, double y, double z, int count) implements CustomPacketPayload {

    public static final Type<ProgressParticlesPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Pyrotech.MOD_ID, "progress_particles"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ProgressParticlesPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.DOUBLE, ProgressParticlesPayload::x,
        ByteBufCodecs.DOUBLE, ProgressParticlesPayload::y,
        ByteBufCodecs.DOUBLE, ProgressParticlesPayload::z,
        ByteBufCodecs.VAR_INT, ProgressParticlesPayload::count,
        ProgressParticlesPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void send(ServerLevel level, double x, double y, double z, int count) {
        PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(BlockPos.containing(x, y, z)),
            new ProgressParticlesPayload(x, y, z, count));
    }

    static void handle(ProgressParticlesPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ProgressParticles.spawn(payload));
    }
}
