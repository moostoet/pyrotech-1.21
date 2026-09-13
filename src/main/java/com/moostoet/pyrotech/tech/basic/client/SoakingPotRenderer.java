package com.moostoet.pyrotech.tech.basic.client;

import com.moostoet.pyrotech.library.fluid.client.FluidBoxRenderer;
import com.moostoet.pyrotech.library.interaction.client.SlotRenderer;
import com.moostoet.pyrotech.tech.basic.block.entity.SoakingPotBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

/** The pot's fluid inside its rim and the item soaking in it, both five pixels lower over a campfire. */
public final class SoakingPotRenderer implements BlockEntityRenderer<SoakingPotBlockEntity> {

    private static final float INSET = 4 / 16f;
    private static final float FLUID_BOTTOM = 1 / 16f;
    private static final float FLUID_HEIGHT = 7 / 16f;
    private static final float CAMPFIRE_DROP = 5 / 16f;
    private static final float ITEM_SCALE = 6 / 16f;
    private static final Vec3 COUNT_OFFSET = new Vec3(0, 0.25, 0);

    public SoakingPotRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(SoakingPotBlockEntity pot, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        float drop = pot.isOverCampfire() ? CAMPFIRE_DROP : 0;
        FluidStack fluid = pot.tank().getFluid();
        if (!fluid.isEmpty() && pot.getLevel() != null) {
            int fluidLight = LevelRenderer.getLightColor(pot.getLevel(), pot.getBlockPos().above());
            float top = FLUID_BOTTOM + FLUID_HEIGHT * fluid.getAmount() / pot.tank().getCapacity() - drop;
            FluidBoxRenderer.Sprites sprites = FluidBoxRenderer.Sprites.of(fluid);
            FluidBoxRenderer.column(pose, FluidBoxRenderer.buffer(buffers), sprites.still(), sprites.color(), fluidLight, INSET, FLUID_BOTTOM - drop, top);
        }
        ItemStack output = pot.output().getStackInSlot(0);
        ItemStack shown = output.isEmpty() ? pot.input().getStackInSlot(0) : output;
        BlockHitResult hit = SlotRenderer.hitOn(pot.getBlockPos());
        pose.pushPose();
        pose.translate(0.5, 0.5 - drop, 0.5);
        pose.pushPose();
        pose.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
        if (!shown.isEmpty()) {
            SlotRenderer.renderItem(pose, buffers, shown, light);
        }
        if (hit != null && hit.getDirection() == Direction.UP) {
            ItemStack held = SlotRenderer.heldItem();
            SlotRenderer.renderGhostFor(pose, buffers, shown, held,
                !held.isEmpty() && pot.input().insertItem(0, held, true).getCount() != held.getCount());
        }
        pose.popPose();
        if (SlotRenderer.showCounts(pot.getBlockPos()) && !shown.isEmpty()) {
            SlotRenderer.renderCount(pose, buffers, shown.getCount(), COUNT_OFFSET, Direction.NORTH);
        }
        pose.popPose();
    }
}
