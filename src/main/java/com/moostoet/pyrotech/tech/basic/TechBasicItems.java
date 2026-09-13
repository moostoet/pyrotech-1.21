package com.moostoet.pyrotech.tech.basic;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.tech.basic.item.MarshmallowItem;
import com.moostoet.pyrotech.tech.basic.item.MarshmallowStickEmptyItem;
import com.moostoet.pyrotech.tech.basic.item.MarshmallowStickItem;
import com.moostoet.pyrotech.tech.basic.item.MarshmallowType;
import com.moostoet.pyrotech.tech.basic.item.TinderItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/** Tech/basic's seven items. The stick with a marshmallow on it stays out of the tab, as in 1.12. */
public final class TechBasicItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Pyrotech.MOD_ID);

    public static final DeferredItem<TinderItem> TINDER = ITEMS.registerItem("tinder", TinderItem::new);
    public static final DeferredItem<MarshmallowItem> MARSHMALLOW = ITEMS.registerItem("marshmallow",
        properties -> new MarshmallowItem(MarshmallowType.PLAIN, properties));
    public static final DeferredItem<MarshmallowItem> MARSHMALLOW_ROASTED = ITEMS.registerItem("marshmallow_roasted",
        properties -> new MarshmallowItem(MarshmallowType.ROASTED, properties), new Item.Properties().stacksTo(1));
    public static final DeferredItem<MarshmallowItem> MARSHMALLOW_BURNED = ITEMS.registerItem("marshmallow_burned",
        properties -> new MarshmallowItem(MarshmallowType.BURNED, properties));
    public static final DeferredItem<MarshmallowStickEmptyItem> MARSHMALLOW_STICK_EMPTY = ITEMS.registerItem("marshmallow_stick_empty",
        MarshmallowStickEmptyItem::new, new Item.Properties().stacksTo(1).durability(8));
    public static final DeferredItem<MarshmallowStickItem> MARSHMALLOW_STICK = ITEMS.registerItem("marshmallow_stick",
        MarshmallowStickItem::new, new Item.Properties().stacksTo(1).durability(8));
    public static final DeferredItem<Item> BARREL_LID = ITEMS.registerSimpleItem("barrel_lid");

    private static final List<DeferredItem<? extends Item>> TAB_ORDER = List.of(
        TINDER, MARSHMALLOW, MARSHMALLOW_ROASTED, MARSHMALLOW_BURNED, MARSHMALLOW_STICK_EMPTY, BARREL_LID);

    private TechBasicItems() {
    }

    static void addToTab(CreativeModeTab.Output output) {
        for (DeferredItem<? extends BlockItem> item : TechBasicBlocks.BLOCK_ITEMS) {
            output.accept(item.get());
        }
        for (DeferredItem<? extends Item> item : TAB_ORDER) {
            output.accept(item.get());
        }
    }
}
