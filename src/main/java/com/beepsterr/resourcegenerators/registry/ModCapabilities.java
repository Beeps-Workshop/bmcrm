package com.beepsterr.resourcegenerators.registry;

import com.beepsterr.resourcegenerators.block.CrystalFormerBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** Exposes block-entity capabilities (item handlers for automation). */
public final class ModCapabilities {

    private ModCapabilities() {}

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.CRYSTAL_FORMER.get(),
                (be, side) -> be.getInventoryForSide(side));
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.CRYSTAL_INFUSER.get(),
                (be, side) -> be.getInventoryForSide(side));
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.RESONATOR.get(),
                (be, side) -> be.getOutput());
    }
}
