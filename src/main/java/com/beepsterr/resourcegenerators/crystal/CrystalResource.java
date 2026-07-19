package com.beepsterr.resourcegenerators.crystal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;

import java.util.Locale;
import java.util.Optional;

/**
 * What a crystal generates. Sealed so the set of kinds is closed and exhaustive.
 *
 * <ul>
 *   <li>{@link OreTagResource} — a material tag ({@code c:ores/<mat>}); the resonator rolls a
 *       <em>random</em> member's break loot table each cycle. This is the default for infused crystals.</li>
 *   <li>{@link BlockResource} — one specific block's break loot table (used by overrides / samples).</li>
 * </ul>
 *
 * <p>Both resolve, via {@link #pickBlock}, to a concrete block whose loot table the resonator rolls
 * (with a synthetic proper-tier tool, so ores yield their raw drops plus any Fortune from the crystal).
 */
public sealed interface CrystalResource permits BlockResource, OreTagResource {

    ResourceKind kind();

    /** Human-readable name of the thing being generated (e.g. "Iron"). */
    Component displayName();

    /** Material key used for color lookup (the {@code <mat>} of {@code c:ores/<mat>}); "" if unknown. */
    String materialName();

    /**
     * Packed 0xRRGGBB tint — server-safe: a curated color if one exists, else a hash of the id.
     * The client's {@code CrystalColors} inserts a "sample the result item's texture" layer between
     * these for item/block rendering.
     */
    default int color() {
        return MaterialColors.curated(materialName()).orElseGet(() -> MaterialColors.hash(subtypeKey()));
    }

    /** Resolve to a concrete block to roll this cycle (random for tag-backed resources). */
    Optional<Holder<Block>> pickBlock(HolderLookup.Provider registries, RandomSource random);

    /** A stable identity string used to distinguish crystal variants (e.g. for JEI subtypes). */
    String subtypeKey();

    /** The kinds of resource a crystal can represent; drives (de)serialization dispatch. */
    enum ResourceKind implements StringRepresentable {
        ORE_TAG("ore_tag"),
        BLOCK("block");

        private final String name;

        ResourceKind(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        public MapCodec<? extends CrystalResource> mapCodec() {
            return switch (this) {
                case ORE_TAG -> OreTagResource.MAP_CODEC;
                case BLOCK -> BlockResource.MAP_CODEC;
            };
        }

        public static final Codec<ResourceKind> CODEC = StringRepresentable.fromEnum(ResourceKind::values);
    }

    Codec<CrystalResource> CODEC =
            ResourceKind.CODEC.dispatch("type", CrystalResource::kind, ResourceKind::mapCodec);

    /** Explicit dispatch keeps generics simple and stays exhaustive as kinds are added. */
    StreamCodec<RegistryFriendlyByteBuf, CrystalResource> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeVarInt(value.kind().ordinal());
                switch (value.kind()) {
                    case ORE_TAG -> OreTagResource.STREAM_CODEC.encode(buf, (OreTagResource) value);
                    case BLOCK -> BlockResource.STREAM_CODEC.encode(buf, (BlockResource) value);
                }
            },
            buf -> {
                ResourceKind kind = ResourceKind.values()[buf.readVarInt()];
                return switch (kind) {
                    case ORE_TAG -> OreTagResource.STREAM_CODEC.decode(buf);
                    case BLOCK -> BlockResource.STREAM_CODEC.decode(buf);
                };
            }
    );

    /** Title-case a raw material/path segment (e.g. "nether_gold" -> "Nether Gold"). */
    static Component prettyName(String raw) {
        String[] parts = raw.replace('/', ' ').replace('_', ' ').trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return Component.literal(sb.toString());
    }
}
