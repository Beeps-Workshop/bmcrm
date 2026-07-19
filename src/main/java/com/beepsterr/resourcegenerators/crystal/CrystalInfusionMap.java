package com.beepsterr.resourcegenerators.crystal;

import com.beepsterr.resourcegenerators.registry.ModDataMaps;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Optional;

/**
 * Resolves the fed material item to the resource a crystal should become.
 *
 * <p>Order:
 * <ol>
 *   <li>the {@link ModDataMaps#INFUSION_OVERRIDE} data map (authoritative override), then</li>
 *   <li>tag inference: the item's material family in priority order
 *       ({@code c:ingots} → {@code c:gems} → {@code c:dusts} → {@code c:raw_materials}), whose
 *       {@code <mat>} sub-tag maps to {@code c:ores/<mat>}.</li>
 * </ol>
 * If neither yields a non-empty ore tag, the item is not infusable and returns empty.
 */
public final class CrystalInfusionMap {

    private CrystalInfusionMap() {}

    /** Material families to check, highest priority first. */
    private static final List<String> FAMILIES = List.of("ingots", "gems", "dusts", "raw_materials");

    public static Optional<CrystalResource> resolve(HolderLookup.Provider registries, ItemStack material) {
        if (material.isEmpty()) {
            return Optional.empty();
        }

        CrystalResource override = material.getItemHolder().getData(ModDataMaps.INFUSION_OVERRIDE);
        if (override != null) {
            return Optional.of(override);
        }

        return inferFromTags(registries, material);
    }

    private static Optional<CrystalResource> inferFromTags(HolderLookup.Provider registries, ItemStack material) {
        HolderLookup.RegistryLookup<Block> blocks = registries.lookupOrThrow(Registries.BLOCK);
        List<TagKey<Item>> itemTags = material.getItemHolder().tags().toList();

        for (String family : FAMILIES) {
            String prefix = family + "/";
            for (TagKey<Item> tag : itemTags) {
                ResourceLocation id = tag.location();
                if (!id.getNamespace().equals("c") || !id.getPath().startsWith(prefix)) {
                    continue;
                }
                String mat = id.getPath().substring(prefix.length());
                TagKey<Block> ores = TagKey.create(Registries.BLOCK,
                        ResourceLocation.fromNamespaceAndPath("c", "ores/" + mat));
                boolean hasUsableOre = blocks.get(ores)
                        .map(set -> set.stream().anyMatch(h -> !com.beepsterr.resourcegenerators.Config.isBlacklisted(h)))
                        .orElse(false);
                if (hasUsableOre) {
                    return Optional.of(new OreTagResource(ores));
                }
            }
        }
        return Optional.empty();
    }
}
