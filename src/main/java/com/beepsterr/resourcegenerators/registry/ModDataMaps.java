package com.beepsterr.resourcegenerators.registry;

import com.beepsterr.resourcegenerators.BeepsResourceGenerators;
import com.beepsterr.resourcegenerators.crystal.CrystalResource;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * Data maps — datapack-driven overrides that let packs/mods correct or fully customize behavior
 * when the standard {@code c:} tags are missing or produce odd results.
 */
public final class ModDataMaps {

    private ModDataMaps() {}

    /**
     * Maps an item to the resource its infusion should produce, overriding tag inference.
     * Defined in {@code data/<ns>/data_maps/item/infusion_override.json}; supports item ids and
     * item tags as keys.
     */
    public static final DataMapType<Item, CrystalResource> INFUSION_OVERRIDE =
            DataMapType.builder(
                    BeepsResourceGenerators.rl("infusion_override"),
                    Registries.ITEM,
                    CrystalResource.CODEC
            ).build();

    public static void register(RegisterDataMapTypesEvent event) {
        event.register(INFUSION_OVERRIDE);
    }
}
