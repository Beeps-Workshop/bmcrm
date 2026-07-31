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
                .description("Forming blank crystals, infusing them with a resource, and melting them down")
                .register();
        helper.addToTag(CREATING_CRYSTALS)
                .add(ModItems.CRYSTAL.getId())
                .add(ModItems.CRYSTAL_FORMER.getId())
                .add(ModItems.CRYSTAL_INFUSER.getId())
                .add(ModItems.CRYSTAL_CRUCIBLE.getId())
                .add(ModItems.LIQUID_RESONANCE_BUCKET.getId());

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
        // Reusable label template so a per-tier line needs one lang key, not N. Covers both stats a
        // tier decides: how often it generates, and how much resonance it can bank before saturating.
        helper.registerSharedText("tier_label", "%s (%s%% Chance, holds %s mB)");
        // Every fuelled machine closes on the same beat, so the line lives in one place. The
        // Resonator is deliberately not one of them — it needs no power at all.
        helper.registerSharedText("machine_power",
                "The %s requires energy to operate. It can be provided with fuel, or supplied via RF");
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        // The Resonator's scene chain, in order. Schematic paths resolve to
        // assets/bmcrm/ponder/<path>.nbt.
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

        // Each machine owns one scene, on its own 5x5x5 build.
        helper.addStoryBoard(ModBlocks.CRYSTAL_FORMER.getId(), "machine/crystal_former",
                BrgPonderScenes::machineFormer).highlightAllTags();
        helper.addStoryBoard(ModBlocks.CRYSTAL_INFUSER.getId(), "machine/crystal_infuser",
                BrgPonderScenes::machineInfuser).highlightAllTags();
        helper.addStoryBoard(ModBlocks.CRYSTAL_CRUCIBLE.getId(), "machine/crystal_crucible",
                BrgPonderScenes::machineCrucible).highlightAllTags();

        // The crystal item keeps the overview of its own life — the tier table and what infusing
        // means — since that spans machines rather than belonging to any one of them.
        helper.forComponents(ModItems.CRYSTAL.getId())
                .addStoryBoard("crystal/former_infuser", BrgPonderScenes::crystalBlank,
                        e -> e.highlightAllTags())
                .addStoryBoard("crystal/former_infuser", BrgPonderScenes::crystalInfusing,
                        e -> e.highlightAllTags());
    }
}
