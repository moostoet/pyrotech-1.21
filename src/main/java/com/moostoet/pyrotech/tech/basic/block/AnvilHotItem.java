package com.moostoet.pyrotech.tech.basic.block;

/**
 * An item that burns whoever walks over the anvil it lies on and makes the anvil crackle
 * and throw flames (tech/basic sign-off, item 7). Bloomery's bloom implements it; the anvil
 * never names the bloom.
 */
public interface AnvilHotItem {

    /** The hot-floor damage a walker takes. */
    float walkDamage();
}
