package com.moostoet.pyrotech.tool.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * A Pyrotech hoe: vanilla's, with the durability tooltip and an optional active behaviour.
 * An active hoe tills the eight blocks around the clicked one first, as in 1.12.
 */
public class PyrotechHoeItem extends HoeItem implements ActiveTool {

    @Nullable
    private final ActiveBehaviour active;

    public PyrotechHoeItem(Tier tier, @Nullable ActiveBehaviour active, Properties properties) {
        super(tier, properties);
        this.active = active;
    }

    @Override
    @Nullable
    public ActiveBehaviour activeBehaviour() {
        return this.active;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (this.active != null && this.active.isActive(context.getItemInHand())) {
            BlockPos centre = context.getClickedPos();
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x != 0 || z != 0) {
                        super.useOn(at(context, centre.offset(x, 0, z)));
                    }
                }
            }
        }
        return super.useOn(context);
    }

    private static UseOnContext at(UseOnContext context, BlockPos pos) {
        BlockPos offset = pos.subtract(context.getClickedPos());
        BlockHitResult hit = new BlockHitResult(context.getClickLocation().add(offset.getX(), 0, offset.getZ()),
            context.getClickedFace(), pos, context.isInside());
        return new UseOnContext(context.getLevel(), context.getPlayer(), context.getHand(), context.getItemInHand(), hit);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return ActiveTools.destroySpeed(this.active, stack, super.getDestroySpeed(stack, state), true);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        ActiveTools.inventoryTick(this.active, stack, level, entity);
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
        return ActiveTools.damageItem(this.active, stack, amount, entity);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ActiveTools.appendHoverText(this.active, stack, tooltip, ActiveBehaviour.ToolKind.HOE);
    }
}
