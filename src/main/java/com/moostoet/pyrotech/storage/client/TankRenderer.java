package com.moostoet.pyrotech.storage.client;

import com.moostoet.pyrotech.library.fluid.client.FluidBoxRenderer;
import com.moostoet.pyrotech.storage.block.entity.TankBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

/** The tank group's fluid as one column drawn by the lowest tank, just inside the walls. */
public final class TankRenderer implements BlockEntityRenderer<TankBlockEntity> {

    private static final float INSET = 0.55f / 16;

    public TankRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TankBlockEntity tank, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (tank.connection().down()) {
            return;
        }
        FluidStack fluid = tank.tank().getFluid();
        if (fluid.isEmpty()) {
            return;
        }
        List<TankBlockEntity> group = tank.group();
        float percent = tank.groupAmount(group) / (float) tank.groupCapacity(group);
        float level = group.size() * percent - INSET;
        FluidBoxRenderer.Sprites sprites = FluidBoxRenderer.Sprites.of(fluid);
        FluidBoxRenderer.column(pose, FluidBoxRenderer.buffer(buffers), sprites.still(), sprites.color(), light, INSET, 0, level);
    }

    @Override
    public AABB getRenderBoundingBox(TankBlockEntity tank) {
        BlockPos pos = tank.getBlockPos();
        int height = tank.connection().down() ? 1 : tank.group().size();
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + height, pos.getZ() + 1);
    }
}
