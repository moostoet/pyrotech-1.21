package com.moostoet.pyrotech.tech.basic.recipe;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

/** The three anvils. A recipe lists the tiers it runs on; a bloomery recipe lists the tiers that may work its bloom. */
public enum AnvilTier implements StringRepresentable {
    GRANITE("granite"),
    IRONCLAD("ironclad"),
    OBSIDIAN("obsidian");

    public static final Codec<AnvilTier> CODEC = StringRepresentable.fromEnum(AnvilTier::values);
    public static final StreamCodec<RegistryFriendlyByteBuf, AnvilTier> STREAM_CODEC = NeoForgeStreamCodecs.enumCodec(AnvilTier.class);

    private final String name;

    AnvilTier(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
