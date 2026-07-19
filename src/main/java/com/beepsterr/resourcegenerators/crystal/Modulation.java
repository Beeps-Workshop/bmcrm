package com.beepsterr.resourcegenerators.crystal;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * A resonance modulation a resonator can apply to crystals in range. Radius-based modulations (like
 * {@link #SILK_TOUCH}) are projected from a Modulator block over an area and do not stack — a crystal
 * either is or isn't affected. The {@code color} is used for the Modulator's Tuning Fork overlay.
 */
public enum Modulation implements StringRepresentable {
    /** Rolls the crystal's ore with a Silk Touch tool, so it yields the ore block instead of drops. */
    SILK_TOUCH("silk_touch", 0xD8E8FF),
    /** Rolls with Fortune (level = number of overlapping Fortune modulators). Suppressed by Silk Touch. */
    FORTUNE("fortune", 0x74E88C);

    public static final Codec<Modulation> CODEC = StringRepresentable.fromEnum(Modulation::values);

    private final String serializedName;
    private final int color;

    Modulation(String serializedName, int color) {
        this.serializedName = serializedName;
        this.color = color;
    }

    /** Overlay colour for the Modulator's area preview. */
    public int color() {
        return color;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
