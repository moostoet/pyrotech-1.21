package com.moostoet.pyrotech.tech.basic.item;

import com.moostoet.pyrotech.tech.basic.TechBasicComponents;
import com.moostoet.pyrotech.tech.basic.TechBasicConfig;
import com.moostoet.pyrotech.tech.basic.TechBasicItems;
import com.moostoet.pyrotech.tech.basic.block.entity.CampfireBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * A stick with a marshmallow on it. Held toward a lit campfire it roasts, and held too
 * long it burns; otherwise it is eaten off the stick, and sneaking takes it off into the
 * other hand. Three components carry the state (tech/basic sign-off): the type, the
 * game time it is done roasting, and the game time it was roasted.
 */
public final class MarshmallowStickItem extends Item {

    public static final int ROASTING_RANGE_BLOCKS = 2;
    public static final int ROASTING_DURATION_TICKS = 100;
    public static final double ROASTING_DURATION_VARIANCE = 0.2;
    public static final int ROASTING_BURN_DURATION_TICKS = 20;
    private static final int ROASTING_USE_DURATION = 12000;
    private static final int EATING_USE_DURATION = 32;

    private static final FoodProperties FOOD = new FoodProperties.Builder().nutrition(2).saturationModifier(0.1f).build();

    public MarshmallowStickItem(Properties properties) {
        super(properties.food(FOOD));
    }

    // -- Components ----------------------------------------------------------

    public static MarshmallowType type(ItemStack stack) {
        return stack.getOrDefault(TechBasicComponents.MARSHMALLOW_TYPE.get(), MarshmallowType.PLAIN);
    }

    private static void setType(ItemStack stack, MarshmallowType type) {
        stack.set(TechBasicComponents.MARSHMALLOW_TYPE.get(), type);
    }

    private static long roastBy(ItemStack stack) {
        Long roastBy = stack.get(TechBasicComponents.ROAST_BY.get());
        return roastBy == null ? Long.MAX_VALUE : roastBy;
    }

    private static boolean isRoasting(ItemStack stack) {
        return stack.has(TechBasicComponents.ROAST_BY.get());
    }

    /** A full stick from an empty one, keeping the stick's wear. */
    public static ItemStack withMarshmallow(ItemStack emptyStick, MarshmallowType type) {
        ItemStack full = new ItemStack(TechBasicItems.MARSHMALLOW_STICK.get());
        full.setDamageValue(emptyStick.getDamageValue());
        setType(full, type);
        return full;
    }

    private static ItemStack emptyStick(ItemStack fullStick) {
        ItemStack empty = new ItemStack(TechBasicItems.MARSHMALLOW_STICK_EMPTY.get());
        empty.setDamageValue(fullStick.getDamageValue());
        return empty;
    }

    // -- Roasting ------------------------------------------------------------

