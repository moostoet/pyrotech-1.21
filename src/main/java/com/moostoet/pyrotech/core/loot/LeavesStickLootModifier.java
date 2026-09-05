package com.moostoet.pyrotech.core.loot;

import com.moostoet.pyrotech.core.CoreConfig;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * The sticks-from-leaves tweak, the 1.12 {@code HarvestDropsEventHandler.Sticks}: broken
 * leaves drop a stick half the time for a player and a tenth of the time otherwise, while
 * {@code DROP_STICKS_FROM_LEAVES} is on. Vanilla's own rarer stick drop stays.
 */
public final class LeavesStickLootModifier extends LootModifier {

    public static final MapCodec<LeavesStickLootModifier> CODEC =
        RecordCodecBuilder.mapCodec(instance -> codecStart(instance).apply(instance, LeavesStickLootModifier::new));

    private static final float PLAYER_CHANCE = 0.5f;
    private static final float OTHER_CHANCE = 0.1f;

    public LeavesStickLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (!CoreConfig.COMMON.dropSticksFromLeaves.get()) {
            return generatedLoot;
        }
        BlockState state = context.getParamOrNull(LootContextParams.BLOCK_STATE);
        if (state == null || !state.is(BlockTags.LEAVES)) {
            return generatedLoot;
        }
        float chance = context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof Player ? PLAYER_CHANCE : OTHER_CHANCE;
        if (context.getRandom().nextFloat() < chance) {
            generatedLoot.add(new ItemStack(Items.STICK));
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
