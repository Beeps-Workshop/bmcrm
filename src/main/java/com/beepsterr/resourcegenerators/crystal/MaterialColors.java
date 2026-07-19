package com.beepsterr.resourcegenerators.crystal;

import net.minecraft.util.Mth;

import java.util.Map;
import java.util.Optional;

/**
 * Colors for crystal materials, layer 1 (curated) + the ultimate hash fallback. Both are computable
 * on server and client (no textures). The middle "generated from the result item's texture" layer
 * lives client-side in {@code CrystalColors}.
 */
public final class MaterialColors {

    private MaterialColors() {}

    /** Hand-picked colors keyed by material name (the {@code <mat>} in {@code c:ores/<mat>}). */
    private static final Map<String, Integer> CURATED = Map.ofEntries(
            Map.entry("iron", 0xD8AF93),
            Map.entry("gold", 0xF9D849),
            Map.entry("copper", 0xE0734D),
            Map.entry("coal", 0x36363A),
            Map.entry("diamond", 0x4AEDD9),
            Map.entry("emerald", 0x19C860),
            Map.entry("lapis", 0x2A5BD7),
            Map.entry("redstone", 0xD01A1A),
            Map.entry("quartz", 0xE6E0D8),
            Map.entry("netherite_scrap", 0x5B4A47),
            // common modded materials
            Map.entry("osmium", 0x1E9BD6),
            Map.entry("tin", 0xBFCCDC),
            Map.entry("lead", 0x494469),
            Map.entry("uranium", 0x4FB82E),
            Map.entry("fluorite", 0x3E9BB5),
            Map.entry("nickel", 0xC7BE8A),
            Map.entry("silver", 0xC5D2D6),
            Map.entry("zinc", 0xC2D0D2),
            Map.entry("aluminum", 0xD4D9DC),
            Map.entry("aluminium", 0xD4D9DC),
            Map.entry("platinum", 0x8FD4E8),
            Map.entry("iridium", 0xE0DCEC)
    );

    /** Layer 1: a curated color for this material, if one exists. */
    public static Optional<Integer> curated(String material) {
        return Optional.ofNullable(CURATED.get(material));
    }

    /** Ultimate fallback: a deterministic-but-arbitrary color derived from a string. */
    public static int hash(String key) {
        float hue = (key.hashCode() & 0xFFFF) / 65535.0f;
        return Mth.hsvToRgb(hue, 0.55f, 0.92f) & 0xFFFFFF;
    }
}
