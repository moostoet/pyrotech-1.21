package com.moostoet.pyrotech.datagen.tool;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.core.PyrotechTags;
import com.moostoet.pyrotech.tool.ToolItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** The vanilla and {@code c:} tags tool's items join (tool sign-off, item 8). */
public final class ToolItemTagsProvider extends ItemTagsProvider {

    public ToolItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                CompletableFuture<TagLookup<Block>> blockTags, ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, Pyrotech.MOD_ID, existingFileHelper);
    }

    @Override
    public String getName() {
        return "Tool " + super.getName();
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // The vanilla type tags. The chopping block recipe reads #minecraft:axes, and the
        // enchantable/mining, mining_loot, sharp_weapon, sword, and durability tags read all five.
        this.add(ItemTags.AXES, ToolItems.AXES);
        this.add(ItemTags.PICKAXES, ToolItems.PICKAXES);
        this.add(ItemTags.SHOVELS, ToolItems.SHOVELS);
        this.add(ItemTags.HOES, ToolItems.HOES);
        this.add(ItemTags.SWORDS, ToolItems.SWORDS);
        // Hunting's knives are swords too; a tag file has one writer, so they join here.
        this.tag(ItemTags.SWORDS).addTag(PyrotechTags.Items.KNIVES);
        // Vanilla names its own shears and shield by id in the enchantable tags, so these do
        // too. The crude fishing rod joins no enchantable tag, as in 1.12.
        this.add(ItemTags.DURABILITY_ENCHANTABLE, ToolItems.SHEARS);
        this.add(ItemTags.DURABILITY_ENCHANTABLE, ToolItems.SHIELDS);
        this.add(ItemTags.MINING_ENCHANTABLE, ToolItems.SHEARS);
        // For other mods, mirroring what NeoForge puts vanilla's tools in.
        this.add(Tags.Items.TOOLS_SHEAR, ToolItems.SHEARS);
        this.add(Tags.Items.TOOLS_SHIELD, ToolItems.SHIELDS);
        this.tag(Tags.Items.TOOLS_FISHING_ROD).add(ToolItems.CRUDE_FISHING_ROD.get());
        this.add(Tags.Items.MINING_TOOL_TOOLS, ToolItems.PICKAXES);
        this.add(Tags.Items.MELEE_WEAPON_TOOLS, ToolItems.SWORDS);
        this.tag(Tags.Items.MELEE_WEAPON_TOOLS).addTag(PyrotechTags.Items.KNIVES);
        this.add(Tags.Items.MELEE_WEAPON_TOOLS, ToolItems.AXES);
    }

    private void add(TagKey<Item> tag, List<? extends DeferredItem<? extends Item>> items) {
        IntrinsicTagAppender<Item> appender = this.tag(tag);
        for (DeferredItem<? extends Item> item : items) {
            appender.add(item.get());
        }
    }
}
