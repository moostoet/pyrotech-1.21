package com.moostoet.pyrotech.core.network;

/**
 * The client-side countdown behind the no-hunger icon. Kept free of client classes so the
 * payload handler can reference it on either side.
 */
public final class NoHungerIndicator {

    private static final int DISPLAY_TICKS = 2 * 20;

    private static int ticksRemaining;

    private NoHungerIndicator() {
    }

    public static void show() {
        ticksRemaining = DISPLAY_TICKS;
    }

    public static void tick() {
        if (ticksRemaining > 0) {
            ticksRemaining--;
        }
    }

    public static boolean isVisible() {
        return ticksRemaining > 0;
    }
}
