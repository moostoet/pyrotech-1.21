package com.moostoet.pyrotech.hunting.item;

import com.moostoet.pyrotech.hunting.entity.SoakingHideItemEntity;
import com.moostoet.pyrotech.library.client.ShiftKey;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/** A scraped hide, plain or small: dropped, it becomes the soaking entity that washes it. */
public final class ScrapedHideItem extends Item {

    private static final int PICKUP_DELAY = 40;

    private final Supplier<? extends Item> washed;

    public ScrapedHideItem(Supplier<? extends Item> washed, Properties properties) {
        super(properties);
        this.washed = washed;
    }

    /** The washed hide a stack of scraped hides becomes, or null for any other stack. */
    @Nullable
    public static Item washedFor(ItemStack stack) {
        return stack.getItem() instanceof ScrapedHideItem hide ? hide.washed.get() : null;
    }

    @Override
    public boolean hasCustomEntity(ItemStack stack) {
        return true;
    }

    @Override
    public Entity createEntity(Level level, Entity location, ItemStack stack) {
        SoakingHideItemEntity entity = new SoakingHideItemEntity(level, location.getX(), location.getY(), location.getZ(), stack);
        entity.setDeltaMovement(location.getDeltaMovement());
        entity.setPickUpDelay(PICKUP_DELAY);
        return entity;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (FMLEnvironment.dist == Dist.CLIENT && ShiftKey.isDown()) {
            tooltip.add(Component.translatable("gui.pyrotech.hide.scraped.washing", minutesAndSeconds(SoakingHideItemEntity.SOAK_TICKS))
                .withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.translatable("gui.pyrotech.tooltip.extended.shift", ChatFormatting.GOLD, ChatFormatting.GRAY));
        }
    }

    /** Athenaeum's {@code ticksToHMS}: minutes and seconds, hours in front only when there are any. */
    private static String minutesAndSeconds(int ticks) {
        int totalSeconds = ticks / 20;
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        return hours > 0 ? String.format("%02d:%02d:%02d", hours, minutes, seconds) : String.format("%02d:%02d", minutes, seconds);
    }
}
