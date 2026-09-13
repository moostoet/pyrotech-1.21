package com.moostoet.pyrotech.tech.basic;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

/**
 * Tech/basic's config (tech/basic sign-off, item 12): the behaviour toggles, two counts,
 * and the anvil hit table in {@code COMMON}, and the multipliers, which the recipe viewer
 * must see, in {@code SERVER}. Every other 1.12 number bakes into the blocks, the items,
 * and the recipes. Keys keep their 1.12 names.
 */
public final class TechBasicConfig {

    public static final Common COMMON;
    public static final ModConfigSpec COMMON_SPEC;
    public static final Server SERVER;
    public static final ModConfigSpec SERVER_SPEC;

    static {
        Pair<Common, ModConfigSpec> common = new ModConfigSpec.Builder().configure(Common::new);
        COMMON = common.getLeft();
        COMMON_SPEC = common.getRight();
        Pair<Server, ModConfigSpec> server = new ModConfigSpec.Builder().configure(Server::new);
        SERVER = server.getLeft();
        SERVER_SPEC = server.getRight();
    }

    private TechBasicConfig() {
    }

    public static final class Common {

        public final ModConfigSpec.BooleanValue allowRecipeClear;
        public final ModConfigSpec.BooleanValue allowRecipeRepeat;
        public final ModConfigSpec.IntValue recipeRepeatToolDamage;
        public final ModConfigSpec.BooleanValue worktableUsesDurability;
        public final ModConfigSpec.BooleanValue stoneWorktableUsesDurability;
        public final ModConfigSpec.BooleanValue autoCreateRecipesFromFood;
        public final ModConfigSpec.IntValue recipeRuinRainTicks;
        public final ModConfigSpec.ConfigValue<List<? extends Integer>> hitReductionPerHammerLevel;
        public final ModConfigSpec.BooleanValue graniteAnvilUseDurability;
        public final ModConfigSpec.BooleanValue ironcladAnvilUseDurability;
        public final ModConfigSpec.BooleanValue obsidianAnvilUseDurability;
        public final ModConfigSpec.BooleanValue choppingBlockUsesDurability;
        public final ModConfigSpec.BooleanValue useAsLadder;
        public final ModConfigSpec.BooleanValue pitKilnExtinguishedByRain;
        public final ModConfigSpec.BooleanValue campfireExtinguishedByRain;
        public final ModConfigSpec.BooleanValue burnedMarshmallowBroadcast;
        public final ModConfigSpec.BooleanValue burnedMarshmallowEatBroadcast;
        public final ModConfigSpec.BooleanValue comfortEnabled;
        public final ModConfigSpec.BooleanValue restingEnabled;
        public final ModConfigSpec.BooleanValue wellFedEnabled;
        public final ModConfigSpec.BooleanValue wellRestedEnabled;
        public final ModConfigSpec.BooleanValue focusedEnabled;

