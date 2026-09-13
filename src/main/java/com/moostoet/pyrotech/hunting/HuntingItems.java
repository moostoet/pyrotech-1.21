package com.moostoet.pyrotech.hunting;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.hunting.item.KnifeItem;
import com.moostoet.pyrotech.hunting.item.PyrotechArrowItem;
import com.moostoet.pyrotech.hunting.item.ScrapedHideItem;
import com.moostoet.pyrotech.hunting.item.SpearItem;
import com.moostoet.pyrotech.tool.ToolTiers;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Hunting's items in the 1.12 registration order. Ids are the 1.12 registry names. The
 * knives take tool's bone, flint, and obsidian tiers and vanilla's for the rest (hunting
 * sign-off, item 9); the spear, arrow, and kit numbers are the 1.12 config defaults, baked.
 */
public final class HuntingItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Pyrotech.MOD_ID);

    private static final List<DeferredItem<? extends Item>> TAB_ORDER = new ArrayList<>();

    // -- Hides -----------------------------------------------------------------

    public static final DeferredItem<Item> HIDE_PIG = simple("hide_pig");
    public static final DeferredItem<Item> HIDE_SHEEP_SHEARED = simple("hide_sheep_sheared");
    public static final DeferredItem<Item> HIDE_WASHED = simple("hide_washed");
    public static final DeferredItem<Item> HIDE_SMALL_WASHED = simple("hide_small_washed");
    public static final DeferredItem<Item> HIDE_LLAMA = simple("hide_llama");
    public static final DeferredItem<Item> HIDE_TANNED = simple("hide_tanned");
    public static final DeferredItem<Item> HIDE_SMALL_TANNED = simple("hide_small_tanned");
    public static final DeferredItem<ScrapedHideItem> HIDE_SCRAPED = ordered(ITEMS.registerItem("hide_scraped",
        properties -> new ScrapedHideItem(HIDE_WASHED, properties)));
    public static final DeferredItem<ScrapedHideItem> HIDE_SMALL_SCRAPED = ordered(ITEMS.registerItem("hide_small_scraped",
        properties -> new ScrapedHideItem(HIDE_SMALL_WASHED, properties)));

    // -- Pelts -----------------------------------------------------------------

    public static final DeferredItem<Item> PELT_RUINED = simple("pelt_ruined");
    public static final DeferredItem<Item> PELT_COW = simple("pelt_cow");
    public static final DeferredItem<Item> PELT_MOOSHROOM = simple("pelt_mooshroom");
    public static final DeferredItem<Item> PELT_POLAR_BEAR = simple("pelt_polar_bear");
    public static final DeferredItem<Item> PELT_BAT = simple("pelt_bat");
    public static final DeferredItem<Item> PELT_HORSE = simple("pelt_horse");
    public static final DeferredItem<Item> PELT_WOLF = simple("pelt_wolf");

    /** The sixteen sheep pelts by fleece colour, in the 1.12 registration order. */
    public static final Map<DyeColor, DeferredItem<Item>> SHEEP_PELTS = sheepPelts();
    /** The four llama pelts by variant, in the 1.12 registration order. */
    public static final Map<Llama.Variant, DeferredItem<Item>> LLAMA_PELTS = llamaPelts();

    // -- Knives ----------------------------------------------------------------

    public static final List<DeferredItem<KnifeItem>> HUNTERS_KNIVES = knives("hunters_knife", HuntingConfig.COMMON.allowHuntersKnifeRepair);
    public static final List<DeferredItem<KnifeItem>> BUTCHERS_KNIVES = knives("butchers_knife", HuntingConfig.COMMON.allowButchersKnifeRepair);

    // -- Leather kits, arrows, spears, the egg -----------------------------------

    /** Eight uses, four for the plain kit: the 1.12 kit config, baked. */
    public static final DeferredItem<Item> LEATHER_DURABLE_REPAIR_KIT = simple("leather_durable_repair_kit",
        new Item.Properties().stacksTo(1).durability(8));
    public static final DeferredItem<Item> LEATHER_DURABLE_UPGRADE_KIT = simple("leather_durable_upgrade_kit",
        new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> LEATHER_REPAIR_KIT = simple("leather_repair_kit",
        new Item.Properties().stacksTo(1).durability(4));

    public static final DeferredItem<PyrotechArrowItem> FLINT_ARROW = ordered(ITEMS.registerItem("flint_arrow",
        properties -> new PyrotechArrowItem(HuntingEntities.FLINT_ARROW, properties), new Item.Properties().stacksTo(16)));
    public static final DeferredItem<PyrotechArrowItem> BONE_ARROW = ordered(ITEMS.registerItem("bone_arrow",
        properties -> new PyrotechArrowItem(HuntingEntities.BONE_ARROW, properties), new Item.Properties().stacksTo(16)));

    public static final DeferredItem<SpearItem> CRUDE_SPEAR = spear("crude_spear", 16, 1.0, 2.0, 2.0);
    public static final DeferredItem<SpearItem> FLINT_SPEAR = spear("flint_spear", 32, 2.0, 1.0, 4.0);
    public static final DeferredItem<SpearItem> BONE_SPEAR = spear("bone_spear", 32, 2.0, 1.0, 4.0);

    /** The 1.12 egg colours. */
    public static final DeferredItem<DeferredSpawnEggItem> MUD_SPAWN_EGG = ordered(ITEMS.registerItem("mud_spawn_egg",
        properties -> new DeferredSpawnEggItem(HuntingEntities.MUD, 0x2b1a0b, 0xa25625, properties)));

    // -- The groups datagen reads ----------------------------------------------

    public static final List<DeferredItem<Item>> HIDES = List.of(
        HIDE_PIG, HIDE_SHEEP_SHEARED, HIDE_WASHED, HIDE_SMALL_WASHED, HIDE_LLAMA, HIDE_TANNED, HIDE_SMALL_TANNED);
    public static final List<DeferredItem<SpearItem>> SPEARS = List.of(CRUDE_SPEAR, FLINT_SPEAR, BONE_SPEAR);

    private HuntingItems() {
    }

    private static <I extends Item> DeferredItem<I> ordered(DeferredItem<I> item) {
        TAB_ORDER.add(item);
        return item;
    }

    private static DeferredItem<Item> simple(String name) {
        return ordered(ITEMS.registerSimpleItem(name));
    }

    private static DeferredItem<Item> simple(String name, Item.Properties properties) {
        return ordered(ITEMS.registerSimpleItem(name, properties));
    }

    private static Map<DyeColor, DeferredItem<Item>> sheepPelts() {
        Map<DyeColor, DeferredItem<Item>> pelts = new EnumMap<>(DyeColor.class);
        for (DyeColor color : List.of(DyeColor.YELLOW, DyeColor.WHITE, DyeColor.LIGHT_GRAY, DyeColor.RED, DyeColor.PURPLE, DyeColor.PINK,
            DyeColor.ORANGE, DyeColor.MAGENTA, DyeColor.LIME, DyeColor.LIGHT_BLUE, DyeColor.GREEN, DyeColor.GRAY, DyeColor.CYAN,
            DyeColor.BROWN, DyeColor.BLUE, DyeColor.BLACK)) {
            pelts.put(color, simple("pelt_sheep_" + sheepPeltName(color)));
        }
        return Map.copyOf(pelts);
    }

    /** The 1.12 ids put the shade after the colour. */
    private static String sheepPeltName(DyeColor color) {
        return switch (color) {
            case LIGHT_GRAY -> "gray_light";
            case LIGHT_BLUE -> "blue_light";
            default -> color.getName();
        };
    }

    private static Map<Llama.Variant, DeferredItem<Item>> llamaPelts() {
        Map<Llama.Variant, DeferredItem<Item>> pelts = new EnumMap<>(Llama.Variant.class);
        for (Llama.Variant variant : List.of(Llama.Variant.WHITE, Llama.Variant.CREAMY, Llama.Variant.GRAY, Llama.Variant.BROWN)) {
            pelts.put(variant, simple("pelt_llama_" + variant.getSerializedName()));
        }
        return Map.copyOf(pelts);
    }

    private static List<DeferredItem<KnifeItem>> knives(String kind, ModConfigSpec.BooleanValue repairAllowed) {
        return List.of(
            knife("bone_" + kind, ToolTiers.BONE, repairAllowed),
            knife("flint_" + kind, ToolTiers.FLINT, repairAllowed),
            knife("stone_" + kind, Tiers.STONE, repairAllowed),
            knife("iron_" + kind, Tiers.IRON, repairAllowed),
            knife("gold_" + kind, Tiers.GOLD, repairAllowed),
            knife("diamond_" + kind, Tiers.DIAMOND, repairAllowed),
            knife("obsidian_" + kind, ToolTiers.OBSIDIAN, repairAllowed));
    }

    private static DeferredItem<KnifeItem> knife(String name, Tier tier, ModConfigSpec.BooleanValue repairAllowed) {
        return ordered(ITEMS.registerItem(name, properties -> new KnifeItem(tier, repairAllowed, properties)));
    }

    private static DeferredItem<SpearItem> spear(String name, int durability, double velocityScalar, double inaccuracy, double thrownDamage) {
        return ordered(ITEMS.registerItem(name, properties -> new SpearItem(velocityScalar, inaccuracy, thrownDamage, properties),
            new Item.Properties().stacksTo(1).durability(durability)));
    }

    /** The 1.12 order: the fluid's bucket, the blocks, the items, then the entity's egg. */
    static void addToTab(CreativeModeTab.Output output) {
        output.accept(HuntingFluids.TANNIN.bucket().get());
        output.accept(HuntingBlocks.CARCASS_ITEM.get());
        output.accept(HuntingBlocks.BUTCHERS_BLOCK_ITEM.get());
        for (DeferredItem<? extends Item> item : TAB_ORDER) {
            output.accept(item.get());
        }
    }
}
