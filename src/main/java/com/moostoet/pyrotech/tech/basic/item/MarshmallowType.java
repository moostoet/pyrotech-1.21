package com.moostoet.pyrotech.tech.basic.item;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

/** What sits on a marshmallow stick. The ordinal is the {@code pyrotech:marshmallow_type} model predicate value. */
public enum MarshmallowType implements StringRepresentable {
    PLAIN("plain"),
    ROASTED("roasted"),
    BURNED("burned");

    public static final Codec<MarshmallowType> CODEC = StringRepresentable.fromEnum(MarshmallowType::values);
    public static final StreamCodec<RegistryFriendlyByteBuf, MarshmallowType> STREAM_CODEC = NeoForgeStreamCodecs.enumCodec(MarshmallowType.class);

    private final String name;

    MarshmallowType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
