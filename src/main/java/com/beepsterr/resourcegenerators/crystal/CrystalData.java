package com.beepsterr.resourcegenerators.crystal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.beepsterr.resourcegenerators.registry.ModRegistries;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryFixedCodec;

import java.util.Optional;

/**
 * The full identity of a crystal item, stored in the {@code crystal_data} data component.
 *
 * <p>Holds the two structural axes — {@link CrystalTier tier} (quality) and
 * {@link CrystalResource resource} (what it generates). The resource is <em>optional</em>:
 * a "blank" crystal produced by the Crystal Former has a tier but no resource yet; the
 * Crystal Infuser fills it in. The third axis, enchantments, lives in the vanilla
 * {@code minecraft:enchantments} component on the stack (reusing vanilla glint/tooltip).
 */
public record CrystalData(Holder<CrystalTier> tier, Optional<CrystalResource> resource) {

    public static final Codec<CrystalData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            RegistryFixedCodec.create(ModRegistries.CRYSTAL_TIER_KEY).fieldOf("tier").forGetter(CrystalData::tier),
            CrystalResource.CODEC.optionalFieldOf("resource").forGetter(CrystalData::resource)
    ).apply(inst, CrystalData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CrystalData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.holderRegistry(ModRegistries.CRYSTAL_TIER_KEY), CrystalData::tier,
            ByteBufCodecs.optional(CrystalResource.STREAM_CODEC), CrystalData::resource,
            CrystalData::new
    );

    /** A blank (uninfused) crystal of the given tier. */
    public static CrystalData blank(Holder<CrystalTier> tier) {
        return new CrystalData(tier, Optional.empty());
    }

    /** This crystal with its resource set (used by the Infuser). */
    public CrystalData withResource(CrystalResource resource) {
        return new CrystalData(tier, Optional.of(resource));
    }

    public boolean isBlank() {
        return resource.isEmpty();
    }
}
