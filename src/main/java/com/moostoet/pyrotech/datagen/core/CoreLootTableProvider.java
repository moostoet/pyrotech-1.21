package com.moostoet.pyrotech.datagen.core;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** The loot tables under {@code data/pyrotech/loot_table}. Later units add their sub providers here. */
public final class CoreLootTableProvider extends LootTableProvider {

    public CoreLootTableProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, Set.of(), List.of(new SubProviderEntry(CoreBlockLootProvider::new, LootContextParamSets.BLOCK)), registries);
    }
}
