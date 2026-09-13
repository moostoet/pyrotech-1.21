package com.moostoet.pyrotech.library.interaction;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Turns a block hit into the slot of a grid block: the hit position and face in the block's
 * own space, with the block turned back to face north as its model is drawn, and a grid
 * lookup over two of the local axes. The 1.12 blocks did this with a custom ray tracer
 * over per-slot boxes.
 */
public final class HitSlots {

    private HitSlots() {
    }

    /** The hit position inside the block, rotated into the space of a north-facing model. */
    public static Vec3 local(BlockHitResult hit, BlockPos pos, Direction facing) {
        Vec3 world = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
        return switch (facing) {
            case EAST -> new Vec3(world.z, world.y, 1 - world.x);
            case SOUTH -> new Vec3(1 - world.x, world.y, 1 - world.z);
            case WEST -> new Vec3(1 - world.z, world.y, world.x);
            default -> world;
        };
    }

    /** The hit face in the space of a north-facing model. */
    public static Direction localFace(Direction face, Direction facing) {
        if (face.getAxis().isVertical()) {
            return face;
        }
        int turns = facing.get2DDataValue() - Direction.NORTH.get2DDataValue();
        return Direction.from2DDataValue(Math.floorMod(face.get2DDataValue() - turns, 4));
    }

    /** The cell a coordinate falls in, for {@code count} cells spanning {@code min} to {@code max}, clamped to the ends. */
    public static int cell(double value, double min, double max, int count) {
        return Mth.clamp(Mth.floor((value - min) / (max - min) * count), 0, count - 1);
    }
}
