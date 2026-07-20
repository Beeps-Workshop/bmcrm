package com.beepsterr.resourcegenerators.util;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Small VoxelShape helpers. */
public final class ShapeUtils {

    private ShapeUtils() {}

    /**
     * Rotate a shape authored for the default {@link Direction#UP} facing so it points along
     * {@code facing} — the rotation that carries +Y onto that direction. Used by face-attached blocks
     * (crystals, modulators) so one authored shape covers all six facings.
     */
    public static VoxelShape rotateFromUp(VoxelShape upShape, Direction facing) {
        if (facing == Direction.UP) {
            return upShape;
        }
        VoxelShape[] acc = {Shapes.empty()};
        upShape.forAllBoxes((x1, y1, z1, x2, y2, z2) -> {
            double[] a = map(facing, x1 * 16, y1 * 16, z1 * 16);
            double[] b = map(facing, x2 * 16, y2 * 16, z2 * 16);
            acc[0] = Shapes.or(acc[0], Block.box(
                    Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.min(a[2], b[2]),
                    Math.max(a[0], b[0]), Math.max(a[1], b[1]), Math.max(a[2], b[2])));
        });
        return acc[0];
    }

    /** Map a pixel-space point of an UP-authored shape to the given facing (rotate +Y onto it). */
    private static double[] map(Direction facing, double x, double y, double z) {
        return switch (facing) {
            case DOWN -> new double[]{x, 16 - y, 16 - z};
            case NORTH -> new double[]{x, z, 16 - y};
            case SOUTH -> new double[]{x, 16 - z, y};
            case EAST -> new double[]{y, 16 - x, z};
            case WEST -> new double[]{16 - y, x, z};
            default -> new double[]{x, y, z};
        };
    }
}
