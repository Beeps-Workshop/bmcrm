package com.beepsterr.resourcegenerators.block;

/**
 * A tiny global "revision" bumped whenever a placed crystal is added or removed. Resonators cache
 * the revision they last scanned at and force a fresh scan the moment it changes — event-driven
 * rescanning without crystals needing to find nearby resonators.
 *
 * <p>It's intentionally coarse: any crystal change anywhere makes every loaded resonator rescan
 * once. Crystal place/break is an infrequent player action, so the occasional extra scan is cheap.
 */
public final class CrystalScanTracker {

    private static volatile long revision = 0;

    private CrystalScanTracker() {}

    /** Call when a placed crystal is added or removed. */
    public static void markDirty() {
        revision++;
    }

    public static long revision() {
        return revision;
    }
}
