package com.moostoet.pyrotech.prototype.campfire;

import net.minecraft.util.StringRepresentable;

/**
 * The "item" value is unused at runtime. It exists because the converted
 * blockstate JSON from 1.12 references it in multipart conditions.
 */
public enum CampfireVariant implements StringRepresentable {
    NORMAL("normal"),
    LIT("lit"),
    ASH("ash"),
    ITEM("item");

    private final String name;

    CampfireVariant(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
