package com.moostoet.pyrotech.tech.basic.client;

import com.moostoet.pyrotech.library.interaction.client.SlotRenderer;
import com.moostoet.pyrotech.tech.basic.block.entity.CompactingBinBlockEntity;
import com.moostoet.pyrotech.tech.basic.recipe.CompactingBinRecipe;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * The heap as a slab of the recipe's result rising with the fill, the count of results
 * it holds, and the held item's ghost until the bin is full.
 */
public final class CompactingBinRenderer implements BlockEntityRenderer<CompactingBinBlockEntity> {

    private static final float SLAB_WIDTH = 12 / 16f;
    private static final float SLAB_HEIGHT = 1 / 16f;
    private static final double FILL_HEIGHT = 13 / 16.0;
    private static final double FILL_BOTTOM = 1.5 / 16.0;
    private static final float GHOST_SCALE = 0.75f;
    private static final Vec3 COUNT_OFFSET = new Vec3(0, 0.25, 0);

    public CompactingBinRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CompactingBinBlockEntity bin, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Optional<CompactingBinRecipe> recipe = bin.currentRecipe();
        int total = bin.input().totalCount();
        boolean full = false;
        if (recipe.isPresent()) {
            double max = CompactingBinBlockEntity.RECIPES_PER_HEAP * recipe.get().amount();
            full = total >= max;
            pose.pushPose();
            pose.translate(0.5, total / max * FILL_HEIGHT + FILL_BOTTOM, 0.5);
            pose.scale(SLAB_WIDTH, SLAB_HEIGHT, SLAB_WIDTH);
            SlotRenderer.renderItem(pose, buffers, recipe.get().result(), light);
            pose.popPose();
            if (SlotRenderer.showCounts(bin.getBlockPos())) {
                pose.pushPose();
                pose.translate(0.5, 1, 0.5);
                SlotRenderer.renderCount(pose, buffers, total / recipe.get().amount(), COUNT_OFFSET, Direction.NORTH);
                pose.popPose();
            }
        }
        BlockHitResult hit = SlotRenderer.hitOn(bin.getBlockPos());
        if (hit != null && hit.getDirection() == Direction.UP && !full) {
            ItemStack held = SlotRenderer.heldItem();
            if (!held.isEmpty() && bin.input().isItemValid(0, held) && bin.input().insertItem(0, held, true).getCount() != held.getCount()) {
                pose.pushPose();
                pose.translate(0.5, 1, 0.5);
                pose.scale(GHOST_SCALE, GHOST_SCALE, GHOST_SCALE);
                SlotRenderer.renderGhost(pose, buffers, held);
                pose.popPose();
            }
        }
    }
}
