package com.moostoet.pyrotech.tech.basic.recipe;

import com.moostoet.pyrotech.core.PyrotechTags;
import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import org.jetbrains.annotations.Nullable;

/**
 * What works an anvil: a hammer or a pickaxe. The 1.12 {@code getTypeFromItemStack}
 * read the hammer config and the pickaxe lists; here a hammer is anything in
 * {@code #pyrotech:hammers} and a pickaxe anything that can dig as one.
 */
public enum AnvilToolType implements StringRepresentable {
    HAMMER("hammer"),
    PICKAXE("pickaxe");

    public static final Codec<AnvilToolType> CODEC = StringRepresentable.fromEnum(AnvilToolType::values);
    public static final StreamCodec<RegistryFriendlyByteBuf, AnvilToolType> STREAM_CODEC = NeoForgeStreamCodecs.enumCodec(AnvilToolType.class);

    private final String name;

    AnvilToolType(String name) {
        this.name = name;
    }

    @Nullable
    public static AnvilToolType of(ItemStack stack) {
        if (stack.is(PyrotechTags.Items.HAMMERS)) {
            return HAMMER;
        }
        if (stack.canPerformAction(ItemAbilities.PICKAXE_DIG)) {
            return PICKAXE;
        }
        return null;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
