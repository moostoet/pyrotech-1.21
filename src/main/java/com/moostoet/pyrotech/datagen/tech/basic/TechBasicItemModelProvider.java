package com.moostoet.pyrotech.datagen.tech.basic;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.tech.basic.block.WearStage;
import com.moostoet.pyrotech.tech.basic.client.TechBasicClient;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.loaders.CompositeModelBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.util.TransformationHelper;

import java.util.List;

/**
 * The fifteen block item models. Most parent on their block model. The campfire item
 * is a composite of the 1.12 {@code item} variant's parts (tinder, fire, and eight
 * logs), the chopping block and the anvils switch models by their wear stage through
 * the {@code pyrotech:damage} predicate, and the barrel by {@code pyrotech:sealed}.
 * The seven plain items keep their migrated models.
 */
public final class TechBasicItemModelProvider extends ItemModelProvider {

    private static final List<String> CHOPPING_BLOCK_CORES = List.of("core", "core_2", "core_2", "core_3", "core_3", "core_4");
    private static final List<String> CHOPPING_BLOCK_BARKS = List.of("bark_a", "bark_b", "bark_c", "bark_d", "bark_e", "bark_f");

    public TechBasicItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Pyrotech.MOD_ID, existingFileHelper);
    }

    @Override
    public String getName() {
        return "Tech Basic Item Models: " + Pyrotech.MOD_ID;
    }

    @Override
    protected void registerModels() {
        this.campfire();
        this.choppingBlock();
        this.anvil("anvil_granite");
        this.anvil("anvil_iron_plated");
        this.anvil("anvil_obsidian");
        this.withExistingParent("barrel", this.modLoc("block/barrel"))
            .override().predicate(TechBasicClient.SEALED_PREDICATE, 1).model(this.getExistingFile(this.modLoc("block/barrel_sealed"))).end();
        this.withExistingParent("kiln_pit", this.modLoc("block/kiln_pit_empty"));
        this.withExistingParent("drying_rack_crude", this.modLoc("block/drying_rack_crude"));
        this.withExistingParent("drying_rack", this.modLoc("block/drying_rack"));
        this.withExistingParent("worktable", this.modLoc("block/worktable"));
        this.withExistingParent("worktable_stone", this.modLoc("block/worktable_stone"));
        this.withExistingParent("compacting_bin", this.modLoc("block/compacting_bin"));
        this.withExistingParent("soaking_pot", this.modLoc("block/soaking_pot"));
        this.withExistingParent("compost_bin", this.modLoc("block/gen/compost_bin/compost_bin"));
        this.withExistingParent("tanning_rack", this.modLoc("block/tanning_rack"));
    }

    private void campfire() {
        CompositeModelBuilder<ItemModelBuilder> composite = this.withExistingParent("campfire", this.mcLoc("block/block"))
            .customLoader(CompositeModelBuilder::begin)
            .child("tinder", this.part("block/gen/campfire/tinder", 0))
            .child("fire", this.part("block/gen/campfire/campfire_fire", 0));
        for (int i = 0; i < 4; i++) {
            composite.child("log_a_" + i, this.part("block/gen/campfire/campfire_log_a", 90 * i));
            composite.child("log_b_" + i, this.part("block/gen/campfire/campfire_log_b", 90 * i));
        }
    }

    private void choppingBlock() {
        ItemModelBuilder base = this.choppingBlockStage("chopping_block", 0);
        for (int damage = 1; damage <= 5; damage++) {
            ItemModelBuilder stage = this.choppingBlockStage("chopping_block_damage_" + damage, damage);
            base.override().predicate(WearStage.PREDICATE, damage).model(stage).end();
        }
    }

    private ItemModelBuilder choppingBlockStage(String name, int damage) {
        ItemModelBuilder builder = this.withExistingParent(name, this.mcLoc("block/block"));
        builder.customLoader(CompositeModelBuilder::begin)
            .child("core", this.part("block/gen/chopping_block/chopping_block_" + CHOPPING_BLOCK_CORES.get(damage), 0))
            .child("bark", this.part("block/gen/chopping_block/chopping_block_" + CHOPPING_BLOCK_BARKS.get(damage), 0));
        return builder;
    }

    private void anvil(String name) {
        ItemModelBuilder base = this.withExistingParent(name, this.modLoc("block/gen/" + name + "/anvil"));
        for (int damage = 1; damage <= 3; damage++) {
            base.override().predicate(WearStage.PREDICATE, damage)
                .model(this.getExistingFile(this.modLoc("block/gen/" + name + "/anvil_" + (damage + 1)))).end();
        }
    }

    /** A composite child on a block model, turned about the block's centre. */
    private ItemModelBuilder part(String blockModel, int rotationY) {
        ItemModelBuilder child = this.nested().parent(this.getExistingFile(this.modLoc(blockModel)));
        if (rotationY != 0) {
            child.rootTransforms().rotation(0, rotationY, 0, true).origin(TransformationHelper.TransformOrigin.CENTER);
        }
        return child;
    }
}
