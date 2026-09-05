package com.moostoet.pyrotech.core;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Core's config. Behaviour toggles and gameplay multipliers live here; numbers that
 * describe an item, recipe, or fuel live in data or as constants (core sign-off, item 3).
 * Keys keep their 1.12 names.
 */
public final class CoreConfig {

    public static final Common COMMON;
    public static final ModConfigSpec COMMON_SPEC;
    public static final Client CLIENT;
    public static final ModConfigSpec CLIENT_SPEC;

    static {
        Pair<Common, ModConfigSpec> common = new ModConfigSpec.Builder().configure(Common::new);
        COMMON = common.getLeft();
        COMMON_SPEC = common.getRight();
        Pair<Client, ModConfigSpec> client = new ModConfigSpec.Builder().configure(Client::new);
        CLIENT = client.getLeft();
        CLIENT_SPEC = client.getRight();
    }

    private CoreConfig() {
    }

    public static final class Common {

        public final ModConfigSpec.BooleanValue preventWoolOnSheepDeath;
        public final ModConfigSpec.BooleanValue removeVanillaCraftingTable;
        public final ModConfigSpec.BooleanValue replaceVanillaFurnace;
        public final ModConfigSpec.BooleanValue dropSticksFromLeaves;
        public final ModConfigSpec.BooleanValue replaceIronIngots;
        public final ModConfigSpec.ConfigValue<String> replaceIronIngotsWith;
        public final ModConfigSpec.BooleanValue requireShovelToPickupWoodChips;

        public final ModConfigSpec.IntValue mulchedFarmlandCharges;
        public final ModConfigSpec.BooleanValue mulchedFarmlandUnlimitedCharges;
        public final ModConfigSpec.BooleanValue mulchedFarmlandAllowTrample;
        public final ModConfigSpec.BooleanValue mulchedFarmlandRestrictToMoisturized;

        public final ModConfigSpec.BooleanValue strawBedDaytimeDestroyCheck;

        public final ModConfigSpec.DoubleValue gloamberryGrowthChance;
        public final ModConfigSpec.DoubleValue gloamberryBerryGrowthChance;
        public final ModConfigSpec.DoubleValue gloamberryObstructedGrowthModifier;
        public final ModConfigSpec.DoubleValue gloamberryDaytimeBerryLossChance;

        public final ModConfigSpec.DoubleValue pyroberryGrowthChance;
        public final ModConfigSpec.DoubleValue pyroberryBerryGrowthChance;
        public final ModConfigSpec.DoubleValue pyroberryRainGrowthRevertChance;
        public final ModConfigSpec.DoubleValue pyroberryObstructedGrowthRevertChance;

