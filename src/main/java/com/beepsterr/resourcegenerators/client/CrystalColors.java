package com.beepsterr.resourcegenerators.client;

import com.beepsterr.resourcegenerators.crystal.CrystalResource;
import com.beepsterr.resourcegenerators.crystal.MaterialColors;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Client-side crystal color resolution: curated color (layer 1) → average of the result item's
 * texture (layer 2) → hash fallback. The middle layer reads the item's PNG straight from the
 * resource pack (not the atlas) so it's a clean material color, not a stone-blended ore texture.
 */
public final class CrystalColors {

    private CrystalColors() {}

    /** Result item forms to sample, most representative first. */
    private static final List<String> FAMILIES = List.of("raw_materials", "gems", "dusts", "ingots");

    /** material -> sampled color (or -1 = none); cached for the session. */
    private static final Map<String, Integer> SAMPLE_CACHE = new HashMap<>();

    public static int display(CrystalResource resource) {
        String material = resource.materialName();
        Optional<Integer> curated = MaterialColors.curated(material);
        if (curated.isPresent()) {
            return curated.get();
        }
        if (!material.isEmpty()) {
            int sampled = sample(material);
            if (sampled != -1) {
                return sampled;
            }
        }
        return MaterialColors.hash(resource.subtypeKey());
    }

    private static int sample(String material) {
        Integer cached = SAMPLE_CACHE.get(material);
        if (cached != null) {
            return cached;
        }
        int color = computeSample(material);
        SAMPLE_CACHE.put(material, color);
        return color;
    }

    private static int computeSample(String material) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return -1;
        }
        var items = mc.level.registryAccess().lookupOrThrow(Registries.ITEM);
        Item representative = null;
        for (String family : FAMILIES) {
            TagKey<Item> tag = TagKey.create(Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath("c", family + "/" + material));
            var set = items.get(tag);
            if (set.isPresent() && set.get().size() > 0) {
                representative = set.get().stream().findFirst().map(Holder::value).orElse(null);
                if (representative != null) {
                    break;
                }
            }
        }
        if (representative == null) {
            return -1;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(representative);
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(
                itemId.getNamespace(), "textures/item/" + itemId.getPath() + ".png");
        var resource = mc.getResourceManager().getResource(texture);
        if (resource.isEmpty()) {
            return -1;
        }
        try (InputStream stream = resource.get().open(); NativeImage image = NativeImage.read(stream)) {
            long r = 0, g = 0, b = 0, n = 0;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int px = image.getPixelRGBA(x, y); // 0xAABBGGRR
                    if (((px >>> 24) & 0xFF) < 128) {
                        continue; // skip (mostly) transparent pixels
                    }
                    r += px & 0xFF;
                    g += (px >>> 8) & 0xFF;
                    b += (px >>> 16) & 0xFF;
                    n++;
                }
            }
            if (n == 0) {
                return -1;
            }
            return (int) ((r / n) << 16 | (g / n) << 8 | (b / n));
        } catch (Exception e) {
            return -1;
        }
    }
}
