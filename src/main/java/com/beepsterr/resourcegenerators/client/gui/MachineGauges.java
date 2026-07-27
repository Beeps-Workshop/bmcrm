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
    /** The vanilla furnace's progress arrow sprite (24x16). */
    private static final ResourceLocation ARROW_SPRITE = ResourceLocation.withDefaultNamespace("container/furnace/burn_progress");

    /** Full flame height in pixels; the menu scales remaining burn to 0..this. */
    public static final int FLAME_HEIGHT = 13;

    // The canonical fuel/flame position lives in MachineLayout, shared with the menus that place the
    // slots — see that class before moving anything.
    /** Full progress-arrow width in pixels. */
    public static final int ARROW_WIDTH = 24;

    // Vanilla GUI palette, for the panels and slots drawn in code rather than painted into a texture.
    private static final int PANEL = 0xFFC6C6C6;
    private static final int HIGHLIGHT = 0xFFFFFFFF;
    private static final int SHADOW = 0xFF555555;
    private static final int SLOT_SHADOW = 0xFF373737;
    private static final int SLOT_HOLE = 0xFF8B8B8B;

    /** Draw the furnace flame at (x, y) — top-left of the 14x14 region — with {@code lit} px remaining. */
    public static void renderFlame(GuiGraphics g, int x, int y, int lit) {
        if (lit > 0) {
            g.blitSprite(LIT_SPRITE, 14, 14, 0, 14 - lit, x, y + 14 - lit, 14, lit);
        }
    }

    // --- Fluid tank gauge ---
    // Drawn from flat fills rather than a sprite: the machine GUI textures don't have a tank cut into
    // them yet, so this draws its own frame and can sit anywhere on the background.

    private static final int TANK_BORDER = 0xFF373737;
    private static final int TANK_BACKGROUND = 0xFF1A1A1A;

    /**
     * Draw a vertical tank at (x, y) with the given size, filled bottom-up to {@code amount/capacity}
     * in {@code color} (0xAARRGGBB).
     */
    public static void renderTank(GuiGraphics g, int x, int y, int width, int height,
                                  int amount, int capacity, int color) {
        // Inset the same way a slot is, so it reads as part of the same panel.
        g.fill(x, y, x + width, y + height, TANK_BACKGROUND);
        g.fill(x - 1, y - 1, x + width, y, SLOT_SHADOW);
        g.fill(x - 1, y - 1, x, y + height, SLOT_SHADOW);
        g.fill(x, y + height, x + width + 1, y + height + 1, HIGHLIGHT);
        g.fill(x + width, y, x + width + 1, y + height + 1, HIGHLIGHT);
        if (capacity <= 0 || amount <= 0) {
            return;
        }
        int filled = Math.min(height, Math.max(1, amount * height / capacity));
        g.fill(x, y + height - filled, x + width, y + height, color);
    }

    /** Draw the progress arrow at (x, y), revealed left-to-right to {@code progress} px wide. */
    public static void renderArrow(GuiGraphics g, int x, int y, int progress) {
        if (progress > 0) {
            g.blitSprite(ARROW_SPRITE, ARROW_WIDTH, 16, 0, 0, x, y, progress, 16);
        }
    }

    /**
     * Draw a vanilla-style GUI panel — flat fill with a bevelled edge. Used by machines that don't
     * have a painted background texture yet, so their layout still reads as a real GUI.
     */
    public static void renderPanel(GuiGraphics g, int x, int y, int width, int height) {
        g.fill(x, y, x + width, y + height, PANEL);
        g.fill(x, y, x + width - 1, y + 1, HIGHLIGHT);
        g.fill(x, y, x + 1, y + height - 1, HIGHLIGHT);
        g.fill(x + 1, y + height - 1, x + width, y + height, SHADOW);
        g.fill(x + width - 1, y + 1, x + width, y + height, SHADOW);
    }

    /**
     * Draw a slot background at (x, y) — the top-left of the 16x16 content area — for slots that the
     * machine's GUI texture doesn't have cut into it yet. Bevelled the same way vanilla paints them.
     */
    public static void renderSlot(GuiGraphics g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, SLOT_HOLE);
        g.fill(x - 1, y - 1, x + 16, y, SLOT_SHADOW);
        g.fill(x - 1, y - 1, x, y + 16, SLOT_SHADOW);
        g.fill(x, y + 16, x + 17, y + 17, HIGHLIGHT);
        g.fill(x + 16, y, x + 17, y + 17, HIGHLIGHT);
    }

    /** Draw the standard 3x9 inventory + hotbar slot backgrounds, top-left of the grid at (x, y). */
    public static void renderPlayerInventorySlots(GuiGraphics g, int x, int y) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                renderSlot(g, x + col * 18, y + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            renderSlot(g, x + col * 18, y + 58);
        }
    }

    /** Whether (mouseX, mouseY) is inside a tank drawn at (x, y) with the given size. */
    public static boolean isOverTank(int x, int y, int width, int height, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
