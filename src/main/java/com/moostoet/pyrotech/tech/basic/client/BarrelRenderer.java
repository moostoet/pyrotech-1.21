package com.moostoet.pyrotech.tech.basic.client;

import com.moostoet.pyrotech.library.fluid.client.FluidBoxRenderer;
import com.moostoet.pyrotech.library.interaction.client.SlotRenderer;
import com.moostoet.pyrotech.tech.basic.block.entity.BarrelBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidStack;

/** The barrel's fluid inside its walls, its four items floating on it, and the lid laid flat on top. */
public final class BarrelRenderer implements BlockEntityRenderer<BarrelBlockEntity> {

    private static final float INSET = 2 / 16f;
    private static final float FLUID_BOTTOM = 2 / 16f;
    private static final float FLUID_HEIGHT = 12 / 16f;
    private static final float ITEM_Y = 14 / 16f;
    private static final float ITEM_SCALE = 3 / 16f;
    private static final float LID_Y = 14 / 16f + 0.05f;

    public BarrelRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(BarrelBlockEntity barrel, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        FluidStack fluid = barrel.tank().getFluid();
        if (!fluid.isEmpty() && barrel.getLevel() != null) {
            int fluidLight = LevelRenderer.getLightColor(barrel.getLevel(), barrel.getBlockPos().above());
            float top = FLUID_BOTTOM + FLUID_HEIGHT * fluid.getAmount() / barrel.tank().getCapacity();
            FluidBoxRenderer.Sprites sprites = FluidBoxRenderer.Sprites.of(fluid);
            FluidBoxRenderer.column(pose, FluidBoxRenderer.buffer(buffers), sprites.still(), sprites.color(), fluidLight, INSET, FLUID_BOTTOM, top);
        }
        BlockHitResult hit = SlotRenderer.hitOn(barrel.getBlockPos());
        int hitSlot = hit != null && hit.getDirection() == Direction.UP && !barrel.isSealed() ? barrel.slotAt(hit) : -1;
        ItemStack held = SlotRenderer.heldItem();
        for (int slot = 0; slot < BarrelBlockEntity.SLOTS; slot++) {
            ItemStack stack = barrel.input().getStackInSlot(slot);
            pose.pushPose();
            pose.translate(slot % 2 == 0 ? 0.3125 : 0.6875, ITEM_Y, slot / 2 == 0 ? 0.3125 : 0.6875);
            pose.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
            if (!stack.isEmpty()) {
                SlotRenderer.renderItem(pose, buffers, stack, light);
            }
            if (slot == hitSlot) {
                SlotRenderer.renderGhostFor(pose, buffers, stack, held, barrel.input().isItemValid(slot, held));
            }
            pose.popPose();
        }
        ItemStack lid = barrel.lid().getStackInSlot(0);
        if (!lid.isEmpty()) {
            pose.pushPose();
            pose.translate(0.5, LID_Y, 0.5);
            pose.mulPose(Axis.XP.rotationDegrees(90));
            SlotRenderer.renderItem(pose, buffers, lid, light);
            pose.popPose();
        }
    }
}
