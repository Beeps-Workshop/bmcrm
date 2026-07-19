package com.beepsterr.resourcegenerators.block;

import net.minecraft.world.phys.AABB;

/**
 * A block entity that has a spatial area worth visualising (a resonator's scan box, later a
 * Modulator's radius, etc.). The Tuning Fork toggles a wireframe overlay of {@link #getPreviewArea()}
 * for any block whose BE implements this. Computed from position + constants, so it works client-side.
 */
public interface AreaPreview {

    /** The world-space box to outline. */
    AABB getPreviewArea();

    /** RGB colour of the overlay. */
    default int getPreviewColor() {
        return 0x4AB0C8; // resonance cyan
    }
}
