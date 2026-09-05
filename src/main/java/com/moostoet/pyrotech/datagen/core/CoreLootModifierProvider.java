package com.moostoet.pyrotech.datagen.core;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.core.loot.IronIngotLootModifier;
import com.moostoet.pyrotech.core.loot.LeavesStickLootModifier;
import com.moostoet.pyrotech.core.loot.VillageFurnaceLootModifier;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
import net.neoforged.neoforge.common.loot.LootTableIdCondition;

import java.util.concurrent.CompletableFuture;

/** Core's three global loot modifiers. Each reads its config toggle when it runs. */
public final class CoreLootModifierProvider extends GlobalLootModifierProvider {

    private static final ResourceLocation SNOWY_HOUSE_CHEST = ResourceLocation.withDefaultNamespace("chests/village/village_snowy_house");

    public CoreLootModifierProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, Pyrotech.MOD_ID);
    }

    @Override
    protected void start() {
        this.add("replace_iron_ingots", new IronIngotLootModifier(new LootItemCondition[0]));
        this.add("remove_village_furnace", new VillageFurnaceLootModifier(new LootItemCondition[]{
            LootTableIdCondition.builder(SNOWY_HOUSE_CHEST).build()}));
        this.add("sticks_from_leaves", new LeavesStickLootModifier(new LootItemCondition[0]));
    }
}
