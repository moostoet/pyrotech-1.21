package com.moostoet.pyrotech.tech.basic.item;

import com.moostoet.pyrotech.tech.basic.TechBasicItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A bare stick: sneak and use it with marshmallows in the other hand to mount one. */
public final class MarshmallowStickEmptyItem extends Item {

    public MarshmallowStickEmptyItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stick = player.getItemInHand(hand);
        ItemStack offhand = player.getOffhandItem();
        if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown() || !offhand.is(TechBasicItems.MARSHMALLOW.get())) {
            return InteractionResultHolder.pass(stick);
        }
        if (!player.isCreative()) {
            offhand.shrink(1);
        }
        ItemStack full = MarshmallowStickItem.withMarshmallow(stick, MarshmallowType.PLAIN);
        Marshmallows.cooldown(player);
        return InteractionResultHolder.sidedSuccess(full, level.isClientSide);
    }
}
