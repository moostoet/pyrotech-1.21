package com.moostoet.pyrotech.core.item;

import com.moostoet.pyrotech.core.CoreBlocks;
import com.moostoet.pyrotech.core.CoreEntities;
import com.moostoet.pyrotech.core.entity.PyroberryCocktailEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A bottle of pyroberry wine with a fuse. Thrown like a rock; the throw numbers are the
 * 1.12 config defaults, now constants (core sign-off, item 3). 1.12 also put the grass
 * clump on the same cooldown, and that stays.
 */
public final class PyroberryCocktailItem extends Item {

    private static final int THROW_COOLDOWN_TICKS = 20;
    private static final float PITCH_OFFSET = -15.0F;
    private static final float VELOCITY = 0.75F;
    private static final float INACCURACY = 5.0F;

    public PyroberryCocktailItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL,
            0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        player.getCooldowns().addCooldown(this, THROW_COOLDOWN_TICKS);
        player.getCooldowns().addCooldown(CoreBlocks.ROCK_GRASS.get().asItem(), THROW_COOLDOWN_TICKS);
        if (!level.isClientSide) {
            PyroberryCocktailEntity cocktail = new PyroberryCocktailEntity(CoreEntities.PYROBERRY_COCKTAIL.get(), player, level);
            cocktail.shootFromRotation(player, player.getXRot(), player.getYRot(), PITCH_OFFSET, VELOCITY, INACCURACY);
            level.addFreshEntity(cocktail);
        }
        stack.consume(1, player);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
