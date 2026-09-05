package com.moostoet.pyrotech.core;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.core.item.HammerItem;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Core's items. Ids are the 1.12 registry names, with the material subtypes flattened to
 * one item each. Food values, durabilities, and effects are the 1.12 config defaults,
 * baked in (core sign-off, item 3).
 */
public final class CoreItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Pyrotech.MOD_ID);

    private static final Map<Material, DeferredItem<Item>> MATERIALS = new EnumMap<>(Material.class);

    static {
        for (Material material : Material.values()) {
            MATERIALS.put(material, ITEMS.registerSimpleItem(material.id()));
        }
    }

    public static final DeferredItem<HammerItem> CRUDE_HAMMER = hammer("crude_hammer", 32);
    public static final DeferredItem<HammerItem> STONE_HAMMER = hammer("stone_hammer", 150);
    public static final DeferredItem<HammerItem> BONE_HAMMER = hammer("bone_hammer", 150);
    public static final DeferredItem<HammerItem> BONE_HAMMER_DURABLE = hammer("bone_hammer_durable", 600);
    public static final DeferredItem<HammerItem> FLINT_HAMMER = hammer("flint_hammer", 150);
    public static final DeferredItem<HammerItem> FLINT_HAMMER_DURABLE = hammer("flint_hammer_durable", 600);
    public static final DeferredItem<HammerItem> IRON_HAMMER = hammer("iron_hammer", 750);
    public static final DeferredItem<HammerItem> GOLD_HAMMER = hammer("gold_hammer", 33);
    public static final DeferredItem<HammerItem> DIAMOND_HAMMER = hammer("diamond_hammer", 4500);
    public static final DeferredItem<HammerItem> OBSIDIAN_HAMMER = hammer("obsidian_hammer", 4035);

    public static final List<DeferredItem<HammerItem>> HAMMERS = List.of(
        CRUDE_HAMMER, STONE_HAMMER, BONE_HAMMER, BONE_HAMMER_DURABLE, FLINT_HAMMER, FLINT_HAMMER_DURABLE,
        IRON_HAMMER, GOLD_HAMMER, DIAMOND_HAMMER, OBSIDIAN_HAMMER);

    public static final DeferredItem<Item> APPLE_BAKED = food("apple_baked", 6, 0.45f);
    public static final DeferredItem<Item> CARROT_ROASTED = food("carrot_roasted", 5, 0.9f);
    public static final DeferredItem<Item> EGG_ROASTED = food("egg_roasted", 6, 0.6f);
    public static final DeferredItem<Item> MUSHROOM_BROWN_ROASTED = food("mushroom_brown_roasted", 5, 0.6f);
    public static final DeferredItem<Item> MUSHROOM_RED_ROASTED = food("mushroom_red_roasted", 5, 0.6f);
    public static final DeferredItem<Item> BEETROOT_ROASTED = food("beetroot_roasted", 2, 0.9f);
    public static final DeferredItem<Item> STRANGE_TUBER = food("strange_tuber", 2, 0.2f);
    public static final DeferredItem<Item> BURNED_FOOD = ITEMS.registerSimpleItem("burned_food",
        new Item.Properties().food(food(2, 0.1f)
            .effect(() -> new MobEffectInstance(MobEffects.HUNGER, 600), 0.95f)
            .build()));
    public static final DeferredItem<Item> TAINTED_MEAT = ITEMS.registerSimpleItem("tainted_meat",
        new Item.Properties().food(food(1, 0.05f)
            .effect(() -> new MobEffectInstance(MobEffects.POISON, 600), 0.95f)
            .build()));

    public static final DeferredItem<Item> FURNACE_CORE = ITEMS.registerSimpleItem("furnace_core");

    private CoreItems() {
    }

    public static DeferredItem<Item> material(Material material) {
        return MATERIALS.get(material);
    }

    private static DeferredItem<HammerItem> hammer(String name, int durability) {
        return ITEMS.registerItem(name, HammerItem::new, new Item.Properties().durability(durability));
    }

    private static FoodProperties.Builder food(int nutrition, float saturationModifier) {
        return new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturationModifier);
    }

    private static DeferredItem<Item> food(String name, int nutrition, float saturationModifier) {
        return ITEMS.registerSimpleItem(name, new Item.Properties().food(food(nutrition, saturationModifier).build()));
    }

    static void addToTab(CreativeModeTab.Output output) {
        for (DeferredItem<HammerItem> hammer : HAMMERS) {
            output.accept(hammer.get());
        }
        for (DeferredItem<Item> material : MATERIALS.values()) {
            output.accept(material.get());
        }
        output.accept(APPLE_BAKED.get());
        output.accept(CARROT_ROASTED.get());
        output.accept(EGG_ROASTED.get());
        output.accept(MUSHROOM_BROWN_ROASTED.get());
        output.accept(MUSHROOM_RED_ROASTED.get());
        output.accept(BEETROOT_ROASTED.get());
        output.accept(STRANGE_TUBER.get());
        output.accept(BURNED_FOOD.get());
        output.accept(TAINTED_MEAT.get());
        output.accept(FURNACE_CORE.get());
    }
}
