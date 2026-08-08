package com.beepsterr.resourcegenerators.compat.jei;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * How a mob crystal is made, as shown in JEI. There is no machine — a blank crystal is attuned in
 * the field, either by killing {@code kills} of the mob with it in the off hand or by draining a
 * spawner. The mob is represented by its spawn egg, which is the only client-side item that stands
 * for an entity type. {@code blanks} and {@code results} are aligned by tier so JEI's cycling shows
 * corresponding pairs.
 */
public record CrystalAttunementRecipe(ItemStack mobEgg, List<ItemStack> blanks, List<ItemStack> results,
                                      int kills) {
}
