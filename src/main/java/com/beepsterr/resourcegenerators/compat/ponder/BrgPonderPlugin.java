package com.beepsterr.resourcegenerators.compat.ponder;

import com.beepsterr.resourcegenerators.BeepsResourceGenerators;
import com.beepsterr.resourcegenerators.registry.ModBlocks;
import com.beepsterr.resourcegenerators.registry.ModItems;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.SharedTextRegistrationHelper;
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
    public void registerSharedText(SharedTextRegistrationHelper helper) {
        // Reusable label template so a per-tier "<Tier> (X% Chance)" line needs one lang key, not N.
        helper.registerSharedText("tier_label", "%s (%s%% Chance)");
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        // The Resonator's scene chain, in order. Schematic paths resolve to
        // assets/beepsresourcegenerators/ponder/<path>.nbt.
        ResourceLocation resonator = ModBlocks.RESONATOR.getId();
        helper.addStoryBoard(resonator, "resonator/basics", BrgPonderScenes::resonatorBase);
        helper.addStoryBoard(resonator, "resonator/basics", BrgPonderScenes::resonatorInterference);
        helper.addStoryBoard(resonator, "resonator/modulators", BrgPonderScenes::resonatorModulators);
        // Each Modulator's scene is appended to the Resonator chain AND shown on its own item.
        helper.addStoryBoard(resonator, "modulator/silk_touch", BrgPonderScenes::modulatorSilkTouch);
        helper.addStoryBoard(ModBlocks.SILK_TOUCH_MODULATOR.getId(), "modulator/silk_touch",
                BrgPonderScenes::modulatorSilkTouch);

        // Crystal-creation tutorial: one 2-page story shown on the crystal, the Former and the Infuser.
        helper.forComponents(
                        ModItems.CRYSTAL.getId(),
                        ModBlocks.CRYSTAL_FORMER.getId(),
                        ModBlocks.CRYSTAL_INFUSER.getId())
                .addStoryBoard("crystal/former_infuser", BrgPonderScenes::crystalBlank)
                .addStoryBoard("crystal/former_infuser", BrgPonderScenes::crystalInfusing);
    }
}
