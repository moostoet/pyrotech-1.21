package com.moostoet.pyrotech.datagen.storage;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.storage.block.BagBlock;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * The thirteen storage block item models the migration could not derive, since the 1.12
 * storage blockstates had no inventory variant: each parents on its block model, the tanks
 * and faucets on the generated wrappers, and the bags switch to the open model on the
 * {@code pyrotech:open} predicate (storage sign-off, item 5).
 */
public final class StorageItemModelProvider extends ItemModelProvider {

    public StorageItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Pyrotech.MOD_ID, existingFileHelper);
    }

    @Override
    public String getName() {
        return "Storage Item Models: " + Pyrotech.MOD_ID;
    }

    @Override
    protected void registerModels() {
        this.blockItem("stash", "stash");
        this.blockItem("stash_stone", "stash_stone");
        this.blockItem("shelf", "shelf");
        this.blockItem("shelf_stone", "shelf_stone");
        this.blockItem("crate", "crate");
        this.blockItem("crate_stone", "crate_stone");
        this.blockItem("wood_rack", "wood_rack");
        this.blockItem("stone_tank", "gen/stone_tank/tank");
        this.blockItem("brick_tank", "gen/brick_tank/tank");
        this.blockItem("faucet_stone", "gen/faucet_stone/faucet");
        this.blockItem("faucet_brick", "gen/faucet_brick/faucet");
        this.bag("bag_simple");
        this.bag("bag_durable");
    }

    private void blockItem(String name, String blockModel) {
        this.withExistingParent(name, this.modLoc("block/" + blockModel));
    }

    private void bag(String name) {
        this.withExistingParent(name, this.modLoc("block/" + name))
            .override().predicate(BagBlock.OPEN_PREDICATE, 1).model(this.getExistingFile(this.modLoc("block/" + name + "_open"))).end();
    }
}
