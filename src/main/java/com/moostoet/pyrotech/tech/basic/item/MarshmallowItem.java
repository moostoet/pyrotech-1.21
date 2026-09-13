package com.moostoet.pyrotech.tech.basic.item;

import com.moostoet.pyrotech.tech.basic.TechBasicConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A marshmallow off the stick: plain, roasted, or burned. Plain gives a moment of speed
 * that stacks to five seconds; roasted gives more, scaled by how fresh it is; burned
 * slows, and tells everyone.
 */
public final class MarshmallowItem extends Item {

    private final MarshmallowType type;

    public MarshmallowItem(MarshmallowType type, Properties properties) {
        super(properties.food(food(type)));
        this.type = type;
    }

    public MarshmallowType type() {
        return this.type;
    }

    private static FoodProperties food(MarshmallowType type) {
        return switch (type) {
            case PLAIN -> new FoodProperties.Builder().nutrition(1).saturationModifier(0.05f).alwaysEdible().build();
            case ROASTED -> new FoodProperties.Builder().nutrition(2).saturationModifier(0.1f).alwaysEdible().build();
            case BURNED -> new FoodProperties.Builder().nutrition(1).saturationModifier(0.01f).alwaysEdible().build();
        };
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        applyEffects(this.type, stack, level, entity);
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (entity instanceof Player player) {
            Marshmallows.cooldown(player);
        }
        return result;
    }

    /** The effect a marshmallow of a kind gives when eaten, from the item or the stick. */
    public static void applyEffects(MarshmallowType type, ItemStack stack, Level level, LivingEntity entity) {
        switch (type) {
            case PLAIN -> Marshmallows.applyEffect(entity, MobEffects.MOVEMENT_SPEED, Marshmallows.PLAIN_SPEED_TICKS,
                Marshmallows.MAX_PLAIN_SPEED_TICKS, true);
            case ROASTED -> {
                double potency = Marshmallows.potency(level, Marshmallows.roastedAt(stack));
                Marshmallows.applyEffect(entity, MobEffects.MOVEMENT_SPEED, (int) (Marshmallows.ROASTED_SPEED_TICKS * potency),
                    Marshmallows.MAX_ROASTED_SPEED_TICKS, true);
            }
            case BURNED -> {
                MinecraftServer server = level.getServer();
                if (server != null && TechBasicConfig.COMMON.burnedMarshmallowEatBroadcast.get()) {
                    server.getPlayerList().broadcastSystemMessage(
                        Component.translatable("gui.pyrotech.marshmallow.burned.eat.broadcast.message", entity.getDisplayName()), false);
                }
                Marshmallows.applyEffect(entity, MobEffects.MOVEMENT_SLOWDOWN, Marshmallows.BURNED_SLOW_TICKS,
                    Marshmallows.BURNED_SLOW_TICKS, false);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (this.type == MarshmallowType.ROASTED && context.level() != null) {
            int percent = (int) Math.round((Marshmallows.potency(context.level(), Marshmallows.roastedAt(stack)) - 1) * 100);
            tooltip.add(Component.translatable("gui.pyrotech.tooltip.potency", percent).withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
