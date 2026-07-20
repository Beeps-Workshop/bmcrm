package com.beepsterr.resourcegenerators.compat.jade;

import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Jade integration entry point. Jade is a soft dependency: this class references Jade API types, so
 * it is only ever class-loaded by Jade's own {@code @WailaPlugin} scanner — when Jade is absent,
 * nothing here loads. Not bundled, not declared in neoforge.mods.toml.
 */
@WailaPlugin
public class BrgJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        // No server-side data needed: a placed crystal's CrystalData is already synced to the client
        // (see PlacedCrystalBlockEntity#getUpdateTag), so the tooltip reads it directly.
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(CrystalTooltipProvider.INSTANCE,
                com.beepsterr.resourcegenerators.block.PlacedCrystalBlock.class);
    }
}