        Common(ModConfigSpec.Builder builder) {
            builder.push("worktable_common");
            this.allowRecipeClear = builder
                .comment("If this is true, a player will be allowed to sneak + click using an",
                    "empty hand to remove all items from the worktable's crafting grid.",
                    "The removed items will be placed into the player's inventory or on top",
                    "of the worktable if the player's inventory is full.")
                .define("ALLOW_RECIPE_CLEAR", false);
            this.allowRecipeRepeat = builder
                .comment("If this is true, a player will be allowed to sneak + click using a",
                    "hammer to automatically place items from their inventory into the",
                    "worktable's crafting grid that match the ingredients for the last",
                    "recipe completed. The hammer will be damaged, see RECIPE_REPEAT_TOOL_DAMAGE.")
                .define("ALLOW_RECIPE_REPEAT", false);
            this.recipeRepeatToolDamage = builder
                .comment("If ALLOW_RECIPE_REPEAT is enabled, this is the amount of damage that",
                    "will be applied to the hammer. Set to zero to disable.")
                .defineInRange("RECIPE_REPEAT_TOOL_DAMAGE", 1, 0, Integer.MAX_VALUE);
            builder.pop();

            builder.push("worktable");
            this.worktableUsesDurability = builder
                .comment("If true, the worktable has durability and will break after 64 crafts completed.")
                .define("USES_DURABILITY", true);
            builder.pop();

            builder.push("stone_worktable");
            this.stoneWorktableUsesDurability = builder
                .comment("If true, the stone worktable has durability and will break after 512 crafts completed.")
                .define("USES_DURABILITY", false);
            builder.pop();

            builder.push("compost_bin");
            this.autoCreateRecipesFromFood = builder
                .comment("Set this to false to prevent the mod from automatically creating",
                    "mulch recipes from food items.")
                .define("AUTO_CREATE_RECIPES_FROM_FOOD", true);
            builder.pop();

            builder.push("tanning_rack");
            this.recipeRuinRainTicks = builder
                .comment("Number of rain ticks before the recipe is ruined.",
                    "Set to -1 to disable.")
                .defineInRange("RECIPE_RUIN_RAIN_TICKS", 2 * 60 * 20, -1, Integer.MAX_VALUE);
            builder.pop();

            builder.push("anvil_common");
            this.hitReductionPerHammerLevel = builder
                .comment("These values are used to reduce the number of hits required to complete",
                    "a recipe.",
                    "",
                    "The index into the array is the tool level, the value at that index",
                    "is the hit reduction. The array can be expanded as needed.",
                    "If the tool level of the tool used exceeds the array length, the",
                    "last element in the array is used.",
                    "",
                    "ie. {wood, stone, iron, diamond}")
                .defineList("HIT_REDUCTION_PER_HAMMER_HARVEST_LEVEL", List.of(0, 1, 2, 3), () -> 0, value -> value instanceof Integer);
            builder.pop();

            builder.push("granite_anvil");
            this.graniteAnvilUseDurability = builder
                .comment("Set to false to prevent the device from wearing out.")
                .define("USE_DURABILITY", true);
            builder.pop();
            builder.push("ironclad_anvil");
            this.ironcladAnvilUseDurability = builder
                .comment("Set to false to prevent the device from wearing out.")
                .define("USE_DURABILITY", true);
            builder.pop();
            builder.push("obsidian_anvil");
            this.obsidianAnvilUseDurability = builder
                .comment("Set to false to prevent the device from wearing out.")
                .define("USE_DURABILITY", true);
            builder.pop();

            builder.push("chopping_block");
            this.choppingBlockUsesDurability = builder
                .comment("Set to false to prevent the device from wearing out.")
                .define("USES_DURABILITY", true);
            builder.pop();

            builder.push("drying_rack");
            this.useAsLadder = builder
                .comment("Set to true to allow the player to climb the sides of drying racks.")
                .define("USE_AS_LADDER", true);
            builder.pop();

            builder.push("pit_kiln");
            this.pitKilnExtinguishedByRain = builder
                .comment("Set to true if the pit kiln should be extinguished by rain.")
                .define("EXTINGUISHED_BY_RAIN", true);
            builder.pop();

            builder.push("campfire");
            this.campfireExtinguishedByRain = builder
                .comment("Set to true if the campfire should be extinguished by rain.")
                .define("EXTINGUISHED_BY_RAIN", true);
            builder.pop();

            builder.push("campfire_marshmallows");
            this.burnedMarshmallowBroadcast = builder
                .comment("Set to false to disable the message broadcast to all players when a",
                    "player burns a marshmallow.",
                    "The message is located under the lang key:",
                    "  gui.pyrotech.marshmallow.burned.broadcast.message")
                .define("ENABLE_BURNED_MARSHMALLOW_BROADCAST_MESSAGE", true);
            this.burnedMarshmallowEatBroadcast = builder
                .comment("Set to false to disable the message broadcast to all players when a",
                    "player eats a burned marshmallow.",
                    "The message is located under the lang key:",
                    "  gui.pyrotech.marshmallow.burned.eat.broadcast.message")
                .define("ENABLE_BURNED_MARSHMALLOW_EAT_BROADCAST_MESSAGE", true);
            builder.pop();

            builder.push("campfire_effects");
            this.comfortEnabled = builder
                .comment("When a player is within range of a campfire, they will",
                    "get the comfort effect.",
                    "Set to false to disable.")
                .define("COMFORT_EFFECT_ENABLED", true);
            this.restingEnabled = builder
                .comment("When a player is within range of a campfire, they will",
                    "get the resting effect.",
                    "Set to false to disable.")
                .define("RESTING_EFFECT_ENABLED", true);
            this.wellFedEnabled = builder
                .comment("When a player eats with full saturation while under the Comfort effect,",
                    "they will gain the Well Fed effect. Set to false to disable.")
                .define("WELL_FED_EFFECT_ENABLED", true);
            this.wellRestedEnabled = builder
                .comment("When a player stands still long enough with the Resting III effect,",
                    "they will gain the Well Rested effect. Set to false to disable.")
                .define("WELL_RESTED_EFFECT_ENABLED", true);
            this.focusedEnabled = builder
                .comment("When a player stands still long enough with the Resting III effect,",
                    "the Well Rested effect, the Comfort effect, and the Well Fed effect,",
                    "they will gain the Focused effect. Set to false to disable.")
                .define("FOCUSED_EFFECT_ENABLED", true);
            builder.pop();
        }

