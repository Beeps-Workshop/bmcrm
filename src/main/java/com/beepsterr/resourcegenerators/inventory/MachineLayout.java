package com.beepsterr.resourcegenerators.inventory;

/**
 * Shared GUI geometry for the crystal machines.
 *
 * <p>The fuel slot and its flame sit in the same place on every machine in the mod — bottom-right,
 * flame directly above the slot. A new machine lays its own widgets out around that; it does not
 * move it. These live here rather than in the client-only gauge code because the menus (which run on
 * the dedicated server too) place the slots, while the screens draw the backgrounds, and the two
 * must agree.
 *
 * <p>Slot coordinates are the top-left of the 16x16 content area, the way menus want them.
 */
public final class MachineLayout {

    private MachineLayout() {}

    /** Standard container size — the machines all use the vanilla 176x166 panel. */
    public static final int WIDTH = 176;
    public static final int HEIGHT = 166;

    /** The one true fuel slot position. */
    public static final int FUEL_SLOT_X = 140, FUEL_SLOT_Y = 53;
    /** The flame, directly above the fuel slot. */
    public static final int FLAME_X = 141, FLAME_Y = 37;

    /** Top-left of the player inventory grid; the hotbar sits 58px below it. */
    public static final int INVENTORY_X = 8, INVENTORY_Y = 84;
}
