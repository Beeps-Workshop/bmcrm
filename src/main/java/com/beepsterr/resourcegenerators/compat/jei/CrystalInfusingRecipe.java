package com.beepsterr.resourcegenerators.compat.jei;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * A Crystal Infuser recipe as shown in JEI for one material: any of {@code materials} + any tier's
 * blank crystal → the same-tier infused crystal. {@code blanks} and {@code results} are aligned by
 * tier so JEI's cycling shows corresponding pairs.
 */
public record CrystalInfusingRecipe(List<ItemStack> materials, List<ItemStack> blanks, List<ItemStack> results) {
}
