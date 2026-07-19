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

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean isBlacklisted(ResourceLocation blockId) {
        return blockId != null && BLACKLISTED_ORES.get().contains(blockId.toString());
    }

    public static boolean isBlacklisted(Holder<Block> block) {
        return block.unwrapKey().map(key -> isBlacklisted(key.location())).orElse(false);
    }
}