        /** The hit reduction for a hammer of the given tool level; a level past the end uses the last entry. */
        public int hitReduction(int toolLevel) {
            List<? extends Integer> table = this.hitReductionPerHammerLevel.get();
            if (table.isEmpty()) {
                return 0;
            }
            return table.get(Math.min(Math.max(toolLevel, 0), table.size() - 1));
        }
    }

    /** The conditional speed modifiers of one drying rack, eleven per rack. */
    public static final class DryingRackModifiers {

        public final ModConfigSpec.DoubleValue directRain;
        public final ModConfigSpec.DoubleValue indirectRain;
        public final ModConfigSpec.DoubleValue nether;
        public final ModConfigSpec.DoubleValue baseDerived;
        public final ModConfigSpec.DoubleValue derivedHot;
        public final ModConfigSpec.DoubleValue derivedDry;
        public final ModConfigSpec.DoubleValue derivedCold;
        public final ModConfigSpec.DoubleValue derivedWet;
        public final ModConfigSpec.IntValue fireSourceBonusRange;
        public final ModConfigSpec.DoubleValue fireSourceBonus;
        public final ModConfigSpec.DoubleValue daytime;

        DryingRackModifiers(ModConfigSpec.Builder builder) {
            builder.push("conditional_modifiers");
            this.directRain = builder
                .comment("The base speed if the device is being directly rained on.")
                .defineInRange("DIRECT_RAIN", -1.0, -Double.MAX_VALUE, Double.MAX_VALUE);
            this.indirectRain = builder
                .comment("The base speed if the it is raining, but not directly on the device, or",
                    "the biome has high humidity.")
                .defineInRange("INDIRECT_RAIN", 0.25, -Double.MAX_VALUE, Double.MAX_VALUE);
            this.nether = builder
                .comment("The base speed if the device is in the Nether.")
                .defineInRange("NETHER", 2.0, -Double.MAX_VALUE, Double.MAX_VALUE);
            this.baseDerived = builder
                .comment("The base derived speed.")
                .defineInRange("BASE_DERIVED", 1.0, -Double.MAX_VALUE, Double.MAX_VALUE);
            this.derivedHot = builder
                .comment("Added to the base derived speed if the biome is hot.")
                .defineInRange("DERIVED_HOT", 0.2, -Double.MAX_VALUE, Double.MAX_VALUE);
            this.derivedDry = builder
                .comment("Added to the base derived speed if the biome is dry.")
                .defineInRange("DERIVED_DRY", 0.2, -Double.MAX_VALUE, Double.MAX_VALUE);
            this.derivedCold = builder
                .comment("Added to the base derived speed if the biome is cold.")
                .defineInRange("DERIVED_COLD", -0.2, -Double.MAX_VALUE, Double.MAX_VALUE);
            this.derivedWet = builder
                .comment("Added to the base derived speed if the biome is wet.")
                .defineInRange("DERIVED_WET", -0.2, -Double.MAX_VALUE, Double.MAX_VALUE);
            this.fireSourceBonusRange = builder
                .comment("The device will gain a bonus for each fire source within this range.")
                .defineInRange("FIRE_SOURCE_BONUS_RANGE", 2, 0, 16);
            this.fireSourceBonus = builder
                .comment("Added to the base derived speed for each fire source block in range.")
                .defineInRange("FIRE_SOURCE_BONUS", 0.2, -Double.MAX_VALUE, Double.MAX_VALUE);
            this.daytime = builder
                .comment("Added to the base derived speed if it isn't raining, the device has a",
                    "direct line of sight to the sky, and it's daytime.")
                .defineInRange("DAYTIME", 0.2, -Double.MAX_VALUE, Double.MAX_VALUE);
            builder.pop();
        }
    }