    private static boolean isLookingAtCampfire(Level level, Player player) {
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        BlockPos pos = hit.getBlockPos();
        return level.getBlockEntity(pos) instanceof CampfireBlockEntity campfire && campfire.isLit()
            && player.distanceToSqr(pos.getCenter()) <= ROASTING_RANGE_BLOCKS * ROASTING_RANGE_BLOCKS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stick = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) {
            return super.use(level, player, hand);
        }
        if (Marshmallows.roastedAt(stick) == 0 && isLookingAtCampfire(level, player)) {
            if (!isRoasting(stick)) {
                int duration = ROASTING_DURATION_TICKS;
                if (!level.isClientSide) {
                    double variance = (level.random.nextDouble() * 2 - 1) * ROASTING_DURATION_VARIANCE;
                    duration = Math.max(0, (int) (duration + duration * variance));
                }
                stick.set(TechBasicComponents.ROAST_BY.get(), level.getGameTime() + duration);
            }
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stick);
        }
        if (!player.isShiftKeyDown()) {
            return super.use(level, player, hand);
        }
        ItemStack offhand = player.getOffhandItem();
        MarshmallowType type = type(stick);
        if (offhand.is(TechBasicItems.MARSHMALLOW.get())) {
            if (type == MarshmallowType.PLAIN && offhand.getCount() < offhand.getMaxStackSize()) {
                offhand.grow(1);
                Marshmallows.cooldown(player);
                return InteractionResultHolder.sidedSuccess(emptyStick(stick), level.isClientSide);
            }
            return InteractionResultHolder.pass(stick);
        }
        if (!offhand.isEmpty()) {
            return InteractionResultHolder.pass(stick);
        }
        ItemStack removed = switch (type) {
            case PLAIN -> new ItemStack(TechBasicItems.MARSHMALLOW.get());
            case ROASTED -> {
                ItemStack roasted = new ItemStack(TechBasicItems.MARSHMALLOW_ROASTED.get());
                roasted.set(TechBasicComponents.ROASTED_AT.get(), Marshmallows.roastedAt(stick));
                yield roasted;
            }
            case BURNED -> new ItemStack(TechBasicItems.MARSHMALLOW_BURNED.get());
        };
        player.setItemSlot(EquipmentSlot.OFFHAND, removed);
        ItemStack empty = emptyStick(stick);
        if (type != MarshmallowType.PLAIN) {
            empty.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        }
        Marshmallows.cooldown(player);
        return InteractionResultHolder.sidedSuccess(empty, level.isClientSide);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return isRoasting(stack) ? ROASTING_USE_DURATION : EATING_USE_DURATION;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return isRoasting(stack) ? UseAnim.DRINK : UseAnim.EAT;
    }

    /** Roasting stops when the campfire leaves the crosshair; the client flips the marshmallow as it cooks. */
    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!(entity instanceof Player player) || !isRoasting(stack)) {
            return;
        }
        if (!isLookingAtCampfire(level, player)) {
            player.releaseUsingItem();
            Marshmallows.cooldown(player);
            return;
        }
        if (level.isClientSide) {
            long now = level.getGameTime();
            long roastBy = roastBy(stack);
            MarshmallowType type = type(stack);
            if (type == MarshmallowType.ROASTED && now >= roastBy + ROASTING_BURN_DURATION_TICKS) {
                setType(stack, MarshmallowType.BURNED);
            } else if (type == MarshmallowType.PLAIN && now >= roastBy) {
                setType(stack, MarshmallowType.ROASTED);
            }
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeCharged) {
        if (!(entity instanceof Player) || Marshmallows.roastedAt(stack) > 0 || !isRoasting(stack)) {
            return;
        }
        long now = level.getGameTime();
        long roastBy = roastBy(stack);
        if (now >= roastBy + ROASTING_BURN_DURATION_TICKS) {
            setType(stack, MarshmallowType.BURNED);
            if (!level.isClientSide) {
                stack.set(TechBasicComponents.ROASTED_AT.get(), now);
                broadcastBurned(level, entity);
            }
        } else if (now >= roastBy) {
            setType(stack, MarshmallowType.ROASTED);
            if (!level.isClientSide) {
                stack.set(TechBasicComponents.ROASTED_AT.get(), now);
            }
        }
        stack.remove(TechBasicComponents.ROAST_BY.get());
    }

    private static void broadcastBurned(Level level, LivingEntity entity) {
        MinecraftServer server = level.getServer();
        if (server != null && TechBasicConfig.COMMON.burnedMarshmallowBroadcast.get()) {
            server.getPlayerList().broadcastSystemMessage(
                Component.translatable("gui.pyrotech.marshmallow.burned.broadcast.message", entity.getDisplayName()), false);
        }
    }

    /** Eating off the stick gives the marshmallow's effect and leaves the stick, worn unless the marshmallow was plain. */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return stack;
        }
        if (isRoasting(stack)) {
            stack.remove(TechBasicComponents.ROAST_BY.get());
            setType(stack, MarshmallowType.BURNED);
            if (!level.isClientSide) {
                stack.set(TechBasicComponents.ROASTED_AT.get(), level.getGameTime());
            }
            return stack;
        }
        MarshmallowType type = type(stack);
        MarshmallowItem.applyEffects(type, stack, level, entity);
        player.eat(level, stack.copy(), FOOD);
        ItemStack empty = emptyStick(stack);
        if (type != MarshmallowType.PLAIN) {
            empty.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        }
        Marshmallows.cooldown(player);
        return empty;
    }

    @Override
    public Component getName(ItemStack stack) {
        return switch (type(stack)) {
            case PLAIN -> Component.translatable("item.pyrotech.marshmallow_on_stick");
            case ROASTED -> Component.translatable("item.pyrotech.marshmallow_on_stick_roasted");
            case BURNED -> Component.translatable("item.pyrotech.marshmallow_on_stick_burned");
        };
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || type(oldStack) != type(newStack);
    }
}
