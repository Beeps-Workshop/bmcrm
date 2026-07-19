package com.beepsterr.resourcegenerators.compat.ponder;

import com.beepsterr.resourcegenerators.BeepsResourceGenerators;
import com.beepsterr.resourcegenerators.registry.ModBlocks;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

/**
 * Registers our in-world Ponder scenes. This class references Ponder API types directly, so it is
 * only ever class-loaded when Ponder is present — see {@link PonderCompat}, which is invoked behind
 * a {@code ModList.isLoaded("ponder")} guard. Ponder is a soft dependency: not bundled, not declared
 * in neoforge.mods.toml.
 */
public class BrgPonderPlugin implements PonderPlugin {

    @Override
    public String getModId() {
        return BeepsResourceGenerators.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        // Both scenes attach to the Resonator item; Ponder shows them as consecutive pages. The
        // schematic path resolves to assets/beepsresourcegenerators/ponder/<path>.nbt.
        ResourceLocation resonator = ModBlocks.RESONATOR.getId();
        helper.addStoryBoard(resonator, "resonator/base", BrgPonderScenes::resonatorBase);
        helper.addStoryBoard(resonator, "resonator/multiple", BrgPonderScenes::resonatorMultiple);
    }
}
