package com.beepsterr.resourcegenerators.registry;

import com.beepsterr.resourcegenerators.BeepsResourceGenerators;
import com.beepsterr.resourcegenerators.crystal.CrystalTier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/** Custom datapack-driven registries. */
public final class ModRegistries {

    private ModRegistries() {}

    /** Datapack registry of crystal tiers: {@code data/<ns>/crystal_tier/*.json}. */
    public static final ResourceKey<Registry<CrystalTier>> CRYSTAL_TIER_KEY =
            ResourceKey.createRegistryKey(BeepsResourceGenerators.rl("crystal_tier"));

    /** Registered on the mod event bus. Network codec is supplied so tiers sync to clients. */
    public static void onNewDataPackRegistry(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(CRYSTAL_TIER_KEY, CrystalTier.CODEC, CrystalTier.CODEC);
    }
}
