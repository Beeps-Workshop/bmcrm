package com.beepsterr.resourcegenerators.registry;

import com.beepsterr.resourcegenerators.BeepsResourceGenerators;
import com.beepsterr.resourcegenerators.crystal.CrystalTier;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

import java.util.Comparator;
import java.util.List;

/** Custom datapack-driven registries. */
@EventBusSubscriber(modid = BeepsResourceGenerators.MOD_ID)
public final class ModRegistries {

    private ModRegistries() {}

    /** Datapack registry of crystal tiers: {@code data/<ns>/bmcrm/crystal_tier/*.json}. */
    public static final ResourceKey<Registry<CrystalTier>> CRYSTAL_TIER_KEY =
            ResourceKey.createRegistryKey(BeepsResourceGenerators.rl("crystal_tier"));

    /** Registered on the mod event bus. Network codec is supplied so tiers sync to clients. */
    public static void onNewDataPackRegistry(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(CRYSTAL_TIER_KEY, CrystalTier.CODEC, CrystalTier.CODEC);
    }

    /**
     * Every usable tier, lowest level first. Entries whose JSON failed to parse are dropped —
     * they are kept in the registry only so that a bad file cannot stop a world from loading
     * (see {@link CrystalTier#broken()}), and must not reach the Former, JEI or the creative tab.
     *
     * <p>Empty if the registry is not available yet (early creative-tab builds, no level).
     */
    public static List<Holder.Reference<CrystalTier>> loadedTiers(HolderLookup.Provider registries) {
        return registries.lookup(CRYSTAL_TIER_KEY)
                .map(lookup -> lookup.listElements()
                        .filter(tier -> !tier.value().isBroken())
                        .sorted(Comparator.comparingInt(tier -> tier.value().level()))
                        .toList())
                .orElseGet(List::of);
    }

    /**
     * Names the entries that failed to parse once the server has them, so a pack author sees
     * which files to fix rather than only the decode errors logged while reading them.
     */
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        List<String> broken = event.getServer().registryAccess()
                .registryOrThrow(CRYSTAL_TIER_KEY)
                .holders()
                .filter(tier -> tier.value().isBroken())
                .map(tier -> tier.key().location().toString())
                .sorted()
                .toList();
        if (!broken.isEmpty()) {
            BeepsResourceGenerators.LOGGER.error(
                    "{} crystal tier(s) failed to load and are being ignored: {}",
                    broken.size(), String.join(", ", broken));
        }
    }
}
