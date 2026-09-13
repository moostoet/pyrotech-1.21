package com.moostoet.pyrotech.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * The eleven 1.12 chunk generators. Each one has a worldgen toggle in the config and
 * one biome modifier that the toggle gates. The order and the ids are the 1.12 config's.
 */
public enum Generator implements StringRepresentable {

    FOSSIL("fossil"),
    LIMESTONE("limestone"),
    DENSE_COAL("dense_coal_ore"),
    DENSE_NETHER_COAL("dense_nether_coal_ore"),
    PYROBERRY_BUSH("pyroberry_bush"),
    GLOAMBERRY_BUSH("gloamberry_bush"),
    FRECKLEBERRY_PLANT("freckleberry_plant"),
    ROCKS("rocks"),
    DENSE_REDSTONE("dense_redstone_ore"),
    DENSE_QUARTZ("dense_quartz_ore"),
    MUD("mud");

    public static final Codec<Generator> CODEC = StringRepresentable.fromEnum(Generator::values);

    private final String id;

    Generator(String id) {
        this.id = id;
    }

    public String id() {
        return this.id;
    }

    @Override
    public String getSerializedName() {
        return this.id;
    }
}
