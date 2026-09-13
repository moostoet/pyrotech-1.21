package com.moostoet.pyrotech.hunting.event;

import com.moostoet.pyrotech.hunting.HuntingConfig;
import com.moostoet.pyrotech.hunting.HuntingDataMaps;
import com.moostoet.pyrotech.hunting.HuntingItems;
import com.moostoet.pyrotech.hunting.HuntingTags;
import com.moostoet.pyrotech.hunting.block.CarcassBlock;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The 1.12 {@code EntityLivingDropsEventHandler}: a kill's leather goes, its captured drops
 * and its carcass drops go into one carcass item, and a sheep or llama adds its pelt. Runs
 * last, after core's wool tweak, as 1.12 ordered it.
 */
public final class CarcassDropsHandler {

    private static final float PELT_CHANCE = 0.85f;
    private static final int PELT_COUNT = 1;

    private CarcassDropsHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player || entity.level().isClientSide) {
            return;
        }
        Collection<ItemEntity> drops = event.getDrops();
        List<ItemStack> captured = new ArrayList<>();
        boolean removeLeather = HuntingConfig.COMMON.removeLeatherDrops.get();
        drops.removeIf(drop -> {
            ItemStack stack = drop.getItem();
            if (removeLeather && stack.is(Items.LEATHER)) {
                return true;
            }
            if (stack.is(HuntingTags.Items.CARCASS_CAPTURED)) {
                captured.add(stack);
                return true;
            }
            return false;
        });
        RandomSource random = entity.getRandom();
        HuntingDataMaps.CarcassDrops table = entity.getType().builtInRegistryHolder().getData(HuntingDataMaps.CARCASS_DROPS);
        if (table != null) {
            for (HuntingDataMaps.ItemRoll roll : table.drops()) {
                ItemStack rolled = roll.roll(random);
                if (!rolled.isEmpty()) {
                    captured.add(rolled);
                }
            }
        }
        if (entity instanceof Sheep sheep && random.nextFloat() <= PELT_CHANCE) {
            captured.add(new ItemStack(sheep.isSheared() ? HuntingItems.HIDE_SHEEP_SHEARED.get() : HuntingItems.SHEEP_PELTS.get(sheep.getColor()).get(),
                PELT_COUNT));
        }
        if (entity instanceof Llama llama && random.nextFloat() <= PELT_CHANCE) {
            captured.add(new ItemStack(HuntingItems.LLAMA_PELTS.get(llama.getVariant()).get(), PELT_COUNT));
        }
        if (!captured.isEmpty()) {
            drops.add(new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(), CarcassBlock.withContents(captured)));
        }
    }
}
