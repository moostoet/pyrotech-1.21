package com.moostoet.pyrotech.hunting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.moostoet.pyrotech.Pyrotech;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.datamaps.DataMapType;

import java.util.List;
import java.util.Optional;

/**
 * The three 1.12 string-map configs as data maps (hunting sign-off, item 2): what a killed
 * animal leaves in its carcass, how much progress each knife adds, and what a knife turns an
 * item into on its way out of a carcass on the butcher's block.
 */
public final class HuntingDataMaps {

    /** An item, a count, and the chance of getting it: the 1.12 {@code (item);(count);(chance)} string. */
    public record ItemRoll(Item item, int count, float chance) {

        public static final Codec<ItemRoll> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(ItemRoll::item),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("count", 1).forGetter(ItemRoll::count),
            Codec.floatRange(0, 1).optionalFieldOf("chance", 1f).forGetter(ItemRoll::chance)
        ).apply(instance, ItemRoll::new));

        public ItemStack roll(RandomSource random) {
            return random.nextFloat() <= this.chance ? new ItemStack(this.item, this.count) : ItemStack.EMPTY;
        }
    }

    /** What an entity type adds to its carcass beyond the captured drops. */
    public record CarcassDrops(List<ItemRoll> drops) {

        public static final Codec<CarcassDrops> CODEC = ItemRoll.CODEC.listOf().xmap(CarcassDrops::new, CarcassDrops::drops);
    }

    /** A knife's progress per use at a carcass and at the butcher's block: the two 1.12 maps as one entry. */
    public record KnifeEfficiency(int carcass, int butchersBlock) {

        public static final Codec<KnifeEfficiency> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ExtraCodecs.POSITIVE_INT.fieldOf("carcass").forGetter(KnifeEfficiency::carcass),
            ExtraCodecs.POSITIVE_INT.fieldOf("butchers_block").forGetter(KnifeEfficiency::butchersBlock)
        ).apply(instance, KnifeEfficiency::new));
    }

    /** What an item leaving a carcass on the butcher's block becomes under a hunter's or a butcher's knife. */
    public record ButcheringTransforms(Optional<ItemRoll> hunters, Optional<ItemRoll> butchers) {

        public static final Codec<ButcheringTransforms> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemRoll.CODEC.optionalFieldOf("hunters").forGetter(ButcheringTransforms::hunters),
            ItemRoll.CODEC.optionalFieldOf("butchers").forGetter(ButcheringTransforms::butchers)
        ).apply(instance, ButcheringTransforms::new));
    }

    public static final DataMapType<EntityType<?>, CarcassDrops> CARCASS_DROPS =
        DataMapType.builder(id("carcass_drops"), Registries.ENTITY_TYPE, CarcassDrops.CODEC).build();
    public static final DataMapType<Item, KnifeEfficiency> KNIFE_EFFICIENCY =
        DataMapType.builder(id("knife_efficiency"), Registries.ITEM, KnifeEfficiency.CODEC).build();
    public static final DataMapType<Item, ButcheringTransforms> BUTCHERING_TRANSFORMS =
        DataMapType.builder(id("butchering_transforms"), Registries.ITEM, ButcheringTransforms.CODEC).build();

    private HuntingDataMaps() {
    }

    /** A knife without an entry adds one, as the 1.12 maps defaulted. */
    public static int efficiency(ItemStack knife, boolean atButchersBlock) {
        KnifeEfficiency efficiency = knife.getItemHolder().getData(KNIFE_EFFICIENCY);
        if (efficiency == null) {
            return 1;
        }
        return atButchersBlock ? efficiency.butchersBlock() : efficiency.carcass();
    }

    /** The butchering transform for the knife in hand, or the item unchanged when there is none or the roll fails. */
    public static ItemStack transform(ItemStack extracted, ItemStack knife, RandomSource random) {
        ButcheringTransforms transforms = extracted.getItemHolder().getData(BUTCHERING_TRANSFORMS);
        if (transforms == null) {
            return extracted;
        }
        Optional<ItemRoll> roll = knife.is(HuntingTags.Items.BUTCHERS_KNIVES) ? transforms.butchers() : transforms.hunters();
        if (roll.isEmpty()) {
            return extracted;
        }
        ItemStack rolled = roll.get().roll(random);
        return rolled.isEmpty() ? extracted : rolled;
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(Pyrotech.MOD_ID, name);
    }
}
