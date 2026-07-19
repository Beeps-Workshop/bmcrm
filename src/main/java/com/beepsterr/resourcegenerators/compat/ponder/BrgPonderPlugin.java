package com.beepsterr.resourcegenerators.compat.ponder;

import com.beepsterr.resourcegenerators.BeepsResourceGenerators;
import com.beepsterr.resourcegenerators.registry.ModBlocks;
import com.beepsterr.resourcegenerators.registry.ModItems;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.createmod.ponder.api.registration.SharedTextRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

/**
 * Registers our in-world Ponder scenes. This class references Ponder API types directly, so it is
 * only ever class-loaded when Ponder is present — see {@link PonderCompat}, which is invoked behind
 * a {@code ModList.isLoaded("ponder")} guard. Ponder is a soft dependency: not bundled, not declared
 * in neoforge.mods.toml.
 */
public class BrgPonderPlugin implements PonderPlugin {

    /** Two browsable Ponder categories for the crystal workflow. */
    private static final ResourceLocation CREATING_CRYSTALS = BeepsResourceGenerators.rl("creating_crystals");
    private static final ResourceLocation USING_CRYSTALS = BeepsResourceGenerators.rl("using_crystals");

    @Override
    public String getModId() {
        return BeepsResourceGenerators.MOD_ID;
    }

    @Override
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        // item(x, true, true) sets both the list icon AND the overview's main item (the header uses it).
        helper.registerTag(CREATING_CRYSTALS)
                .addToIndex()
                .item(ModItems.CRYSTAL_FORMER.get(), true, true)
                .title("Creating Crystals")
                .description("Forming blank crystals and infusing them with a resource")
                .register();
        helper.addToTag(CREATING_CRYSTALS)
                .add(ModItems.CRYSTAL.getId())
                .add(ModItems.CRYSTAL_FORMER.getId())
                .add(ModItems.CRYSTAL_INFUSER.getId());

        helper.registerTag(USING_CRYSTALS)
                .addToIndex()
                .item(ModItems.RESONATOR.get(), true, true)
                .title("Using Crystals")
                .description("Resonators, and the Modulators that shape what they produce")
                .register();
        helper.addToTag(USING_CRYSTALS)
                .add(ModItems.RESONATOR.getId())
                .add(ModItems.CRYSTAL.getId())
                .add(ModItems.SILK_TOUCH_MODULATOR.getId())
                .add(ModItems.FORTUNE_MODULATOR.getId())
                .add(ModItems.AUTO_SMELT_MODULATOR.getId());
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
        helper.addStoryBoard(resonator, "resonator/basics", BrgPonderScenes::resonatorBase).highlightAllTags();
        helper.addStoryBoard(resonator, "resonator/basics", BrgPonderScenes::resonatorInterference).highlightAllTags();
        helper.addStoryBoard(resonator, "resonator/modulators", BrgPonderScenes::resonatorModulators).highlightAllTags();
        // Each Modulator's scene is appended to the Resonator chain AND shown on its own item.
        helper.addStoryBoard(resonator, "modulator/silk_touch", BrgPonderScenes::modulatorSilkTouch).highlightAllTags();
        helper.addStoryBoard(ModBlocks.SILK_TOUCH_MODULATOR.getId(), "modulator/silk_touch",
                BrgPonderScenes::modulatorSilkTouch).highlightAllTags();
        helper.addStoryBoard(resonator, "modulator/fortune", BrgPonderScenes::modulatorFortune).highlightAllTags();
        helper.addStoryBoard(ModBlocks.FORTUNE_MODULATOR.getId(), "modulator/fortune",
                BrgPonderScenes::modulatorFortune).highlightAllTags();
        helper.addStoryBoard(resonator, "modulator/auto_smelt", BrgPonderScenes::modulatorAutoSmelt).highlightAllTags();
        helper.addStoryBoard(ModBlocks.AUTO_SMELT_MODULATOR.getId(), "modulator/auto_smelt",
                BrgPonderScenes::modulatorAutoSmelt).highlightAllTags();

        // Crystal-creation tutorial (2 pages), but each machine opens on its own step.
        // Crystal item + Former: forming first, then infusing.
        helper.forComponents(ModItems.CRYSTAL.getId(), ModBlocks.CRYSTAL_FORMER.getId())
                .addStoryBoard("crystal/former_infuser", BrgPonderScenes::crystalBlank,
                        e -> e.highlightAllTags())
                .addStoryBoard("crystal/former_infuser", BrgPonderScenes::crystalInfusing,
                        e -> e.highlightAllTags());
        // Infuser: infusing first, then forming.
        ResourceLocation infuser = ModBlocks.CRYSTAL_INFUSER.getId();
        helper.addStoryBoard(infuser, "crystal/former_infuser", BrgPonderScenes::crystalInfusing).highlightAllTags();
        helper.addStoryBoard(infuser, "crystal/former_infuser", BrgPonderScenes::crystalBlank).highlightAllTags();
    }
}
