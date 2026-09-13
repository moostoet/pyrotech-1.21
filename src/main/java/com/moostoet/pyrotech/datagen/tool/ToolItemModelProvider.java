package com.moostoet.pyrotech.datagen.tool;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.tool.ToolItems;
import com.moostoet.pyrotech.tool.item.ActiveBehaviour;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.List;

/**
 * The tool item models the migrated assets lack: the 1.12 files sat under a {@code tool/}
 * prefix that 1.21 has no place for. Handheld models for the tools and shears, flat ones
 * for the kits, the {@code pyrotech:active} override on the ten active tools, and the
 * {@code cast} override on the crude fishing rod. The two shields keep their static
 * {@code builtin/entity} models.
 */
public final class ToolItemModelProvider extends ItemModelProvider {

    public ToolItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Pyrotech.MOD_ID, existingFileHelper);
    }

    @Override
    public String getName() {
        return "Tool Item Models: " + Pyrotech.MOD_ID;
    }

    @Override
    protected void registerModels() {
        for (List<? extends DeferredItem<? extends Item>> tools : List.of(
            ToolItems.AXES, ToolItems.HOES, ToolItems.PICKAXES, ToolItems.SHOVELS, ToolItems.SWORDS, ToolItems.SHEARS)) {
            for (DeferredItem<? extends Item> tool : tools) {
                this.handheldItem(tool.get());
            }
        }
        this.handheldItem(ToolItems.UNFIRED_CLAY_SHEARS.get());
        for (DeferredItem<? extends Item> tool : ToolItems.ACTIVE_TOOLS) {
            String name = tool.getId().getPath();
            ItemModelBuilder active = this.handheld(name + "_active");
            this.getBuilder(name).override().predicate(ActiveBehaviour.MODEL_PREDICATE, 1).model(active).end();
        }
        this.basicItem(ToolItems.BONE_TOOL_REPAIR_KIT.get());
        this.basicItem(ToolItems.FLINT_TOOL_REPAIR_KIT.get());

        ItemModelBuilder cast = this.rod("crude_fishing_rod_cast", "crude_fishing_rod_cast");
        this.rod("crude_fishing_rod", "crude_fishing_rod_uncast")
            .override().predicate(ResourceLocation.withDefaultNamespace("cast"), 1).model(cast).end();
    }

    private ItemModelBuilder handheld(String name) {
        return this.singleTexture(name, this.mcLoc("item/handheld"), "layer0", this.modLoc("item/" + name));
    }

    private ItemModelBuilder rod(String name, String texture) {
        return this.singleTexture(name, this.mcLoc("item/handheld_rod"), "layer0", this.modLoc("item/" + texture));
    }
}
