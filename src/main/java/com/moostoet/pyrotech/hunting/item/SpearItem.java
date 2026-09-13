package com.moostoet.pyrotech.hunting.item;

import com.moostoet.pyrotech.hunting.entity.SpearEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * A spear: drawn like a bow and thrown on release at the bow's power curve times its own
 * velocity scalar, with its own inaccuracy and thrown damage. Each throw costs one
 * durability; a throw that breaks the spear throws nothing.
 */
public final class SpearItem extends Item {

    private static final int USE_DURATION = 72000;
    private static final float MINIMUM_POWER = 0.1f;

    private final double velocityScalar;
    private final double inaccuracy;
    private final double thrownDamage;

    public SpearItem(double velocityScalar, double inaccuracy, double thrownDamage, Properties properties) {
        super(properties);
        this.velocityScalar = velocityScalar;
        this.inaccuracy = inaccuracy;
        this.thrownDamage = thrownDamage;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) {
            return;
        }
        int charge = this.getUseDuration(stack, entity) - timeLeft;
        if (charge < 0) {
            return;
        }
        float power = BowItem.getPowerForTime(charge);
        if (power < MINIMUM_POWER) {
            return;
        }
        if (!level.isClientSide) {
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
            if (!stack.isEmpty()) {
                SpearEntity spear = new SpearEntity(level, player, stack);
                spear.setBaseDamage(this.thrownDamage);
                spear.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, (float) (power * this.velocityScalar), (float) this.inaccuracy);
                if (power == 1) {
                    spear.setCritArrow(true);
                }
                if (player.hasInfiniteMaterials()) {
                    spear.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                }
                level.addFreshEntity(spear);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F,
                1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + power * 0.5F);
            if (!player.hasInfiniteMaterials()) {
                player.getInventory().removeItem(stack);
            }
        }
        player.awardStat(Stats.ITEM_USED.get(this));
    }
}
