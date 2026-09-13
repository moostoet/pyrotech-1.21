package com.moostoet.pyrotech.tool;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.tool.item.ActiveBehaviour;
import com.moostoet.pyrotech.tool.item.CrudeFishingRodItem;
import com.moostoet.pyrotech.tool.item.PyrotechAxeItem;
import com.moostoet.pyrotech.tool.item.PyrotechHoeItem;
import com.moostoet.pyrotech.tool.item.PyrotechPickaxeItem;
import com.moostoet.pyrotech.tool.item.PyrotechShearsItem;
import com.moostoet.pyrotech.tool.item.PyrotechShieldItem;
import com.moostoet.pyrotech.tool.item.PyrotechShovelItem;
import com.moostoet.pyrotech.tool.item.PyrotechSwordItem;
import com.moostoet.pyrotech.tool.item.QuartzBehaviour;
import com.moostoet.pyrotech.tool.item.RedstoneBehaviour;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Tool's 50 items. Ids are the 1.12 registry names. Durabilities, attack values, and
 * enchantabilities are the 1.12 defaults, baked in (tool sign-off, items 1 and 2). Every
 * axe hits for 8 in total and the pickaxes, shovels, swords, and hoes keep vanilla's shape,
 * so the attack damage argument is the 1.12 total minus the tier's bonus.
 */
