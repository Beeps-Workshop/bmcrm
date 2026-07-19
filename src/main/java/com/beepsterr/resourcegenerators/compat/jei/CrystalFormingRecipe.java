package com.beepsterr.resourcegenerators.compat.jei;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/** A Crystal Former recipe as shown in JEI: base + catalyst → a blank crystal of some tier. */
public record CrystalFormingRecipe(Ingredient base, Ingredient catalyst, ItemStack result) {
}
