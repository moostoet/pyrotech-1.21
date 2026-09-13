package com.moostoet.pyrotech.datagen.hunting;

import com.moostoet.pyrotech.core.CoreItems;
import com.moostoet.pyrotech.core.Material;
import com.moostoet.pyrotech.hunting.HuntingDataMaps;
import com.moostoet.pyrotech.hunting.HuntingDataMaps.ButcheringTransforms;
import com.moostoet.pyrotech.hunting.HuntingDataMaps.CarcassDrops;
import com.moostoet.pyrotech.hunting.HuntingDataMaps.ItemRoll;
import com.moostoet.pyrotech.hunting.HuntingDataMaps.KnifeEfficiency;
import com.moostoet.pyrotech.hunting.HuntingItems;
import com.moostoet.pyrotech.hunting.item.KnifeItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/** The three hunting data maps with the 1.12 config defaults (hunting sign-off, item 2). */
public final class HuntingDataMapProvider extends DataMapProvider {

    public HuntingDataMapProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider);
    }

    @Override
    public String getName() {
        return "Hunting Data Maps";
    }

    @Override
    protected void gather(HolderLookup.Provider provider) {
        Item boneShard = CoreItems.material(Material.BONE_SHARD).get();
        Item lard = CoreItems.material(Material.LARD).get();
        Item taintedMeat = CoreItems.TAINTED_MEAT.get();

        Builder<CarcassDrops, EntityType<?>> drops = this.builder(HuntingDataMaps.CARCASS_DROPS);
        drops.add(EntityType.PIG.builtInRegistryHolder(), drops(roll(HuntingItems.HIDE_PIG, 1, 0.85f), roll(boneShard, 2, 0.5f), roll(lard, 2, 0.5f)), false);
        drops.add(EntityType.COW.builtInRegistryHolder(), drops(roll(HuntingItems.PELT_COW, 1, 0.85f), roll(boneShard, 2, 0.5f), roll(lard, 2, 0.5f)), false);
        drops.add(EntityType.MOOSHROOM.builtInRegistryHolder(), drops(roll(HuntingItems.PELT_MOOSHROOM, 1, 0.65f), roll(boneShard, 2, 0.5f), roll(lard, 2, 0.5f)), false);
        drops.add(EntityType.POLAR_BEAR.builtInRegistryHolder(), drops(roll(HuntingItems.PELT_POLAR_BEAR, 1, 0.85f), roll(boneShard, 2, 0.5f), roll(lard, 3, 0.5f)), false);
        drops.add(EntityType.BAT.builtInRegistryHolder(), drops(roll(HuntingItems.PELT_BAT, 1, 0.65f)), false);
        drops.add(EntityType.HORSE.builtInRegistryHolder(), drops(roll(HuntingItems.PELT_HORSE, 1, 0.85f), roll(boneShard, 2, 0.5f), roll(lard, 2, 0.5f)), false);
        drops.add(EntityType.DONKEY.builtInRegistryHolder(), drops(roll(HuntingItems.PELT_HORSE, 1, 0.85f), roll(boneShard, 2, 0.5f), roll(lard, 1, 0.5f)), false);
        drops.add(EntityType.RABBIT.builtInRegistryHolder(), drops(roll(Items.RABBIT_HIDE, 1, 0.65f), roll(boneShard, 1, 0.25f), roll(lard, 1, 0.5f)), false);
        drops.add(EntityType.WOLF.builtInRegistryHolder(), drops(roll(HuntingItems.PELT_WOLF, 1, 0.85f), roll(boneShard, 2, 0.5f), roll(lard, 1, 0.5f)), false);

        // Bone and flint 15 and 25, stone 10 and 17, iron 20 and 34, gold 5 and 8, diamond 35
        // and 58, obsidian 20 and 34, the same for both knife kinds.
        int[] atCarcass = {15, 15, 10, 20, 5, 35, 20};
        int[] atButchersBlock = {25, 25, 17, 34, 8, 58, 34};
        Builder<KnifeEfficiency, Item> efficiency = this.builder(HuntingDataMaps.KNIFE_EFFICIENCY);
        for (List<DeferredItem<KnifeItem>> knives : List.of(HuntingItems.HUNTERS_KNIVES, HuntingItems.BUTCHERS_KNIVES)) {
            for (int i = 0; i < knives.size(); i++) {
                efficiency.add(knives.get(i), new KnifeEfficiency(atCarcass[i], atButchersBlock[i]), false);
            }
        }

        // A butcher's knife ruins pelts, turns bone shards into bones, and doubles meat and
        // lard; a hunter's knife doubles pelts and bone shards and taints meat.
        Builder<ButcheringTransforms, Item> transforms = this.builder(HuntingDataMaps.BUTCHERING_TRANSFORMS);
        for (ItemLike hide : List.of(HuntingItems.HIDE_PIG, HuntingItems.PELT_COW, HuntingItems.PELT_MOOSHROOM, HuntingItems.PELT_POLAR_BEAR,
            HuntingItems.PELT_BAT, HuntingItems.PELT_HORSE, Items.RABBIT_HIDE, HuntingItems.PELT_WOLF)) {
            transforms.add(hide.asItem().builtInRegistryHolder(),
                both(roll(hide, 2, 0.85f), roll(HuntingItems.PELT_RUINED, 1, 0.85f)), false);
        }
        transforms.add(boneShard.builtInRegistryHolder(), both(roll(boneShard, 2, 0.5f), roll(Items.BONE, 1, 0.5f)), false);
        for (Item meat : List.of(Items.BEEF, Items.CHICKEN, Items.MUTTON, Items.RABBIT, Items.PORKCHOP)) {
            transforms.add(meat.builtInRegistryHolder(), both(roll(taintedMeat, 1, 0.85f), roll(meat, 2, 0.85f)), false);
        }
        transforms.add(Items.RABBIT_FOOT.builtInRegistryHolder(), both(roll(Items.RABBIT_FOOT, 2, 0.85f), roll(Items.RABBIT_FOOT, 2, 0.85f)), false);
        transforms.add(Items.RED_MUSHROOM.builtInRegistryHolder(), both(roll(Items.RED_MUSHROOM, 4, 0.85f), roll(Items.RED_MUSHROOM, 4, 0.85f)), false);
        transforms.add(lard.builtInRegistryHolder(), new ButcheringTransforms(Optional.empty(), Optional.of(roll(lard, 2, 0.85f))), false);
    }

    private static ItemRoll roll(ItemLike item, int count, float chance) {
        return new ItemRoll(item.asItem(), count, chance);
    }

    private static CarcassDrops drops(ItemRoll... rolls) {
        return new CarcassDrops(List.of(rolls));
    }

    private static ButcheringTransforms both(ItemRoll hunters, ItemRoll butchers) {
        return new ButcheringTransforms(Optional.of(hunters), Optional.of(butchers));
    }
}