    public static final class Server {

        public final ModConfigSpec.IntValue compostDurationTicks;
        public final ModConfigSpec.DoubleValue tanningRackDurationModifier;
        public final ModConfigSpec.DoubleValue barrelDurationModifier;
        public final ModConfigSpec.DoubleValue soakingPotDurationModifier;
        public final DryingRackModifiers crudeDryingRackModifiers;
        public final ModConfigSpec.DoubleValue crudeDryingRackSpeedModifier;
        public final ModConfigSpec.DoubleValue crudeDryingRackDurationModifier;
        public final DryingRackModifiers dryingRackModifiers;
        public final ModConfigSpec.DoubleValue dryingRackSpeedModifier;
        public final ModConfigSpec.DoubleValue dryingRackDurationModifier;
        public final ModConfigSpec.DoubleValue pitKilnDurationModifier;
        public final ModConfigSpec.DoubleValue pitKilnVariableSpeedModifier;
        public final ModConfigSpec.IntValue campfireCookTimeTicks;
        public final ModConfigSpec.DoubleValue comfortSaturationModifier;
        public final ModConfigSpec.DoubleValue comfortHungerModifier;
        public final ModConfigSpec.DoubleValue wellFedExhaustionModifier;
        public final ModConfigSpec.DoubleValue focusedMaximumAccumulatedBonus;
        public final ModConfigSpec.DoubleValue focusedAccumulatedBonus;
        public final ModConfigSpec.DoubleValue focusedBonus;

