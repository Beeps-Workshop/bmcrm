package com.beepsterr.resourcegenerators.crystal;

import com.beepsterr.resourcegenerators.BeepsResourceGenerators;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
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

    /** What a well-formed entry has to look like. Also does all the encoding. */
    public static final Codec<CrystalTier> STRICT_CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.fieldOf("level").forGetter(CrystalTier::level),
            HEX_COLOR.optionalFieldOf("color", 0xFFFFFF).forGetter(CrystalTier::color),
            Codec.FLOAT.optionalFieldOf("roll_chance", 0.05f).forGetter(CrystalTier::rollChance),
            Ingredient.CODEC.fieldOf("base_ingredient").forGetter(CrystalTier::base),
            Ingredient.CODEC.fieldOf("catalyst_ingredient").forGetter(CrystalTier::catalyst),
            Codec.BOOL.optionalFieldOf("hidden", false).forGetter(CrystalTier::hidden),
            Codec.INT.optionalFieldOf("resonance_capacity", 0).forGetter(CrystalTier::resonanceCapacity)
    ).apply(inst, CrystalTier::new));

    /**
     * Level stamped on the placeholder below. A registry keeps its values in an identity map and
     * rejects the same instance twice, so brokenness cannot be a shared singleton — it is carried
     * by this level instead, which also survives the trip through the network codec so clients
     * filter the placeholder out of JEI and the creative tab exactly like the server does.
     */
    private static final int BROKEN_LEVEL = Integer.MIN_VALUE;

    /**
     * Stand-in registered when an entry fails to parse. A datapack registry aborts world loading
     * if any of its files errors, which turns one typo in a pack into an unloadable world — so a
     * bad entry becomes one of these instead, and every place that lists tiers drops it. Its
     * ingredients are an empty tag, so the Former can never produce one even if a filter is missed.
     */
    public static CrystalTier broken() {
        Ingredient nothing = Ingredient.of(
                TagKey.create(Registries.ITEM, BeepsResourceGenerators.rl("uncraftable")));
        return new CrystalTier(BROKEN_LEVEL, 0xFF00FF, 0f, nothing, nothing, true, 0);
    }

    /** True for a placeholder left behind by an entry whose JSON did not parse. */
    public boolean isBroken() {
        return level == BROKEN_LEVEL;
    }

    /** Never fails: a malformed entry is logged and replaced by {@link #BROKEN}. */
    private static final Decoder<CrystalTier> LENIENT_DECODER = new Decoder<>() {
        @Override
        public <T> DataResult<Pair<CrystalTier, T>> decode(DynamicOps<T> ops, T input) {
            DataResult<Pair<CrystalTier, T>> parsed = STRICT_CODEC.decode(ops, input);
            if (parsed.error().isEmpty()) {
                return parsed;
            }
            if (isEmptyObject(ops, input)) {
                BeepsResourceGenerators.LOGGER.error(
                        "Ignoring an empty crystal tier entry. An empty JSON object does not remove a tier."
                        + "to remove one use "
                        + "{\"neoforge:conditions\": [{\"type\": \"neoforge:false\"}]}");
            } else {
                BeepsResourceGenerators.LOGGER.error("Ignoring a malformed crystal tier entry: {} -- in {}",
                        parsed.error().get().message(), abbreviate(input));
            }
            return DataResult.success(Pair.of(broken(), input));
        }
    };

    /** Registered as both the file and the network codec for {@code bmcrm:crystal_tier}. */
    public static final Codec<CrystalTier> CODEC =
            Codec.of(STRICT_CODEC, LENIENT_DECODER, "CrystalTier[lenient]");

    private static <T> boolean isEmptyObject(DynamicOps<T> ops, T input) {
        return ops.getMap(input).result().map(map -> map.entries().findAny().isEmpty()).orElse(false);
    }

    private static String abbreviate(Object input) {
        String s = String.valueOf(input);
        return s.length() <= 200 ? s : s.substring(0, 200) + "...";
    }
}
