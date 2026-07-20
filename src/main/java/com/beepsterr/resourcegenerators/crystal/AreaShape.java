package com.beepsterr.resourcegenerators.crystal;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * The footprint a Modulator projects, given a horizontal radius {@code h} and vertical radius {@code v}.
 * Both coverage (does a crystal at offset dx/dy/dz fall inside?) and the Tuning Fork preview boxes are
 * defined here so they can never disagree. The footprint is authored for the default UP facing and
 * rotates with the modulator: a wall-mounted "+" stands up in the wall plane, etc.
 */
public enum AreaShape implements StringRepresentable {
    /** A filled box: |dx|<=h, |dz|<=h, |dy|<=v (h=1 -> 3x3). */
    BOX("box"),
    /** A "+": the two arms along X and Z within h, |dy|<=v (arm 2 -> reaches 2 blocks each way). */
    PLUS("plus");

    public static final Codec<AreaShape> CODEC = StringRepresentable.fromEnum(AreaShape::values);

    private final String serializedName;

    AreaShape(String serializedName) {
        this.serializedName = serializedName;
    }

    /** Whether a crystal at the given (UP-facing local) offset is inside this footprint. */
    public boolean covers(int h, int v, int dx, int dy, int dz) {
        if (Math.abs(dy) > v || Math.abs(dx) > h || Math.abs(dz) > h) {
            return false;
        }
        return switch (this) {
            case BOX -> true;
            case PLUS -> dx == 0 || dz == 0;
        };
    }

    /** Coverage for a modulator with the given facing: the world offset is rotated into local space first. */
    public boolean covers(Direction facing, int h, int v, int dx, int dy, int dz) {
        int[] local = worldToLocal(facing, dx, dy, dz);
        return covers(h, v, local[0], local[1], local[2]);
    }

    /** Wireframe boxes for the Tuning Fork overlay, rotated to the modulator's facing. */
    public List<AABB> previewBoxes(Direction facing, BlockPos p, int h, int v) {
        List<AABB> out = new ArrayList<>();
        for (AABB local : localBoxes(h, v)) {
            out.add(rotate(facing, local).move(p.getX(), p.getY(), p.getZ()));
        }
        return out;
    }

    /** Offset boxes (relative to the modulator cell) for the UP facing. */
    private List<AABB> localBoxes(int h, int v) {
        return switch (this) {
            case BOX -> List.of(new AABB(-h, -v, -h, h + 1, v + 1, h + 1));
            case PLUS -> List.of(
                    new AABB(-h, -v, 0, h + 1, v + 1, 1),   // X arm
                    new AABB(0, -v, -h, 1, v + 1, h + 1));  // Z arm
        };
    }

    /** Rotate a world offset back into the UP-facing local frame (inverse of the facing rotation). */
    private static int[] worldToLocal(Direction facing, int dx, int dy, int dz) {
        return switch (facing) {
            case DOWN -> new int[]{dx, -dy, -dz};
            case NORTH -> new int[]{dx, -dz, dy};
            case SOUTH -> new int[]{dx, dz, -dy};
            case EAST -> new int[]{-dy, dx, dz};
            case WEST -> new int[]{dy, -dx, dz};
            default -> new int[]{dx, dy, dz};
        };
    }

    /** Rotate an offset AABB (UP-facing) about the modulator's cell centre onto the given facing. */
    private static AABB rotate(Direction facing, AABB box) {
        if (facing == Direction.UP) {
            return box;
        }
        double[] a = localToWorld(facing, box.minX - 0.5, box.minY - 0.5, box.minZ - 0.5);
        double[] b = localToWorld(facing, box.maxX - 0.5, box.maxY - 0.5, box.maxZ - 0.5);
        return new AABB(
                Math.min(a[0], b[0]) + 0.5, Math.min(a[1], b[1]) + 0.5, Math.min(a[2], b[2]) + 0.5,
                Math.max(a[0], b[0]) + 0.5, Math.max(a[1], b[1]) + 0.5, Math.max(a[2], b[2]) + 0.5);
    }

    /** Rotate a local (UP-facing) offset onto the given facing (+Y carried onto that direction). */
    private static double[] localToWorld(Direction facing, double x, double y, double z) {
        return switch (facing) {
            case DOWN -> new double[]{x, -y, -z};
            case NORTH -> new double[]{x, z, -y};
            case SOUTH -> new double[]{x, -z, y};
            case EAST -> new double[]{y, -x, z};
            case WEST -> new double[]{-y, x, z};
            default -> new double[]{x, y, z};
        };
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
