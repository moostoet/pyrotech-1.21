package com.moostoet.pyrotech.tech.basic.block;

import net.minecraft.util.StringRepresentable;

/** The pit kiln's five states: dug, thatched, logs on, burning, and burned out. */
public enum PitKilnVariant implements StringRepresentable {
    EMPTY("empty"),
    THATCH("thatch"),
    WOOD("wood"),
    ACTIVE("active"),
    COMPLETE("complete");

    private final String name;

    PitKilnVariant(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
