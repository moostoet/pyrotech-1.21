package com.moostoet.pyrotech.core;

/**
 * The 1.12 {@code ItemMaterial} subtypes, one registered item each. The id is the 1.12
 * variant name, which the migrated lang keys and textures already use.
 */
public enum Material {
    PIT_ASH("pit_ash"),
    COAL_COKE("coal_coke"),
    STRAW("straw"),
    FLINT_CLAY_BALL("flint_clay_ball"),
    REFRACTORY_CLAY_BALL("refractory_clay_ball"),
    REFRACTORY_BRICK("refractory_brick"),
    POTTERY_FRAGMENTS("pottery_fragments", "clay_fragments"),
    POTTERY_SHARD("pottery_shard", "clay_shard"),
    SLAKED_LIME("slaked_lime"),
    UNFIRED_REFRACTORY_BRICK("unfired_refractory_brick"),
    FLINT_SHARD("flint_shard"),
    BONE_SHARD("bone_shard"),
    PLANT_FIBERS("plant_fibers"),
    PLANT_FIBERS_DRIED("plant_fibers_dried"),
    TWINE("twine"),
    CHARCOAL_FLAKES("charcoal_flakes"),
    BRICK_STONE("brick_stone", "stone_brick"),
    CLAY_LUMP("clay_lump"),
    DIAMOND_SHARD("diamond_shard"),
    IRON_SHARD("iron_shard"),
    BOARD("board"),
    COAL_PIECES("coal_pieces"),
    QUICKLIME("quicklime"),
    BOARD_TARRED("board_tarred"),
    UNFIRED_BRICK("unfired_brick"),
    PULP("pulp"),
    TWINE_DURABLE("twine_durable"),
    STICK_STONE("stick_stone", "stone_stick"),
    DUST_LIMESTONE("dust_limestone"),
    KINDLING("kindling"),
    KINDLING_TARRED("kindling_tarred"),
    DUST_FLINT("dust_flint"),
    GLASS_SHARD("glass_shard"),
    OBSIDIAN_SHARD("obsidian_shard"),
    GOLD_SHARD("gold_shard"),
    REFRACTORY_CLAY_LUMP("refractory_clay_lump"),
    DENSE_REDSTONE("dense_redstone"),
    DENSE_QUARTZ("dense_quartz"),
    LEATHER_SHEET("leather_sheet"),
    LEATHER_STRAP("leather_strap"),
    LEATHER_CORD("leather_cord"),
    LEATHER_DURABLE("leather_durable"),
    LEATHER_DURABLE_SHEET("leather_durable_sheet"),
    LEATHER_DURABLE_STRAP("leather_durable_strap"),
    LEATHER_DURABLE_CORD("leather_durable_cord"),
    LEATHER_SMALL("leather_small"),
    FLETCHING("fletching"),
    STONE_TOOL_SHAFT("stone_tool_shaft"),
    BOW_DRILL_DURABLE_STICK("bow_drill_durable_stick"),
    LARD("lard"),
    DOUGH("dough"),
    FLOUR("flour"),
    BREAD_DOUGH("bread_dough"),
    COOKIE_DOUGH("cookie_dough"),
    CLAY_BLASTING("clay_blasting");

    private final String id;
    private final String texture;

    Material(String id) {
        this(id, id);
    }

    /** Four variants keep the 1.12 texture file name, which differs from the item id. */
    Material(String id, String texture) {
        this.id = id;
        this.texture = texture;
    }

    public String id() {
        return this.id;
    }

    /** The item texture name under {@code textures/item/}. */
    public String texture() {
        return this.texture;
    }
}
