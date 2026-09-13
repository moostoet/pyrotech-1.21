package com.moostoet.pyrotech.library.client;

import net.minecraft.client.gui.screens.Screen;

/** The shift test behind the extended tooltips, kept in a client class so a server never loads {@link Screen}. */
public final class ShiftKey {

    private ShiftKey() {
    }

    public static boolean isDown() {
        return Screen.hasShiftDown();
    }
}
