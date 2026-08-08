package com.beepsterr.resourcegenerators.crystal;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * A resource backed by a source {@link Block}: the resonator rolls that block's
 * existing break loot table (so vanilla and modded ores work with no hand-authored
 * loot tables, and Fortune/Silk Touch from the crystal's enchantments apply to the
 * synthetic mining tool).
 */
public record BlockResource(Holder<Block> block) implements CrystalResource {

    public static final MapCodec<BlockResource> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            BuiltInRegistries.BLOCK.holderByNameCodec().fieldOf("block").forGetter(BlockResource::block)
    ).apply(inst, BlockResource::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BlockResource> STREAM_CODEC =
            ByteBufCodecs.holderRegistry(Registries.BLOCK).map(BlockResource::new, BlockResource::block);

    @Override
    public ResourceKind kind() {
        return ResourceKind.BLOCK;
    }

    /** The block's own break loot table — rolled by the resonator (a later slice). */
    public ResourceKey<LootTable> lootTable() {
        return block.value().getLootTable();
    }

    @Override
    public Optional<Holder<Block>> pickBlock(HolderLookup.Provider registries, RandomSource random) {
        return com.beepsterr.resourcegenerators.Config.isBlacklisted(block) ? Optional.empty() : Optional.of(block);
    }

    @Override
    public List<ItemStack> roll(ServerLevel level, BlockPos origin, ItemStack tool, RandomSource random) {
        return pickBlock(level.registryAccess(), random)
                .map(picked -> CrystalResource.rollBlock(level, origin, tool, picked.value()))
                .orElseGet(List::of);
    }

    @Override
    public String subtypeKey() {
        return "block:" + block.unwrapKey().map(k -> k.location().toString()).orElse("?");
    }

    @Override
    public Component displayName() {
        String name = block.value().getName().getString();
        // Ore blocks read nicer as just the material ("Iron Ore" -> "Iron").
        if (name.endsWith(" Ore")) {
            name = name.substring(0, name.length() - 4);
        }
        return Component.literal(name);
    }

    /** Best-effort material name from the block id (e.g. "deepslate_iron_ore" -> "iron"). */
    @Override
    public String materialName() {
        return block.unwrapKey().map(k -> {
            String path = k.location().getPath();
            if (path.startsWith("deepslate_")) {
                path = path.substring("deepslate_".length());
            }
            if (path.endsWith("_ore")) {
                path = path.substring(0, path.length() - "_ore".length());
            }
            return path;
        }).orElse("");
    }
}
