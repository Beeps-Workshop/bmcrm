package com.beepsterr.resourcegenerators.crystal;

import com.beepsterr.resourcegenerators.registry.ModDataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * Accumulated resonance on a crystal — how much it has generated over its life, in fluid mB.
 *
 * <p>Stored in the {@code crystal_charge} component as a plain int, deliberately <em>outside</em>
 * {@link CrystalData}: the charge is orthogonal to a crystal's identity, so it must not affect
 * recipe matching or the JEI subtype key (which would otherwise split one material into an entry
 * per charge value). Each successful generation adds one unit, capped at the tier's
 * {@link CrystalTier#resonanceCapacity()}; melting the crystal yields exactly what it holds.
 */
public final class CrystalCharge {

    private CrystalCharge() {}

    /** How much this crystal currently holds (0 if it has never generated). */
    public static int get(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.CRYSTAL_CHARGE.get(), 0);
    }

    /** The most this crystal could ever hold, from its tier (0 if it has no crystal data). */
    public static int capacity(ItemStack stack) {
        CrystalData data = stack.get(ModDataComponents.CRYSTAL_DATA.get());
        return data == null ? 0 : capacity(data);
    }

    public static int capacity(CrystalData data) {
        return Math.max(0, data.tier().value().resonanceCapacity());
    }

    /** Set the charge, clamped to the tier's capacity; the component is dropped when it lands on 0. */
    public static void set(ItemStack stack, int amount) {
        int clamped = Mth.clamp(amount, 0, capacity(stack));
        if (clamped <= 0) {
            stack.remove(ModDataComponents.CRYSTAL_CHARGE.get());
        } else {
            stack.set(ModDataComponents.CRYSTAL_CHARGE.get(), clamped);
        }
    }

    /** Whether this crystal has hit its tier's cap and can store no more. */
    public static boolean isSaturated(ItemStack stack) {
        int capacity = capacity(stack);
        return capacity > 0 && get(stack) >= capacity;
    }

    /** 0..1 fill of the crystal's capacity; 0 for tiers that can't accumulate. */
    public static float fraction(ItemStack stack) {
        int capacity = capacity(stack);
        return capacity <= 0 ? 0f : (float) get(stack) / capacity;
    }
}
