package com.moostoet.pyrotech.storage.item;

import com.moostoet.pyrotech.library.client.ShiftKey;
import com.moostoet.pyrotech.storage.StorageComponents;
import com.moostoet.pyrotech.storage.block.BagBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.List;

/**
 * A rock bag in hand: it collects rocks as they are picked up while open, pours into a
 * block's inventory on a sneak-click, spills out in front on a sneak-click at air while
 * open, and opens or closes on a sneak-click at nothing. Its contents and open state are
 * components, so they survive placing and picking up.
 */
public final class BagItem extends BlockItem {

    private static final int COLOR_BAG = 0x70341E;
    private static final int COLOR_BAG_FULL = 0xFF0000;

    private final BagBlock bag;

    public BagItem(BagBlock bag, Properties properties) {
        super(bag, properties);
        this.bag = bag;
    }

    public BagBlock bag() {
        return this.bag;
    }

    public static boolean isOpen(ItemStack stack) {
        return BagBlock.isOpen(stack);
    }

    public boolean isItemValid(ItemStack stack) {
        return stack.is(this.bag.allowed());
    }

    public static BagContents contents(ItemStack stack) {
        return stack.getOrDefault(StorageComponents.BAG_CONTENTS, BagContents.EMPTY);
    }

    public static int count(ItemStack stack) {
        return contents(stack).totalCount();
    }

    /** A working copy of the bag's inventory; {@link #save} writes it back. */
    public BagStackHandler handler(ItemStack stack) {
        BagStackHandler handler = new BagStackHandler(this.bag.capacity(), this::isItemValid);
        contents(stack).copyInto(handler);
        return handler;
    }

    /** Writes the inventory back; an emptied bag drops the component, so it equals a fresh one. */
    public static void save(ItemStack stack, BagStackHandler handler) {
        BagContents contents = BagContents.of(handler);
        if (contents.isEmpty()) {
            stack.remove(StorageComponents.BAG_CONTENTS);
        } else {
            stack.set(StorageComponents.BAG_CONTENTS, contents);
        }
    }

    /** Puts what fits of {@code items} into the bag and returns the rest. */
    public static ItemStack insert(ItemStack bagStack, ItemStack items, boolean simulate) {
        if (!(bagStack.getItem() instanceof BagItem bagItem)) {
            return items;
        }
        BagStackHandler handler = bagItem.handler(bagStack);
        ItemStack remainder = handler.insert(items, simulate);
        if (!simulate) {
            save(bagStack, handler);
        }
        return remainder;
    }

    // -- The bar and the tooltip ---------------------------------------------

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return count(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13f * count(stack) / this.bag.capacity());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return count(stack) >= this.bag.capacity() ? COLOR_BAG_FULL : COLOR_BAG;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        BagContents contents = contents(stack);
        int capacity = this.bag.capacity();
        if (contents.isEmpty()) {
            tooltip.add(Component.translatable("gui.pyrotech.tooltip.item.capacity.empty", capacity));
            return;
        }
        tooltip.add(Component.translatable("gui.pyrotech.tooltip.item.capacity", contents.totalCount(), capacity));
        if (FMLEnvironment.dist == Dist.CLIENT && ShiftKey.isDown()) {
            int digits = 0;
            for (ItemStack item : contents.stacks()) {
                digits = Math.max(digits, String.valueOf(item.getCount()).length());
            }
            for (ItemStack item : contents.stacks()) {
                String count = String.valueOf(item.getCount());
                Component padded = Component.literal("0".repeat(digits - count.length())).withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal(count).withStyle(ChatFormatting.YELLOW));
                tooltip.add(Component.literal(" ").append(padded).append(" ").append(item.getHoverName().copy().withStyle(ChatFormatting.GOLD)));
            }
        } else {
            tooltip.add(Component.translatable("gui.pyrotech.tooltip.extended.shift", ChatFormatting.GOLD, ChatFormatting.GRAY));
        }
    }

    // -- Use -----------------------------------------------------------------

    /** Sneak-click on a block: pour into its inventory, else spill in front while open. */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction face = context.getClickedFace();
        IItemHandler target = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, face);
        if (target != null) {
            if (!level.isClientSide) {
                this.pourInto(stack, target);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        BlockPos front = pos.relative(face);
        if (isOpen(stack) && level.isEmptyBlock(front)) {
            if (!level.isClientSide) {
                this.spill(stack, level, front);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    private void pourInto(ItemStack stack, IItemHandler target) {
        BagStackHandler handler = this.handler(stack);
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack inSlot = handler.getStackInSlot(slot);
            if (inSlot.isEmpty()) {
                continue;
            }
            ItemStack remaining = ItemHandlerHelper.insertItem(target, inSlot.copy(), false);
            if (remaining.getCount() != inSlot.getCount()) {
                handler.setStackInSlot(slot, remaining);
            }
        }
        save(stack, handler);
    }

    private void spill(ItemStack stack, Level level, BlockPos pos) {
        for (ItemStack item : contents(stack).stacks()) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, item.copy());
        }
        stack.remove(StorageComponents.BAG_CONTENTS);
    }

    /** Sneak-click at nothing: open or close. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            BagBlock.setOpen(stack, !isOpen(stack));
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return super.use(level, player, hand);
    }
}
