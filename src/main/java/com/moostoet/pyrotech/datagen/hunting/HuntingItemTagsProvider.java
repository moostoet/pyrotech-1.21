package com.moostoet.pyrotech.datagen.hunting;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.core.CoreItems;
import com.moostoet.pyrotech.core.Material;
import com.moostoet.pyrotech.hunting.HuntingItems;
import com.moostoet.pyrotech.hunting.HuntingTags;
import com.moostoet.pyrotech.hunting.item.KnifeItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.concurrent.CompletableFuture;

/**
 * Hunting's item tags: the two knife families, the 1.12 {@code hideScrapeable} and
 * {@code hideSmallScrapeable} ore names, the repair kits, the carcass capture list
 * (sign-off item 2), and the {@code shard} ore name over core's eight shards.
 */
public final class HuntingItemTagsProvider extends ItemTagsProvider {

    public HuntingItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                   CompletableFuture<TagLookup<Block>> blockTags, ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, Pyrotech.MOD_ID, existingFileHelper);
    }

    @Override
    public String getName() {
        return "Hunting " + super.getName();
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        for (DeferredItem<KnifeItem> knife : HuntingItems.HUNTERS_KNIVES) {
            this.tag(HuntingTags.Items.HUNTERS_KNIVES).add(knife.get());
        }
        for (DeferredItem<KnifeItem> knife : HuntingItems.BUTCHERS_KNIVES) {
            this.tag(HuntingTags.Items.BUTCHERS_KNIVES).add(knife.get());
        }

        IntrinsicTagAppender<Item> scrapeable = this.tag(HuntingTags.Items.SCRAPEABLE_HIDES).add(
            HuntingItems.PELT_COW.get(),
            HuntingItems.PELT_HORSE.get(),
            HuntingItems.PELT_MOOSHROOM.get(),
            HuntingItems.PELT_POLAR_BEAR.get(),
            HuntingItems.HIDE_PIG.get(),
            HuntingItems.HIDE_SHEEP_SHEARED.get(),
            HuntingItems.HIDE_LLAMA.get(),
            HuntingItems.PELT_WOLF.get());
        for (DeferredItem<Item> pelt : HuntingItems.SHEEP_PELTS.values()) {
            scrapeable.add(pelt.get());
        }
        for (DeferredItem<Item> pelt : HuntingItems.LLAMA_PELTS.values()) {
            scrapeable.add(pelt.get());
        }
        this.tag(HuntingTags.Items.SMALL_SCRAPEABLE_HIDES).add(HuntingItems.PELT_BAT.get(), Items.RABBIT_HIDE);

        this.tag(HuntingTags.Items.LEATHER_REPAIR_KITS).add(HuntingItems.LEATHER_REPAIR_KIT.get(), HuntingItems.LEATHER_DURABLE_REPAIR_KIT.get());

        this.tag(HuntingTags.Items.CARCASS_CAPTURED).add(
            Items.BEEF, Items.CHICKEN, Items.MUTTON, Items.RABBIT, Items.RABBIT_FOOT, Items.PORKCHOP, Items.RED_MUSHROOM);

        this.material(HuntingTags.Items.SHARDS, Material.POTTERY_SHARD, Material.BONE_SHARD, Material.FLINT_SHARD, Material.GLASS_SHARD,
            Material.GOLD_SHARD, Material.DIAMOND_SHARD, Material.IRON_SHARD, Material.OBSIDIAN_SHARD);
    }

    private void material(TagKey<Item> tag, Material... materials) {
        IntrinsicTagAppender<Item> appender = this.tag(tag);
        for (Material material : materials) {
            appender.add(CoreItems.material(material).get());
        }
    }
}
