package com.beepsterr.resourcegenerators.compat.ponder;

import net.createmod.ponder.foundation.PonderIndex;

/**
 * Isolated Ponder entry point. Every method here touches Ponder classes, so the whole class is only
 * loaded when Ponder is actually installed. Callers MUST guard invocation with
 * {@code ModList.get().isLoaded("ponder")} so this never resolves when Ponder is absent.
 */
public final class PonderCompat {

    private PonderCompat() {}

    /** Register our Ponder plugin. Call from client setup, before Ponder's FMLLoadCompleteEvent. */
    public static void register() {
        PonderIndex.addPlugin(new BrgPonderPlugin());
    }
}
