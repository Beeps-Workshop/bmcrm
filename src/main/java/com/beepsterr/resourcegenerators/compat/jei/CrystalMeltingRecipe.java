package com.beepsterr.resourcegenerators.compat.jei;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * A "what a crystal melts into" recipe for JEI: melting a crystal in the Crucible destroys it and
 * releases the resonance it banked while generating. One of these per tier, since what a crystal is
 * worth depends on its tier's capacity rather than on what it was infused with.
 *
 * @param crystals the crystals of this tier (JEI cycles them; melting works the same for all)
 * @param capacity the tier's resonance capacity in mB — what a saturated crystal is worth
 */
public record CrystalMeltingRecipe(List<ItemStack> crystals, int capacity) {
}
