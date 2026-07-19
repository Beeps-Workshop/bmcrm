package com.beepsterr.resourcegenerators;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * Common config. Currently just a blacklist of ore blocks that crystals should never generate from
 * (and that won't be offered as crystals) — a pragmatic escape hatch for odd sources the {@code c:}
 * tags surface, e.g. nether gold ore (which drops gold nuggets, not raw gold).
 */
public final class Config {

    private Config() {}

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLACKLISTED_ORES = BUILDER
            .comment("Ore block ids that crystals never generate from, and that are not offered as crystals.")
            .defineListAllowEmpty("blacklistedOres",
                    List.of("minecraft:nether_gold_ore"),
                    o -> o instanceof String s && ResourceLocation.tryParse(s) != null);

    public static final ModConfigSpec.DoubleValue RAIN_EFFICIENCY = BUILDER
            .comment("Roll-chance multiplier for a crystal exposed to rain (or whose resonator is rained on).",
                    "1.0 disables the penalty entirely; 0.5 halves the effective roll chance.")
            .defineInRange("rainEfficiency", 0.5, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue MOB_EFFICIENCY = BUILDER
            .comment("Roll-chance multiplier while a hostile mob is near the resonator (noisy neighbors).",
                    "1.0 disables the penalty entirely; 0.5 halves the effective roll chance.")
            .defineInRange("mobEfficiency", 0.5, 0.0, 1.0);

    public static final ModConfigSpec.IntValue MOB_DISRUPTION_RADIUS = BUILDER
            .comment("How many blocks around the resonator are scanned for hostile mobs.")
            .defineInRange("mobDisruptionRadius", 6, 0, 32);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean isBlacklisted(ResourceLocation blockId) {
        return blockId != null && BLACKLISTED_ORES.get().contains(blockId.toString());
    }

    public static boolean isBlacklisted(Holder<Block> block) {
        return block.unwrapKey().map(key -> isBlacklisted(key.location())).orElse(false);
    }
}
