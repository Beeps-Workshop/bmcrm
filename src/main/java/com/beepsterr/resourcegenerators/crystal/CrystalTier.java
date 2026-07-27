package com.beepsterr.resourcegenerators.crystal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * A data-driven "tier" of crystal — the quality axis, formed by the Crystal Former from a
 * base + catalyst pair (glass → amethyst → diamond → ender). Entries live in the datapack
 * registry {@code bmcrm:crystal_tier} and are defined by JSON.
 *
 * @param level      ordinal quality (1..n); higher is better
 * @param color      packed 0xRRGGBB tint used for display
 * @param rollChance per-work-cycle probability (0..1) that a crystal of this tier generates
 * @param base       the "base" item the Former consumes for this tier (e.g. a bowl)
 * @param catalyst   the tier-defining item the Former consumes (e.g. glass, a diamond)
 * @param hidden     when true, hide this tier from player-facing listings/counts (e.g. Ponder);
 *                   used for testing/creative-only tiers
 * @param resonanceCapacity how much resonance (in mB) a crystal of this tier can accumulate before it
 *                   saturates — one resonator work cycle stores 1mB whether or not the crystal
 *                   generated, so this is both the charge cap and the crystal's full melt value.
 *                   0 means the tier never accumulates.
 */
public record CrystalTier(
        int level,
        int color,
        float rollChance,
        Ingredient base,
        Ingredient catalyst,
        boolean hidden,
        int resonanceCapacity
) {
    /** Codec for a {@code "#RRGGBB"} (or bare {@code RRGGBB}) hex string ↔ packed int. */
    public static final Codec<Integer> HEX_COLOR = Codec.STRING.comapFlatMap(
            s -> {
                String hex = s.startsWith("#") ? s.substring(1) : s;
                try {
                    return DataResult.success((int) (Long.parseLong(hex, 16) & 0xFFFFFF));
                } catch (NumberFormatException e) {
                    return DataResult.error(() -> "Not a hex color: " + s);
                }
            },
            i -> String.format("#%06X", i & 0xFFFFFF)
    );

    public static final Codec<CrystalTier> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.fieldOf("level").forGetter(CrystalTier::level),
            HEX_COLOR.optionalFieldOf("color", 0xFFFFFF).forGetter(CrystalTier::color),
            Codec.FLOAT.optionalFieldOf("roll_chance", 0.05f).forGetter(CrystalTier::rollChance),
            Ingredient.CODEC.fieldOf("base_ingredient").forGetter(CrystalTier::base),
            Ingredient.CODEC.fieldOf("catalyst_ingredient").forGetter(CrystalTier::catalyst),
            Codec.BOOL.optionalFieldOf("hidden", false).forGetter(CrystalTier::hidden),
            Codec.INT.optionalFieldOf("resonance_capacity", 0).forGetter(CrystalTier::resonanceCapacity)
    ).apply(inst, CrystalTier::new));
}
