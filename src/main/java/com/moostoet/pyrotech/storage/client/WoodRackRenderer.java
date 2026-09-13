package com.moostoet.pyrotech.storage.client;

import com.moostoet.pyrotech.library.interaction.HitSlots;
import com.moostoet.pyrotech.library.interaction.client.SlotRenderer;
import com.moostoet.pyrotech.storage.block.entity.WoodRackBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** The rack's logs lying through it in three rows of three, their counts on the side the player looks at. */
public final class WoodRackRenderer implements BlockEntityRenderer<WoodRackBlockEntity> {

    private static final double OPENING = 10 / 16.0;
    private static final double SIZE = OPENING / WoodRackBlockEntity.COLUMNS;
    private static final float LENGTH = 14 / 16f;
    private static final Vec3 COUNT_OFFSET_NORTH = new Vec3(0, 0, -0.5);
    private static final Vec3 COUNT_OFFSET_SOUTH = new Vec3(0, 0, 0.5);

    public WoodRackRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(WoodRackBlockEntity rack, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Direction facing = rack.facing();
        BlockHitResult hit = SlotRenderer.hitOn(rack.getBlockPos());
        int hitSlot = hit == null ? -1 : rack.slotAt(hit);
        Vec3 countOffset = hit == null || !SlotRenderer.showCounts(rack.getBlockPos()) ? null
            : countOffset(HitSlots.localFace(hit.getDirection(), facing));
        ItemStack held = SlotRenderer.heldItem();
        pose.pushPose();
        SlotRenderer.rotateToFacing(pose, facing);
        for (int slot = 0; slot < rack.handler().getSlots(); slot++) {
            ItemStack stack = rack.handler().getStackInSlot(slot);
            int column = slot % WoodRackBlockEntity.COLUMNS;
            int row = slot / WoodRackBlockEntity.COLUMNS;
            pose.pushPose();
            pose.translate(column * SIZE + SIZE / 2 + WoodRackBlockEntity.OPENING_MIN_X, row * SIZE + SIZE / 2 + WoodRackBlockEntity.OPENING_MIN_Y, 0.5);
            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees(90));
            pose.scale((float) SIZE, LENGTH, (float) SIZE);
            if (!stack.isEmpty()) {
                SlotRenderer.renderItem(pose, buffers, stack, light);
            }
            if (slot == hitSlot) {
                SlotRenderer.renderGhostFor(pose, buffers, stack, held, rack.accepts(slot, held));
            }
            pose.popPose();
            if (countOffset != null && !stack.isEmpty()) {
                SlotRenderer.renderCount(pose, buffers, stack.getCount(), countOffset, facing);
            }
            pose.popPose();
        }
        pose.popPose();
    }

    private static Vec3 countOffset(Direction localFace) {
        return switch (localFace) {
            case NORTH -> COUNT_OFFSET_NORTH;
            case SOUTH -> COUNT_OFFSET_SOUTH;
            default -> null;
        };
    }
}
