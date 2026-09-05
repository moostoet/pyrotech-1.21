package com.moostoet.pyrotech.core.loot;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.core.CoreConfig;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * The iron ingot tweak: every iron ingot in generated loot becomes the configured item,
 * raw iron by default, while {@code REPLACE_IRON_INGOTS} is on. 1.12 rewrote the loot
 * table entries; this rewrites the drops, which is the same thing to a player.
 */
public final class IronIngotLootModifier extends LootModifier {

    public static final MapCodec<IronIngotLootModifier> CODEC =
        RecordCodecBuilder.mapCodec(instance -> codecStart(instance).apply(instance, IronIngotLootModifier::new));

    public IronIngotLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (!CoreConfig.COMMON.replaceIronIngots.get()) {
            return generatedLoot;
        }
        String replacementId = CoreConfig.COMMON.replaceIronIngotsWith.get();
        Item replacement = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(replacementId)).orElse(null);
        if (replacement == null) {
            Pyrotech.LOGGER.error("Unable to locate item for iron ingot replacement: {}", replacementId);
            return generatedLoot;
        }
        for (int i = 0; i < generatedLoot.size(); i++) {
            ItemStack stack = generatedLoot.get(i);
            if (stack.is(Items.IRON_INGOT)) {
                generatedLoot.set(i, new ItemStack(replacement, stack.getCount()));
            }
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