public final class ToolItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Pyrotech.MOD_ID);

    private static final float AXE_TOTAL_DAMAGE = 8.0f;

    // -- Crude: vanilla stone at half speed, no sword ------------------------

    public static final DeferredItem<PyrotechAxeItem> CRUDE_AXE = axe("crude_axe", ToolTiers.CRUDE, -3.2f, null);
    public static final DeferredItem<PyrotechHoeItem> CRUDE_HOE = hoe("crude_hoe", ToolTiers.CRUDE, -2.0f, null);
    public static final DeferredItem<PyrotechPickaxeItem> CRUDE_PICKAXE = pickaxe("crude_pickaxe", ToolTiers.CRUDE, null);
    public static final DeferredItem<PyrotechShovelItem> CRUDE_SHOVEL = shovel("crude_shovel", ToolTiers.CRUDE, null);
    public static final DeferredItem<CrudeFishingRodItem> CRUDE_FISHING_ROD = ITEMS.registerItem("crude_fishing_rod",
        CrudeFishingRodItem::new, new Item.Properties().durability(16));

    public static final DeferredItem<PyrotechShieldItem> CRUDE_SHIELD = ITEMS.registerItem("crude_shield",
        PyrotechShieldItem::new, new Item.Properties().durability(50));
    public static final DeferredItem<PyrotechShieldItem> DURABLE_SHIELD = ITEMS.registerItem("durable_shield",
        PyrotechShieldItem::new, new Item.Properties().durability(200));

    // -- Bone and flint, plain and durable -------------------------------------

    public static final DeferredItem<PyrotechAxeItem> BONE_AXE = axe("bone_axe", ToolTiers.BONE, -3.2f, null);
    public static final DeferredItem<PyrotechHoeItem> BONE_HOE = hoe("bone_hoe", ToolTiers.BONE, -2.0f, null);
    public static final DeferredItem<PyrotechPickaxeItem> BONE_PICKAXE = pickaxe("bone_pickaxe", ToolTiers.BONE, null);
    public static final DeferredItem<PyrotechShovelItem> BONE_SHOVEL = shovel("bone_shovel", ToolTiers.BONE, null);
    public static final DeferredItem<PyrotechSwordItem> BONE_SWORD = sword("bone_sword", ToolTiers.BONE, null);
    public static final DeferredItem<PyrotechAxeItem> BONE_AXE_DURABLE = axe("bone_axe_durable", ToolTiers.BONE_DURABLE, -3.2f, null);
    public static final DeferredItem<PyrotechHoeItem> BONE_HOE_DURABLE = hoe("bone_hoe_durable", ToolTiers.BONE_DURABLE, -2.0f, null);
    public static final DeferredItem<PyrotechPickaxeItem> BONE_PICKAXE_DURABLE = pickaxe("bone_pickaxe_durable", ToolTiers.BONE_DURABLE, null);
    public static final DeferredItem<PyrotechShovelItem> BONE_SHOVEL_DURABLE = shovel("bone_shovel_durable", ToolTiers.BONE_DURABLE, null);

    public static final DeferredItem<PyrotechAxeItem> FLINT_AXE = axe("flint_axe", ToolTiers.FLINT, -3.2f, null);
    public static final DeferredItem<PyrotechHoeItem> FLINT_HOE = hoe("flint_hoe", ToolTiers.FLINT, -2.0f, null);
    public static final DeferredItem<PyrotechPickaxeItem> FLINT_PICKAXE = pickaxe("flint_pickaxe", ToolTiers.FLINT, null);
    public static final DeferredItem<PyrotechShovelItem> FLINT_SHOVEL = shovel("flint_shovel", ToolTiers.FLINT, null);
    public static final DeferredItem<PyrotechSwordItem> FLINT_SWORD = sword("flint_sword", ToolTiers.FLINT, null);
    public static final DeferredItem<PyrotechAxeItem> FLINT_AXE_DURABLE = axe("flint_axe_durable", ToolTiers.FLINT_DURABLE, -3.2f, null);
    public static final DeferredItem<PyrotechHoeItem> FLINT_HOE_DURABLE = hoe("flint_hoe_durable", ToolTiers.FLINT_DURABLE, -2.0f, null);
    public static final DeferredItem<PyrotechPickaxeItem> FLINT_PICKAXE_DURABLE = pickaxe("flint_pickaxe_durable", ToolTiers.FLINT_DURABLE, null);
    public static final DeferredItem<PyrotechShovelItem> FLINT_SHOVEL_DURABLE = shovel("flint_shovel_durable", ToolTiers.FLINT_DURABLE, null);

    // -- Obsidian: the iron-level tier, with the higher damage bonus -----------

    public static final DeferredItem<PyrotechAxeItem> OBSIDIAN_AXE = axe("obsidian_axe", ToolTiers.OBSIDIAN, -3.0f, null);
    public static final DeferredItem<PyrotechHoeItem> OBSIDIAN_HOE = hoe("obsidian_hoe", ToolTiers.OBSIDIAN, -1.0f, null);
    public static final DeferredItem<PyrotechPickaxeItem> OBSIDIAN_PICKAXE = pickaxe("obsidian_pickaxe", ToolTiers.OBSIDIAN, null);
    public static final DeferredItem<PyrotechShovelItem> OBSIDIAN_SHOVEL = shovel("obsidian_shovel", ToolTiers.OBSIDIAN, null);
    public static final DeferredItem<PyrotechSwordItem> OBSIDIAN_SWORD = sword("obsidian_sword", ToolTiers.OBSIDIAN, null);

    // -- Redstone and quartz: the active tools ---------------------------------

    public static final DeferredItem<PyrotechAxeItem> REDSTONE_AXE = axe("redstone_axe", ToolTiers.REDSTONE, -2.8f, RedstoneBehaviour.INSTANCE);
    public static final DeferredItem<PyrotechHoeItem> REDSTONE_HOE = hoe("redstone_hoe", ToolTiers.REDSTONE, -2.0f, RedstoneBehaviour.INSTANCE);
    public static final DeferredItem<PyrotechPickaxeItem> REDSTONE_PICKAXE = pickaxe("redstone_pickaxe", ToolTiers.REDSTONE, RedstoneBehaviour.INSTANCE);
    public static final DeferredItem<PyrotechShovelItem> REDSTONE_SHOVEL = shovel("redstone_shovel", ToolTiers.REDSTONE, RedstoneBehaviour.INSTANCE);
    public static final DeferredItem<PyrotechSwordItem> REDSTONE_SWORD = sword("redstone_sword", ToolTiers.REDSTONE, RedstoneBehaviour.INSTANCE);

    public static final DeferredItem<PyrotechAxeItem> QUARTZ_AXE = axe("quartz_axe", ToolTiers.QUARTZ, -2.8f, QuartzBehaviour.INSTANCE);
    public static final DeferredItem<PyrotechHoeItem> QUARTZ_HOE = hoe("quartz_hoe", ToolTiers.QUARTZ, -2.0f, QuartzBehaviour.INSTANCE);
    public static final DeferredItem<PyrotechPickaxeItem> QUARTZ_PICKAXE = pickaxe("quartz_pickaxe", ToolTiers.QUARTZ, QuartzBehaviour.INSTANCE);
    public static final DeferredItem<PyrotechShovelItem> QUARTZ_SHOVEL = shovel("quartz_shovel", ToolTiers.QUARTZ, QuartzBehaviour.INSTANCE);
    public static final DeferredItem<PyrotechSwordItem> QUARTZ_SWORD = sword("quartz_sword", ToolTiers.QUARTZ, QuartzBehaviour.INSTANCE);

    // -- Shears, repair kits ---------------------------------------------------

    public static final DeferredItem<Item> UNFIRED_CLAY_SHEARS = ITEMS.registerSimpleItem("unfired_clay_shears", new Item.Properties().stacksTo(1));
    public static final DeferredItem<PyrotechShearsItem> CLAY_SHEARS = shears("clay_shears", 60, 0);
    public static final DeferredItem<PyrotechShearsItem> STONE_SHEARS = shears("stone_shears", 90, 0);
    public static final DeferredItem<PyrotechShearsItem> BONE_SHEARS = shears("bone_shears", 120, 0);
    public static final DeferredItem<PyrotechShearsItem> FLINT_SHEARS = shears("flint_shears", 120, 0);
    public static final DeferredItem<PyrotechShearsItem> GOLD_SHEARS = shears("gold_shears", 30, Tiers.GOLD.getEnchantmentValue());
    public static final DeferredItem<PyrotechShearsItem> DIAMOND_SHEARS = shears("diamond_shears", 952, 0);
    public static final DeferredItem<PyrotechShearsItem> OBSIDIAN_SHEARS = shears("obsidian_shears", 852, 0);

    /** Four uses each: the 1.12 kit config, baked (tool sign-off, item 2). */
    public static final DeferredItem<Item> BONE_TOOL_REPAIR_KIT = ITEMS.registerSimpleItem("bone_tool_repair_kit",
        new Item.Properties().stacksTo(1).durability(4));
    public static final DeferredItem<Item> FLINT_TOOL_REPAIR_KIT = ITEMS.registerSimpleItem("flint_tool_repair_kit",
        new Item.Properties().stacksTo(1).durability(4));

    // -- The groups datagen and the client read --------------------------------

    public static final List<DeferredItem<PyrotechAxeItem>> AXES = List.of(
        CRUDE_AXE, BONE_AXE, BONE_AXE_DURABLE, FLINT_AXE, FLINT_AXE_DURABLE, OBSIDIAN_AXE, REDSTONE_AXE, QUARTZ_AXE);
    public static final List<DeferredItem<PyrotechHoeItem>> HOES = List.of(
        CRUDE_HOE, BONE_HOE, BONE_HOE_DURABLE, FLINT_HOE, FLINT_HOE_DURABLE, OBSIDIAN_HOE, REDSTONE_HOE, QUARTZ_HOE);
    public static final List<DeferredItem<PyrotechPickaxeItem>> PICKAXES = List.of(
        CRUDE_PICKAXE, BONE_PICKAXE, BONE_PICKAXE_DURABLE, FLINT_PICKAXE, FLINT_PICKAXE_DURABLE, OBSIDIAN_PICKAXE, REDSTONE_PICKAXE, QUARTZ_PICKAXE);
    public static final List<DeferredItem<PyrotechShovelItem>> SHOVELS = List.of(
        CRUDE_SHOVEL, BONE_SHOVEL, BONE_SHOVEL_DURABLE, FLINT_SHOVEL, FLINT_SHOVEL_DURABLE, OBSIDIAN_SHOVEL, REDSTONE_SHOVEL, QUARTZ_SHOVEL);
    public static final List<DeferredItem<PyrotechSwordItem>> SWORDS = List.of(
        BONE_SWORD, FLINT_SWORD, OBSIDIAN_SWORD, REDSTONE_SWORD, QUARTZ_SWORD);
    /** The ten redstone and quartz tools, which carry the {@code pyrotech:active} model predicate. */
    public static final List<DeferredItem<? extends Item>> ACTIVE_TOOLS = List.of(
        REDSTONE_AXE, REDSTONE_HOE, REDSTONE_PICKAXE, REDSTONE_SHOVEL, REDSTONE_SWORD,
        QUARTZ_AXE, QUARTZ_HOE, QUARTZ_PICKAXE, QUARTZ_SHOVEL, QUARTZ_SWORD);
    /** The seven working shears; the unfired clay shears are a plain item. */
    public static final List<DeferredItem<PyrotechShearsItem>> SHEARS = List.of(
        CLAY_SHEARS, STONE_SHEARS, BONE_SHEARS, FLINT_SHEARS, GOLD_SHEARS, DIAMOND_SHEARS, OBSIDIAN_SHEARS);
    public static final List<DeferredItem<PyrotechShieldItem>> SHIELDS = List.of(CRUDE_SHIELD, DURABLE_SHIELD);

    /** The 1.12 registration order, which was its creative tab order. */
    private static final List<DeferredItem<? extends Item>> TAB_ORDER = List.of(
        CRUDE_AXE, CRUDE_HOE, CRUDE_PICKAXE, CRUDE_SHOVEL, CRUDE_FISHING_ROD, CRUDE_SHIELD, DURABLE_SHIELD,
        BONE_AXE, BONE_HOE, BONE_PICKAXE, BONE_SHOVEL, BONE_SWORD,
        BONE_AXE_DURABLE, BONE_HOE_DURABLE, BONE_PICKAXE_DURABLE, BONE_SHOVEL_DURABLE,
        FLINT_AXE, FLINT_HOE, FLINT_PICKAXE, FLINT_SHOVEL, FLINT_SWORD,
        FLINT_AXE_DURABLE, FLINT_HOE_DURABLE, FLINT_PICKAXE_DURABLE, FLINT_SHOVEL_DURABLE,
        REDSTONE_AXE, REDSTONE_HOE, REDSTONE_PICKAXE, REDSTONE_SHOVEL, REDSTONE_SWORD,
        QUARTZ_AXE, QUARTZ_HOE, QUARTZ_PICKAXE, QUARTZ_SHOVEL, QUARTZ_SWORD,
        OBSIDIAN_AXE, OBSIDIAN_HOE, OBSIDIAN_PICKAXE, OBSIDIAN_SHOVEL, OBSIDIAN_SWORD,
        UNFIRED_CLAY_SHEARS, CLAY_SHEARS, STONE_SHEARS, BONE_SHEARS, FLINT_SHEARS, GOLD_SHEARS, DIAMOND_SHEARS, OBSIDIAN_SHEARS,
        BONE_TOOL_REPAIR_KIT, FLINT_TOOL_REPAIR_KIT);

    private ToolItems() {
    }

    private static DeferredItem<PyrotechAxeItem> axe(String name, Tier tier, float attackSpeed, @Nullable ActiveBehaviour active) {
        return ITEMS.registerItem(name, properties -> new PyrotechAxeItem(tier, active,
            properties.attributes(AxeItem.createAttributes(tier, AXE_TOTAL_DAMAGE - tier.getAttackDamageBonus(), attackSpeed))));
    }

    private static DeferredItem<PyrotechPickaxeItem> pickaxe(String name, Tier tier, @Nullable ActiveBehaviour active) {
        return ITEMS.registerItem(name, properties -> new PyrotechPickaxeItem(tier, active,
            properties.attributes(PickaxeItem.createAttributes(tier, 1.0f, -2.8f))));
    }

    private static DeferredItem<PyrotechShovelItem> shovel(String name, Tier tier, @Nullable ActiveBehaviour active) {
        return ITEMS.registerItem(name, properties -> new PyrotechShovelItem(tier, active,
            properties.attributes(ShovelItem.createAttributes(tier, 1.5f, -3.0f))));
    }

    /** A 1.12 hoe added no attack damage, and its speed was the tier's bonus less two. */
    private static DeferredItem<PyrotechHoeItem> hoe(String name, Tier tier, float attackSpeed, @Nullable ActiveBehaviour active) {
        return ITEMS.registerItem(name, properties -> new PyrotechHoeItem(tier, active,
            properties.attributes(HoeItem.createAttributes(tier, -tier.getAttackDamageBonus(), attackSpeed))));
    }

    private static DeferredItem<PyrotechSwordItem> sword(String name, Tier tier, @Nullable ActiveBehaviour active) {
        return ITEMS.registerItem(name, properties -> new PyrotechSwordItem(tier, active, properties));
    }

    private static DeferredItem<PyrotechShearsItem> shears(String name, int durability, int enchantmentValue) {
        return ITEMS.registerItem(name, properties -> new PyrotechShearsItem(enchantmentValue, properties),
            new Item.Properties().durability(durability));
    }

    static void addToTab(CreativeModeTab.Output output) {
        for (DeferredItem<? extends Item> item : TAB_ORDER) {
            output.accept(item.get());
        }
    }
}
