package com.moostoet.pyrotech.core.item;

import com.moostoet.pyrotech.core.CoreBlocks;
import com.moostoet.pyrotech.core.entity.ThrownRockEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.function.Supplier;

/**
 * A rock in hand: placed on a block like any block item, thrown at the air. The throw
 * numbers are the 1.12 config defaults, now constants (core sign-off, item 3). The
 * cooldown covers every rock, as 1.12's one rock item did for its twelve variants.
 */
public final class RockItem extends BlockItem {

    private static final int THROW_COOLDOWN_TICKS = 10;
    private static final float PITCH_OFFSET = -15.0F;
    private static final float VELOCITY = 0.75F;
    private static final float INACCURACY = 5.0F;

    private final Supplier<EntityType<ThrownRockEntity>> entityType;

    public RockItem(Block block, Supplier<EntityType<ThrownRockEntity>> entityType, Properties properties) {
        super(block, properties);
        this.entityType = entityType;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL,
            0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        for (DeferredItem<RockItem> rock : CoreBlocks.ROCK_ITEMS) {
            player.getCooldowns().addCooldown(rock.get(), THROW_COOLDOWN_TICKS);
        }
        if (!level.isClientSide) {
            ThrownRockEntity rock = new ThrownRockEntity(this.entityType.get(), player, level, this::asItem);
            rock.setItem(stack);
            rock.shootFromRotation(player, player.getXRot(), player.getYRot(), PITCH_OFFSET, VELOCITY, INACCURACY);
            level.addFreshEntity(rock);
        }
        stack.consume(1, player);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
