package com.moostoet.pyrotech.core.loot;

import com.moostoet.pyrotech.core.CoreConfig;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * The furnace half of the village tweaks that the chunk scan cannot reach: the snowy
 * village house chest holds a furnace, and this strips it while
 * {@code REPLACE_VANILLA_FURNACE} is on (progression skips sign-off, item 8). The datagen
 * conditions pin it to that one table.
 */
public final class VillageFurnaceLootModifier extends LootModifier {

    public static final MapCodec<VillageFurnaceLootModifier> CODEC =
        RecordCodecBuilder.mapCodec(instance -> codecStart(instance).apply(instance, VillageFurnaceLootModifier::new));

    public VillageFurnaceLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (CoreConfig.COMMON.replaceVanillaFurnace.get()) {
            generatedLoot.removeIf(stack -> stack.is(Items.FURNACE));
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
