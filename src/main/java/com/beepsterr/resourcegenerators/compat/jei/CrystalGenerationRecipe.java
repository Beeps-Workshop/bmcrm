package com.beepsterr.resourcegenerators.compat.jei;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * A "what a crystal generates" recipe for JEI: any tier of a material's crystal, placed near a
 * Resonator, generates that material's drop. Lets players press R on e.g. raw iron and discover
 * the Iron Crystal produces it.
 *
 * @param crystals the infused crystals for this material (one per tier; JEI cycles them)
 * @param outputs  the items the crystal generates (the material's raw/gem/dust drop, all variants)
 */
public record CrystalGenerationRecipe(List<ItemStack> crystals, List<ItemStack> outputs) {
}
