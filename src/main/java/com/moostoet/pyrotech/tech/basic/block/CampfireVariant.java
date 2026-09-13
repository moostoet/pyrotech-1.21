package com.moostoet.pyrotech.tech.basic.block;

import net.minecraft.util.StringRepresentable;

/** Unlit with tinder, lit, or burned out to ash. The converted {@code item} value is gone with the regenerated blockstate. */
public enum CampfireVariant implements StringRepresentable {
    NORMAL("normal"),
    LIT("lit"),
    ASH("ash");

    private final String name;

    CampfireVariant(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
