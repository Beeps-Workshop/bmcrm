package com.beepsterr.resourcegenerators.crystal;

import com.beepsterr.resourcegenerators.BeepsResourceGenerators;
import com.beepsterr.resourcegenerators.Config;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * A resource backed by a mob: the resonator rolls that entity's death loot table, so rare drops,
 * modded mobs and any pack-edited table all work with no hand-authored data.
 *
 * <p>Rolling needs a real entity for {@link LootContextParams#THIS_ENTITY}, so a throwaway instance
 * is created and never added to the world. A {@link FakePlayerFactory fake player} is supplied as
 * the killer because vanilla gates the interesting drops behind {@code killed_by_player} — without
 * one a zombie crystal would yield only rotten flesh and a blaze crystal would yield nothing at all.
 */
public record EntityResource(Holder<EntityType<?>> entity) implements CrystalResource {

    public static final MapCodec<EntityResource> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            BuiltInRegistries.ENTITY_TYPE.holderByNameCodec().fieldOf("entity").forGetter(EntityResource::entity)
    ).apply(inst, EntityResource::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, EntityResource> STREAM_CODEC =
            ByteBufCodecs.holderRegistry(Registries.ENTITY_TYPE).map(EntityResource::new, EntityResource::entity);

    /**
     * The mobs offered as crystals, in id order. Deliberately a <em>client-safe</em> rule: loot
     * tables live in the server-only reloadable registries, so listings can't ask "does this mob
     * drop anything". {@link MobCategory#MISC} covers boats, projectiles, armour stands, villagers
     * and iron golems, which leaves the spawnable mobs; bosses come off via the config blacklist.
     */
    public static List<Holder.Reference<EntityType<?>>> attunable(HolderLookup.Provider registries) {
        return registries.lookupOrThrow(Registries.ENTITY_TYPE).listElements()
                .filter(h -> isAttunable(h.value()))
                .sorted(Comparator.comparing(h -> h.key().location().toString()))
                .toList();
    }

    /**
     * Mobs excluded because their death table yields nothing. A datapack tag rather than a config
     * list because it is a fact about the mob, not a preference, and because tags — unlike loot
     * tables — are synced, so the creative tab and JEI can consult it client-side.
     */
    public static final TagKey<EntityType<?>> NO_DROPS =
            TagKey.create(Registries.ENTITY_TYPE, BeepsResourceGenerators.rl("no_drops"));

    /** Whether this mob may back a crystal at all — the same rule the listings and both
     *  attunement routes use, so a blacklisted boss can't be farmed in by hand either. */
    public static boolean isAttunable(EntityType<?> type) {
        return type.getCategory() != MobCategory.MISC
                && !type.is(NO_DROPS)
                && !Config.isMobBlacklisted(BuiltInRegistries.ENTITY_TYPE.getKey(type));
    }

    @Override
    public ResourceKind kind() {
        return ResourceKind.ENTITY;
    }

    /** Mobs aren't block-backed; the resonator reaches them through {@link #roll} instead. */
    @Override
    public Optional<Holder<Block>> pickBlock(HolderLookup.Provider registries, RandomSource random) {
        return Optional.empty();
    }

    @Override
    public List<ItemStack> roll(ServerLevel level, BlockPos origin, ItemStack tool, RandomSource random) {
        Entity sample = entity.value().create(level);
        if (!(sample instanceof LivingEntity living)) {
            return List.of(); // non-living (or un-creatable) entity types have no death table
        }
        Vec3 pos = Vec3.atCenterOf(origin);
        living.setPos(pos);

        var killer = FakePlayerFactory.getMinecraft(level);
        DamageSource source = level.damageSources().playerAttack(killer);

        LootTable table = level.getServer().reloadableRegistries().getLootTable(living.getLootTable());
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, living)
                .withParameter(LootContextParams.ORIGIN, pos)
                .withParameter(LootContextParams.DAMAGE_SOURCE, source)
                .withParameter(LootContextParams.ATTACKING_ENTITY, killer)
                .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer)
                .create(LootContextParamSets.ENTITY);
        return CrystalResource.dropped(table.getRandomItems(params));
    }

    @Override
    public String subtypeKey() {
        return "entity:" + entity.unwrapKey().map(k -> k.location().toString()).orElse("?");
    }

    /** Mob id path, used for the colour hash (there are no curated colours for mobs). */
    @Override
    public String materialName() {
        return entity.unwrapKey().map(k -> k.location().getPath()).orElse("");
    }

    @Override
    public Component displayName() {
        return entity.value().getDescription();
    }
}
