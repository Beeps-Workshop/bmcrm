package com.beepsterr.resourcegenerators.crystal;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * The footprint a Modulator projects, given a horizontal radius {@code h} and vertical radius {@code v}.
 * Both coverage (does a crystal at offset dx/dy/dz fall inside?) and the Tuning Fork preview boxes are
 * defined here so they can never disagree.
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

    /** Whether a crystal at the given offset from the modulator is inside this footprint. */
    public boolean covers(int h, int v, int dx, int dy, int dz) {
        if (Math.abs(dy) > v || Math.abs(dx) > h || Math.abs(dz) > h) {
            return false;
        }
        return switch (this) {
            case BOX -> true;
            case PLUS -> dx == 0 || dz == 0;
        };
    }

    /** Wireframe boxes for the Tuning Fork overlay (a box is one AABB; a plus is its two crossing arms). */
    public List<AABB> previewBoxes(BlockPos p, int h, int v) {
        double x = p.getX(), y = p.getY(), z = p.getZ();
        return switch (this) {
            case BOX -> List.of(new AABB(x - h, y - v, z - h, x + h + 1, y + v + 1, z + h + 1));
            case PLUS -> List.of(
                    new AABB(x - h, y - v, z, x + h + 1, y + v + 1, z + 1),   // X arm
                    new AABB(x, y - v, z - h, x + 1, y + v + 1, z + h + 1));  // Z arm
        };
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
