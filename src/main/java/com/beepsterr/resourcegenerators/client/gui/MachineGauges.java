package com.beepsterr.resourcegenerators.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Shared GUI drawing for the crystal machines: the fuel flame. Uses the vanilla furnace burn sprite so
 * the indicator is identical to a furnace's, depleting from the top as the fuel burns down.
 */
public final class MachineGauges {

    private MachineGauges() {}

    /** The vanilla furnace's lit-flame sprite (14x14). */
    private static final ResourceLocation LIT_SPRITE = ResourceLocation.withDefaultNamespace("container/furnace/lit_progress");

    /** Full flame height in pixels; the menu scales remaining burn to 0..this. */
    public static final int FLAME_HEIGHT = 13;

    /** Draw the furnace flame at (x, y) — top-left of the 14x14 region — with {@code lit} px remaining. */
    public static void renderFlame(GuiGraphics g, int x, int y, int lit) {
        if (lit > 0) {
            g.blitSprite(LIT_SPRITE, 14, 14, 0, 14 - lit, x, y + 14 - lit, 14, lit);
        }
    }
}