        Server(ModConfigSpec.Builder builder) {
            builder.push("compost_bin");
            this.compostDurationTicks = builder
                .comment("How long does the process take in ticks.")
                .defineInRange("COMPOST_DURATION_TICKS", 24000 * 4, 1, Integer.MAX_VALUE);
            builder.pop();

            builder.push("tanning_rack");
            this.tanningRackDurationModifier = durationModifier(builder);
            builder.pop();

            builder.push("barrel");
            this.barrelDurationModifier = durationModifier(builder);
            builder.pop();

            builder.push("soaking_pot");
            this.soakingPotDurationModifier = durationModifier(builder);
            builder.pop();

            builder.push("crude_drying_rack");
            this.crudeDryingRackModifiers = new DryingRackModifiers(builder);
            this.crudeDryingRackSpeedModifier = builder
                .comment("speed = speed * SPEED_MODIFIER")
                .defineInRange("SPEED_MODIFIER", 1.0, 0, Double.MAX_VALUE);
            this.crudeDryingRackDurationModifier = durationModifier(builder);
            builder.pop();

            builder.push("drying_rack");
            this.dryingRackModifiers = new DryingRackModifiers(builder);
            this.dryingRackSpeedModifier = builder
                .comment("speed = speed * SPEED_MODIFIER")
                .defineInRange("SPEED_MODIFIER", 1.35, 0, Double.MAX_VALUE);
            this.dryingRackDurationModifier = durationModifier(builder);
            builder.pop();

            builder.push("pit_kiln");
            this.pitKilnDurationModifier = durationModifier(builder);
            this.pitKilnVariableSpeedModifier = builder
                .comment("SPEED_SCALAR=(1-VARIABLE_SPEED_MODIFIER)PERCENTAGE_FULL+VARIABLE_SPEED_MODIFIER",
                    "",
                    "If set to 0.5, the Pit Kiln will complete 1 item in 50% of the time.",
                    "For each item added after the first, the duration increases linearly",
                    "until it is 100% when full.",
                    "Setting the value to 0 is not recommended as it will cause one",
                    "item to complete instantly.")
                .defineInRange("VARIABLE_SPEED_MODIFIER", 0.5, 0, 1);
            builder.pop();

            builder.push("campfire");
            this.campfireCookTimeTicks = builder
                .comment("How many ticks to cook food on the campfire.")
                .defineInRange("COOK_TIME_TICKS", 90 * 20, 1, Integer.MAX_VALUE);
            builder.pop();

            builder.push("campfire_effects");
            this.comfortSaturationModifier = builder
                .comment("A percentile modifier for the amount of additional saturation restored",
                    "when a player eats food with the comfort effect.",
                    "Saturation restored = food saturation + food saturation * modifier")
                .defineInRange("COMFORT_SATURATION_MODIFIER", 0.5, 0, Double.MAX_VALUE);
            this.comfortHungerModifier = builder
                .comment("A percentile modifier for the amount of additional hunger restored",
                    "when a player eats food with the comfort effect.",
                    "Hunger restored = food hunger + food hunger * modifier")
                .defineInRange("COMFORT_HUNGER_MODIFIER", 0.5, 0, Double.MAX_VALUE);
            this.wellFedExhaustionModifier = builder
                .comment("Percentile exhaustion modifier for the Well Fed effect.")
                .defineInRange("WELL_FED_EXHAUSTION_MODIFIER", 0.5, 0, 1);
            this.focusedMaximumAccumulatedBonus = builder
                .comment("Maximum XP bonus that a player can accumulate.")
                .defineInRange("FOCUSED_MAXIMUM_ACCUMULATED_BONUS", 1.5, 0, Double.MAX_VALUE);
            this.focusedAccumulatedBonus = builder
                .comment("This defines how much XP bonus is accumulated per cycle. This effect",
                    "cycles at the same rate as the Resting III effect.")
                .defineInRange("FOCUSED_ACCUMULATED_BONUS", 0.05, 0, Double.MAX_VALUE);
            this.focusedBonus = builder
                .comment("Additional XP granted by the XP bonus on collection. The default is",
                    "100% additional XP, so effectively double the XP collected.")
                .defineInRange("FOCUSED_BONUS", 1.0, 0, Double.MAX_VALUE);
            builder.pop();
        }

        private static ModConfigSpec.DoubleValue durationModifier(ModConfigSpec.Builder builder) {
            return builder
                .comment("Multiplicative modifier applied to every recipe in this device.",
                    "recipeDurationTicks = recipeDurationTicks * BASE_RECIPE_DURATION_MODIFIER")
                .defineInRange("BASE_RECIPE_DURATION_MODIFIER", 1.0, 0, Double.MAX_VALUE);
        }

        /** A recipe's time under the device's multiplier, at least one tick, as the 1.12 {@code getTimeTicks} computed it. */
        public static int scaled(int ticks, ModConfigSpec.DoubleValue modifier) {
            return (int) Math.max(1, ticks * modifier.get());
        }
    }
}
