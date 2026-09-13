package com.moostoet.pyrotech.library.interaction.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The client side of a slot: the stored item drawn in place, the translucent ghost of the
 * item the player is about to put in, and the count shown while sneaking. These are
 * athenaeum's solid, additive, and text render passes, one helper each, for a block entity
 * renderer to call inside its own facing rotation.
 */
public final class SlotRenderer {

    private static final int GHOST_ALPHA = 0x60;
    private static final int COUNT_BACKGROUND = 0x80000000;
    private static final float COUNT_SCALE = 0.0125f;

    private SlotRenderer() {
    }

    /** Turns the pose so a north-facing model's coordinates draw at the block's facing. */
    public static void rotateToFacing(PoseStack pose, Direction facing) {
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90 * (facing.get2DDataValue() - Direction.NORTH.get2DDataValue())));
        pose.translate(-0.5, 0, -0.5);
    }

    /** The stored item, with the pose already at the slot's transform. */
    public static void renderItem(PoseStack pose, MultiBufferSource buffers, ItemStack stack, int light) {
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY,
            pose, buffers, null, 0);
    }

    /** The ghost of an item: full bright and see-through, with the pose at the slot's transform. */
    public static void renderGhost(PoseStack pose, MultiBufferSource buffers, ItemStack stack) {
        VertexConsumer target = buffers.getBuffer(RenderType.entityTranslucentEmissive(TextureAtlas.LOCATION_BLOCKS));
        MultiBufferSource ghost = type -> new GhostVertexConsumer(target, GHOST_ALPHA);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.NONE, LightTexture.FULL_BRIGHT,
            OverlayTexture.NO_OVERLAY, pose, ghost, null, 0);
    }

    /**
     * The 1.12 additive pass for one slot: the held item's ghost over an empty slot it may
     * go in, or the slot's own item lit up when the hand is empty or the player sneaks.
     */
    public static void renderGhostFor(PoseStack pose, MultiBufferSource buffers, ItemStack inSlot, ItemStack held, boolean heldValid) {
        if (inSlot.isEmpty()) {
            if (!held.isEmpty() && heldValid) {
                renderGhost(pose, buffers, held);
            }
        } else if (held.isEmpty() || sneaking()) {
            renderGhost(pose, buffers, inSlot);
        }
    }

    /** The block hit under the cursor when it is this block, else null. */
    @Nullable
    public static BlockHitResult hitOn(BlockPos pos) {
        return Minecraft.getInstance().hitResult instanceof BlockHitResult hit
            && hit.getType() == HitResult.Type.BLOCK
            && hit.getBlockPos().equals(pos) ? hit : null;
    }

    /** True while the player sneaks with the cursor on the block: the 1.12 condition for the counts. */
    public static boolean showCounts(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null && minecraft.player.isShiftKeyDown() && hitOn(pos) != null;
    }

    public static ItemStack heldItem() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null ? ItemStack.EMPTY : minecraft.player.getMainHandItem();
    }

    public static boolean sneaking() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null && minecraft.player.isShiftKeyDown();
    }

    /**
     * A count over a slot, facing the camera through walls, as athenaeum's
     * {@code renderItemCount} drew it. Counts of one stay hidden. The pose is at the slot's
     * translation, unrotated; {@code offset} is the 1.12 text offset in model space.
     */
    public static void renderCount(PoseStack pose, MultiBufferSource buffers, int count, Vec3 offset, Direction facing) {
        if (count <= 1) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        String text = String.valueOf(count);
        pose.pushPose();
        pose.translate(offset.x, offset.y, offset.z);
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90 * (facing.get2DDataValue() - Direction.NORTH.get2DDataValue())));
        pose.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
        pose.scale(COUNT_SCALE, -COUNT_SCALE, COUNT_SCALE);
        float x = -font.width(text) / 2f;
        font.drawInBatch(text, x, 0, 0xFFFFFFFF, false, pose.last().pose(), buffers, Font.DisplayMode.SEE_THROUGH,
            COUNT_BACKGROUND, LightTexture.FULL_BRIGHT);
        pose.popPose();
    }
}
