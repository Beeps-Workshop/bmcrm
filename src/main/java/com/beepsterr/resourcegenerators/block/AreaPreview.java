package com.beepsterr.resourcegenerators.block;

import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * A block entity that has a spatial area worth visualising (a resonator's scan box, a Modulator's
 * footprint, etc.). The Tuning Fork toggles a wireframe overlay of {@link #getPreviewBoxes()} for any
 * block whose BE implements this. Computed from position + constants, so it works client-side. Returns
 * a list because a footprint may be more than one box (e.g. a "+" is two crossing arms).
 */
public interface AreaPreview {

    /** The world-space boxes to outline. */
    List<AABB> getPreviewBoxes();

    /** RGB colour of the overlay. */
    default int getPreviewColor() {
        return 0x4AB0C8; // resonance cyan
    }
}