        Common(ModConfigSpec.Builder builder) {
            builder.push("tweaks");
            preventWoolOnSheepDeath = builder
                .comment("If true, vanilla sheep won't drop wool when killed.")
                .define("PREVENT_WOOL_ON_SHEEP_DEATH", true);
            removeVanillaCraftingTable = builder
                .comment("When a vanilla crafting table generates in a new chunk, for example in a village,",
                    "the table is removed.")
                .define("REMOVE_VANILLA_CRAFTING_TABLE", true);
            replaceVanillaFurnace = builder
                .comment("When a vanilla furnace, blast furnace, or smoker generates in a new chunk, for example",
                    "in a village, it is replaced with cobblestone. A vanilla campfire is removed.",
                    "Also strips the furnace from the snowy village house chest loot.")
                .define("REPLACE_VANILLA_FURNACE", true);
            dropSticksFromLeaves = builder
                .comment("Set to false to disable dropping sticks from leaves.")
                .define("DROP_STICKS_FROM_LEAVES", true);
            replaceIronIngots = builder
                .comment("Pyrotech will swap iron ingots for raw iron in loot tables by default.",
                    "This feature may not play well with mods designed to modify loot tables.",
                    "Set to false to disable.")
                .define("REPLACE_IRON_INGOTS", true);
            replaceIronIngotsWith = builder
                .comment("The item that replaces iron ingots in loot tables.")
                .define("REPLACE_IRON_INGOTS_WITH", "minecraft:raw_iron",
                    value -> value instanceof String s && ResourceLocation.tryParse(s) != null);
            requireShovelToPickupWoodChips = builder
                .comment("Set to false to allow all wood chips to be collected with any held item.",
                    "Affects the pile of wood chips, the block of wood chips, and the chopping block's pile.")
                .define("REQUIRE_SHOVEL_TO_PICKUP_WOOD_CHIPS", true);
            builder.pop();

            builder.push("mulched_farmland");
            mulchedFarmlandCharges = builder
                .comment("The number of times the mulched farmland will apply bonemeal to a crop before",
                    "reverting to normal moisturized farmland.")
                .defineInRange("CHARGES", 6, 1, Integer.MAX_VALUE);
            mulchedFarmlandUnlimitedCharges = builder
                .comment("Set to true to ignore the charge count and allow the mulched farmland",
                    "to exist indefinitely.")
                .define("UNLIMITED_CHARGES", false);
            mulchedFarmlandAllowTrample = builder
                .comment("Set to true to allow the farmland to be trampled and turned to dirt the",
                    "same as vanilla farmland.")
                .define("ALLOW_TRAMPLE", false);
            mulchedFarmlandRestrictToMoisturized = builder
                .comment("Set to true to restrict the placement of mulch to moisturized farmland",
                    "only. If set to false, mulch can be placed on wet or dry farmland.")
                .define("RESTRICT_TO_MOISTURIZED_FARMLAND", false);
            builder.pop();

            builder.push("straw_bed");
            strawBedDaytimeDestroyCheck = builder
                .comment("Check for daytime when destroying the bed.",
                    "If false, the bed will be destroyed whenever the player leaves it, regardless of time of day.")
                .define("DAYTIME_DESTROY_CHECK", true);
            builder.pop();

            builder.push("gloamberry_bush");
            gloamberryGrowthChance = builder
                .comment("The chance of advancing to the next growth stage when the block randomly ticks.")
                .defineInRange("GROWTH_CHANCE", 0.05, 0, 1);
            gloamberryBerryGrowthChance = builder
                .comment("The chance of advancing to the last growth stage when the block randomly ticks.")
                .defineInRange("BERRY_GROWTH_CHANCE", 0.1, 0, 1);
            gloamberryObstructedGrowthModifier = builder
                .comment("The multiplicative modifier applied to the growth chance when the block can't see sky.",
                    "chance = chance * modifier")
                .defineInRange("OBSTRUCTED_GROWTH_MULTIPLICATIVE_MODIFIER", 0.25, 0, 1);
            gloamberryDaytimeBerryLossChance = builder
                .comment("The chance of losing its berries during the day.")
                .defineInRange("DAYTIME_BERRY_LOSS_CHANCE", 0.75, 0, 1);
            builder.pop();

            builder.push("pyroberry_bush");
            pyroberryGrowthChance = builder
                .comment("The chance of advancing to the next growth stage when the block randomly ticks.")
                .defineInRange("GROWTH_CHANCE", 0.025, 0, 1);
            pyroberryBerryGrowthChance = builder
                .comment("The chance of advancing to the last growth stage when the block randomly ticks.")
                .defineInRange("BERRY_GROWTH_CHANCE", 0.05, 0, 1);
            pyroberryRainGrowthRevertChance = builder
                .comment("The chance of reverting to a previous growth stage when the block randomly ticks in the rain.")
                .defineInRange("RAIN_GROWTH_REVERT_CHANCE", 1.0, 0, 1);
            pyroberryObstructedGrowthRevertChance = builder
                .comment("The chance of reverting to a previous growth stage when the block randomly ticks and can't see sky.")
                .defineInRange("OBSTRUCTED_GROWTH_REVERT_CHANCE", 0.25, 0, 1);
            builder.pop();
        }
    }

    public static final class Client {

        public final ModConfigSpec.BooleanValue showDurabilityTooltips;
        public final ModConfigSpec.BooleanValue showBurnTimeInTooltips;
        public final ModConfigSpec.BooleanValue showRecipeProgressionParticles;

        Client(ModConfigSpec.Builder builder) {
            builder.push("client");
            showDurabilityTooltips = builder
                .comment("Set to false to hide the durability tooltip on tools.")
                .define("SHOW_DURABILITY_TOOLTIPS", true);
            showBurnTimeInTooltips = builder
                .comment("Show an item's burn time in its tooltip.")
                .define("SHOW_BURN_TIME_IN_TOOLTIPS", true);
            showRecipeProgressionParticles = builder
                .comment("Some interactions will give off some green particles to indicate",
                    "that the recipe / tool combination is valid and recipe progress",
                    "has incremented.",
                    "Set to false to disable these progression particles.")
                .define("SHOW_RECIPE_PROGRESSION_PARTICLES", true);
            builder.pop();
        }
    }
}
