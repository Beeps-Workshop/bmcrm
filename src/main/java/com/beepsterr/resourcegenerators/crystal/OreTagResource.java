package com.beepsterr.resourcegenerators.crystal;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;

import java.util.Optional;

/**
 * A resource backed by a material tag such as {@code c:ores/iron}. Each cycle the resonator picks
 * a <em>random</em> block from the tag and rolls its break loot table — so mods that split an ore
 * into odd variants can't break us, and pack-added ore variants are automatically included.
 */
public record OreTagResource(TagKey<Block> oresTag) implements CrystalResource {

    public static final MapCodec<OreTagResource> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            TagKey.codec(Registries.BLOCK).fieldOf("tag").forGetter(OreTagResource::oresTag)
    ).apply(inst, OreTagResource::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, OreTagResource> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> ResourceLocation.STREAM_CODEC.encode(buf, value.oresTag().location()),
            buf -> new OreTagResource(TagKey.create(Registries.BLOCK, ResourceLocation.STREAM_CODEC.decode(buf))));

    @Override
    public ResourceKind kind() {
        return ResourceKind.ORE_TAG;
    }

    @Override
    public Optional<Holder<Block>> pickBlock(HolderLookup.Provider registries, RandomSource random) {
        return registries.lookupOrThrow(Registries.BLOCK).get(oresTag).flatMap(set -> {
            // Never roll blacklisted ores (e.g. nether gold ore).
            java.util.List<Holder<Block>> valid = set.stream()
                    .filter(h -> !com.beepsterr.resourcegenerators.Config.isBlacklisted(h))
                    .toList();
            return valid.isEmpty() ? Optional.empty() : Optional.of(valid.get(random.nextInt(valid.size())));
        });
    }

    @Override
    public String subtypeKey() {
        return "ore:" + oresTag.location();
    }

    /** Material name = the segment after the last {@code /} of the tag path (e.g. "iron"). */
    @Override
    public String materialName() {
        String path = oresTag.location().getPath();
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    @Override
    public Component displayName() {
        return CrystalResource.prettyName(materialName());
    }
}
