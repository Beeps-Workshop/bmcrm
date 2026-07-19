package com.beepsterr.resourcegenerators.compat;

import com.almostreliable.unified.api.AlmostUnified;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * AlmostUnified integration — isolated so it is only class-loaded when AlmostUnified is present
 * (callers must guard with {@code ModList.isLoaded("almostunified")}).
 *
 * <p>Rolling a random ore variant in a big pack can drop a non-preferred / disabled-variant item;
 * this remaps a rolled drop to the pack's preferred variant so, e.g., both Mekanism and AllTheOres
 * fluorite collapse to the one unified item.
 */
public final class AlmostUnifiedCompat {

    private AlmostUnifiedCompat() {}

    /** Return the stack unified to the pack's preferred variant, or the same stack if none applies. */
    public static ItemStack unify(ItemStack stack) {
        if (stack.isEmpty()) {
            return stack;
        }
        AlmostUnified au = AlmostUnified.INSTANCE;
        if (!au.isRuntimeLoaded()) {
            return stack;
        }
        Item target = au.getVariantItemTarget(stack.getItem());
        if (target == null || target == Items.AIR || target == stack.getItem()) {
            return stack;
        }
        return new ItemStack(target, stack.getCount());
    }
}
