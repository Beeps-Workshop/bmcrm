package com.beepsterr.resourcegenerators.compat.ponder;

import com.beepsterr.resourcegenerators.crystal.CrystalResource;
import com.beepsterr.resourcegenerators.crystal.CrystalTier;
import com.beepsterr.resourcegenerators.crystal.OreTagResource;
import com.beepsterr.resourcegenerators.item.CrystalItem;
import com.beepsterr.resourcegenerators.registry.ModItems;
import com.beepsterr.resourcegenerators.registry.ModRegistries;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.ParticleEmitter;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Comparator;
import java.util.List;

/**
 * Ponder storyboards for the Resonator. The starting layouts are authored in-world and loaded from
 * {@code assets/bmcrm/ponder/resonator/*.nbt}; the crystals carry their
 * {@code CrystalData}, so they render with their real resource tints. Blocks live in the schematic
 * and are revealed over time with {@code showSection}.
 */
public final class BrgPonderScenes {

    private BrgPonderScenes() {}

    // --- resonator/basics.nbt layout (Scenes 1 & 2) ---
    private static final int[] BASICS_RESONATOR_A = {5, 1, 7};
    private static final int[] BASICS_RESONATOR_B = {9, 1, 7};
    private static final int[] BASICS_CRYSTAL = {7, 1, 7};

    // --- resonator/modulators.nbt layout (Scene 3) ---
    private static final int[] MOD_RESONATOR = {7, 1, 9};
    private static final int[] MOD_MODULATOR = {7, 1, 5};

    private static BlockPos pos(SceneBuildingUtil util, int[] p) {
        return util.grid().at(p[0], p[1], p[2]);
    }

    /** Scene 1: how the Resonator works — interaction, dissonance, multiple crystals, the Tuning Fork. */
    public static void resonatorBase(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("resonator_base", "The Resonator");
        scene.configureBasePlate(0, 0, 15);
        scene.scaleSceneView(0.6f);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().layer(0), Direction.UP);
        scene.idle(10);

        BlockPos resonatorA = pos(util, BASICS_RESONATOR_A);
        BlockPos crystal = pos(util, BASICS_CRYSTAL);
        scene.world().showSection(util.select().position(resonatorA), Direction.DOWN);
        scene.world().showSection(util.select().position(crystal), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(70)
                .attachKeyFrame()
                .text("The Resonator interacts with nearby crystals")
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(resonatorA, Direction.EAST));
        scene.idle(60);

        // The crystal resonates: a spark on it, and its resource surfaces at the Resonator.
        ParticleEmitter spark = scene.effects()
                .simpleParticleEmitter(ParticleTypes.ENCHANT, util.vector().of(0, 0.1, 0));
        scene.effects().emitParticles(util.vector().topOf(crystal), spark, 4, 30);
        scene.overlay().showControls(util.vector().topOf(resonatorA), Pointing.DOWN, 40)
                .withItem(new ItemStack(Items.RAW_COPPER));
        scene.idle(50);

        scene.overlay().showText(70)
                .attachKeyFrame()
                .text("A single crystal can only correctly resonate with one Resonator")
                .placeNearTarget()
                .pointAt(util.vector().topOf(crystal));
        scene.idle(80);

        // A second Resonator causes dissonance -> the shared crystal breaks.
        BlockPos resonatorB = pos(util, BASICS_RESONATOR_B);
        scene.world().showSection(util.select().position(resonatorB), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(80)
                .attachKeyFrame()
                .text("Adding a second Resonator nearby creates dissonance")
                .colored(PonderPalette.RED)
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(resonatorB, Direction.WEST));
        scene.idle(50);
        scene.overlay().showOutline(PonderPalette.RED, "dissonant_crystal", util.select().position(crystal), 40);
        scene.idle(45);
        scene.world().destroyBlock(crystal);
        scene.idle(20);
        scene.world().hideSection(util.select().position(resonatorB), Direction.UP);
        scene.idle(20);

        // Multiple crystals for one Resonator.
        scene.overlay().showText(70)
                .attachKeyFrame()
                .text("Multiple crystals can be used within the same Resonator")
                .pointAt(util.vector().topOf(util.grid().at(7, 1, 4)));
        scene.world().showSection(util.select().fromTo(6, 1, 4, 8, 1, 4), Direction.DOWN);
        scene.idle(80);

        // The Tuning Fork reveals the Resonator's range (5 out/up, 2 down from (5,1,7)).
        Selection range = util.select().fromTo(0, -1, 2, 10, 6, 12);
        scene.overlay().showOutline(PonderPalette.BLUE, "resonator_range", range, 90);
        scene.overlay().showControls(util.vector().topOf(resonatorA), Pointing.DOWN, 60)
                .rightClick().withItem(new ItemStack(ModItems.TUNING_FORK.get()));
        scene.overlay().showText(80)
                .attachKeyFrame()
                .text("A Tuning Fork can be used to show the Resonator's range")
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(resonatorA, Direction.EAST));
        scene.idle(90);

        scene.markAsFinished();
    }

    /** Scene 2: interference — rain and nearby mobs reduce the Resonator's success. */
    public static void resonatorInterference(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("resonator_interference", "Interference");
        scene.configureBasePlate(0, 0, 15);
        scene.scaleSceneView(0.6f);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().layer(0), Direction.UP);
        scene.idle(10);

        BlockPos resonatorA = pos(util, BASICS_RESONATOR_A);
        BlockPos crystal = pos(util, BASICS_CRYSTAL);
        scene.world().showSection(util.select().position(resonatorA), Direction.DOWN);
        scene.world().showSection(util.select().position(crystal), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(70)
                .attachKeyFrame()
                .text("A Resonator is vulnerable to interference")
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(resonatorA, Direction.EAST));
        scene.idle(80);

        // Fake rain: splash water from above the Resonator so it rains onto it.
        ParticleEmitter rain = scene.effects()
                .simpleParticleEmitter(ParticleTypes.SPLASH, util.vector().of(0, -0.25, 0));
        scene.effects().emitParticles(util.vector().of(5.5, 2.7, 7.5), rain, 6, 80);
        scene.overlay().showText(80)
                .attachKeyFrame()
                .text("For example, rain near its operating area will reduce the chance of success")
                .placeNearTarget()
                .pointAt(util.vector().topOf(resonatorA));
        scene.idle(90);

        // A zombie wanders in — a noisy neighbor.
        var zombie = scene.world().createEntity(level -> {
            Zombie z = new Zombie(EntityType.ZOMBIE, level);
            z.setPos(8.5, 1.0, 8.5);
            z.setYBodyRot(210.0f);
            z.setYHeadRot(210.0f);
            return z;
        });
        scene.idle(10);
        scene.overlay().showText(70)
                .attachKeyFrame()
                .text("Noisy neighbors can also interfere")
                .pointAt(util.vector().of(8.5, 1.9, 8.5));
        scene.idle(80);

        // Deal with the neighbor.
        scene.overlay().showControls(util.vector().of(8.5, 2.3, 8.5), Pointing.DOWN, 40)
                .withItem(new ItemStack(Items.DIAMOND_SWORD));
        scene.idle(45);
        scene.world().modifyEntity(zombie, net.minecraft.world.entity.Entity::discard);
        scene.idle(10);
        scene.overlay().showText(80)
                .attachKeyFrame()
                .text("It's best to keep your Resonator free from distractions")
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(resonatorA, Direction.EAST));
        scene.idle(90);

        scene.markAsFinished();
    }

    /** Scene 3: introduces Modulators (leads into the per-modulator scenes that follow). */
    public static void resonatorModulators(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("resonator_modulators", "Modulators");
        scene.configureBasePlate(0, 0, 15);
        scene.scaleSceneView(0.6f);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().layer(0), Direction.UP);
        scene.idle(10);

        BlockPos resonator = pos(util, MOD_RESONATOR);
        BlockPos modulator = pos(util, MOD_MODULATOR);
        Selection crystals = util.select().fromTo(6, 1, 4, 8, 1, 6)
                .substract(util.select().position(modulator));
        scene.world().showSection(util.select().position(resonator), Direction.DOWN);
        scene.world().showSection(crystals, Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(80)
                .attachKeyFrame()
                .text("Modulators are special blocks that modify how the Resonator interacts with nearby crystals")
                .pointAt(util.vector().topOf(util.grid().at(7, 1, 4)));
        scene.idle(90);

        scene.world().showSection(util.select().position(modulator), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(70)
                .attachKeyFrame()
                .text("They can be placed as part of your crystal arrangement")
                .placeNearTarget()
                .pointAt(util.vector().topOf(modulator));
        scene.idle(80);

        // Tuning Fork shows the Modulator's area (silk touch: flat 3x3x1 around (7,1,5)).
        scene.overlay().showOutline(PonderPalette.BLUE, "modulator_area",
                util.select().fromTo(6, 1, 4, 8, 1, 6), 100);
        scene.overlay().showControls(util.vector().topOf(modulator), Pointing.DOWN, 60)
                .rightClick().withItem(new ItemStack(ModItems.TUNING_FORK.get()));
        scene.overlay().showText(90)
                .attachKeyFrame()
                .text("Some Modulators have an area of effect, and others affect the entire system")
                .placeNearTarget()
                .pointAt(util.vector().topOf(modulator));
        scene.idle(100);

        scene.markAsFinished();
    }

    /** Modulator detail scene: Silk Touch turns the surrounding crystals' output into ore blocks. */
    public static void modulatorSilkTouch(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("modulator_silk_touch", "Modulator: Silk Touch");
        scene.configureBasePlate(0, 0, 15);
        scene.scaleSceneView(0.6f);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().layer(0), Direction.UP);
        scene.idle(10);
        scene.world().showSection(util.select().layer(1), Direction.DOWN); // everything: resonator, crystals, modulator
        scene.idle(15);

        BlockPos resonator = util.grid().at(7, 1, 7);
        scene.overlay().showText(80)
                .attachKeyFrame()
                .text("The Silk Touch Modulator allows you to obtain ore blocks instead of raw ore")
                .pointAt(util.vector().topOf(util.grid().at(10, 1, 7)));
        scene.idle(90);

        // Crystals WITHOUT a modulator -> raw ore comes out of the Resonator.
        scene.overlay().showOutline(PonderPalette.WHITE, "plain_patch",
                util.select().fromTo(3, 1, 6, 5, 1, 8), 70);
        scene.overlay().showControls(util.vector().topOf(resonator), Pointing.DOWN, 70)
                .withItem(new ItemStack(Items.RAW_COPPER));
        scene.idle(80);

        // Crystals WITHIN the modulator's area -> the ore block comes out instead.
        scene.overlay().showOutline(PonderPalette.BLUE, "modulated_patch",
                util.select().fromTo(9, 1, 6, 11, 1, 8), 70);
        scene.overlay().showControls(util.vector().topOf(resonator), Pointing.DOWN, 70)
                .withItem(new ItemStack(Items.COPPER_ORE));
        scene.idle(80);

        // Its range: the crystals directly surrounding it.
        scene.overlay().showText(80)
                .attachKeyFrame()
                .text("The Silk Touch Modulator affects all crystals directly surrounding it")
                .placeNearTarget()
                .pointAt(util.vector().topOf(util.grid().at(10, 1, 7)));
        scene.idle(40);
        scene.overlay().showOutline(PonderPalette.BLUE, "modulator_range",
                util.select().fromTo(9, 1, 6, 11, 1, 8), 90);
        scene.idle(100);

        scene.markAsFinished();
    }

    /** Modulator detail scene: Fortune adds ore drops over a "+", stacks, and yields to Silk Touch. */
    public static void modulatorFortune(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("modulator_fortune", "Modulator: Fortune");
        scene.configureBasePlate(0, 0, 15);
        scene.scaleSceneView(0.6f);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().layer(0), Direction.UP);
        scene.idle(10);

        // Resonator + the crystal row (excluding the modulator slot at (7,1,5)).
        BlockPos resonator = util.grid().at(7, 1, 9);
        scene.world().showSection(util.select().position(resonator), Direction.DOWN);
        Selection crystals = util.select().fromTo(5, 1, 5, 9, 1, 5)
                .substract(util.select().position(util.grid().at(7, 1, 5)));
        scene.world().showSection(crystals, Direction.DOWN);
        scene.idle(10);

        // The middle Fortune modulator appears.
        BlockPos fortune = util.grid().at(7, 1, 5);
        scene.world().showSection(util.select().position(fortune), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(90)
                .attachKeyFrame()
                .text("Fortune Modulators apply the Fortune enchantment on the crystal whenever it generates resources")
                .placeNearTarget()
                .pointAt(util.vector().topOf(fortune));
        scene.idle(100);

        // Its "+" footprint (arm 2 = a 5x5 plus): two crossing arms.
        scene.overlay().showText(80)
                .attachKeyFrame()
                .text("The Fortune Modulator operates on a 5x5 plus")
                .placeNearTarget()
                .pointAt(util.vector().topOf(fortune));
        scene.overlay().showOutline(PonderPalette.GREEN, "fortune_x", util.select().fromTo(5, 1, 5, 9, 1, 5), 90);
        scene.overlay().showOutline(PonderPalette.GREEN, "fortune_z", util.select().fromTo(7, 1, 3, 7, 1, 7), 90);
        scene.idle(100);

        // Two more Fortune modulators -> stacking.
        scene.world().showSection(util.select().position(util.grid().at(4, 1, 5)), Direction.DOWN);
        scene.world().showSection(util.select().position(util.grid().at(10, 1, 5)), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(80)
                .attachKeyFrame()
                .text("Multiple Fortune Modulators can affect one crystal")
                .placeNearTarget()
                .pointAt(util.vector().topOf(util.grid().at(5, 1, 5)));
        scene.idle(90);

        // The Silk Touch modulator suppresses Fortune on the crystals it covers.
        BlockPos silk = util.grid().at(9, 1, 4);
        scene.world().showSection(util.select().position(silk), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(90)
                .attachKeyFrame()
                .text("A Silk Touch Modulator disables the effects of any Fortune Modulators")
                .placeNearTarget()
                .pointAt(util.vector().topOf(silk));
        scene.idle(50);
        scene.overlay().showControls(util.vector().topOf(util.grid().at(8, 1, 5)), Pointing.DOWN, 60)
                .withItem(new ItemStack(Items.BARRIER));
        scene.overlay().showControls(util.vector().topOf(util.grid().at(9, 1, 5)), Pointing.DOWN, 60)
                .withItem(new ItemStack(Items.BARRIER));
        scene.idle(80);

        scene.markAsFinished();
    }

    /** Modulator detail scene: Auto-Smelt smelts crystals whose beam to the resonator crosses it. */
    public static void modulatorAutoSmelt(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("modulator_auto_smelt", "Modulator: Auto-Smelt");
        scene.configureBasePlate(0, 0, 15);
        scene.scaleSceneView(0.6f);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().layer(0), Direction.UP);
        scene.idle(10);

        // Resonator + the crystal cluster close to the smelter (not the lone crystal or modulator yet).
        BlockPos resonator = util.grid().at(9, 1, 7);
        scene.world().showSection(util.select().position(resonator), Direction.DOWN);
        scene.world().showSection(util.select().fromTo(4, 1, 6, 5, 1, 8), Direction.DOWN);
        scene.idle(10);

        // The smelter appears between the cluster and the resonator.
        BlockPos smelter = util.grid().at(7, 1, 7);
        scene.world().showSection(util.select().position(smelter), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(90)
                .attachKeyFrame()
                .text("The Auto-Smelt Modulator affects any crystals that sit between it and the Resonator")
                .placeNearTarget()
                .pointAt(util.vector().topOf(smelter));
        scene.idle(100);

        // One affected crystal would produce raw iron...
        BlockPos crystal = util.grid().at(5, 1, 7);
        scene.overlay().showOutline(PonderPalette.GREEN, "affected", util.select().position(crystal), 100);
        scene.overlay().showControls(util.vector().topOf(crystal), Pointing.DOWN, 60)
                .withItem(new ItemStack(Items.RAW_IRON));
        scene.idle(70);
        // ...but the smelter turns it into an ingot...
        scene.overlay().showOutline(PonderPalette.OUTPUT, "smelter", util.select().position(smelter), 100);
        scene.overlay().showControls(util.vector().topOf(smelter), Pointing.DOWN, 60)
                .withItem(new ItemStack(Items.IRON_INGOT));
        scene.idle(70);
        // ...which is what comes out of the resonator.
        scene.overlay().showControls(util.vector().topOf(resonator), Pointing.DOWN, 70)
                .withItem(new ItemStack(Items.IRON_INGOT));
        scene.idle(80);

        // A lone crystal whose beam does not cross the smelter.
        BlockPos lone = util.grid().at(7, 1, 4);
        scene.world().showSection(util.select().position(lone), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showOutline(PonderPalette.GREEN, "lone", util.select().position(lone), 90);
        scene.overlay().showControls(util.vector().topOf(lone), Pointing.DOWN, 60)
                .withItem(new ItemStack(Items.RAW_IRON));
        scene.overlay().showText(90)
                .attachKeyFrame()
                .text("Any crystal that does not intersect an Auto-Smelt Modulator does not get affected")
                .placeNearTarget()
                .pointAt(util.vector().topOf(lone));
        scene.idle(100);
        // So it stays raw at the resonator.
        scene.overlay().showControls(util.vector().topOf(resonator), Pointing.DOWN, 70)
                .withItem(new ItemStack(Items.RAW_IRON));
        scene.idle(80);

        scene.markAsFinished();
    }

    // --- Crystal creation tutorial (shared by the crystal item, the Former and the Infuser) ---
    // Structure: crystal/former_infuser.nbt — Infuser (5,1,7), a blank crystal (7,1,7), Former (9,1,7).

    /** Tutorial 1: forming blank crystals. */
    public static void crystalBlank(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("crystal_blank", "Blank Crystals");
        scene.configureBasePlate(0, 0, 15);
        scene.scaleSceneView(0.6f);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().layer(0), Direction.UP);
        scene.idle(10);
        scene.world().showSection(util.select().layer(1), Direction.DOWN);
        scene.idle(15);

        BlockPos former = util.grid().at(9, 1, 7);
        scene.overlay().showControls(util.vector().topOf(former), Pointing.DOWN, 60).rightClick();
        scene.overlay().showText(70)
                .attachKeyFrame()
                .text("Blank Crystals can be created using the Crystal Former")
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(former, Direction.WEST));
        scene.idle(80);

        // Data-driven list of every tier: blank crystals spread along Z (a receding line on the right),
        // each with its own "<Tier> (X% Chance)" label beside it.
        List<Holder.Reference<CrystalTier>> tiers = tiers();
        scene.overlay().showText(60)
                .attachKeyFrame()
                .text("There are %s different types of Blank Crystal you can form", tiers.size())
                .independent(30);
        scene.idle(50);
        for (int i = 0; i < tiers.size(); i++) {
            Holder.Reference<CrystalTier> tier = tiers.get(i);
            double z = 8.0 + (i - (tiers.size() - 1) / 2.0) * 1.8; // spread along Z
            var at = util.vector().of(11.5, 1.4, z);
            scene.world().createItemEntity(at, util.vector().of(0, 0, 0), CrystalItem.createBlank(tier));
            int percent = Math.round(tier.value().rollChance() * 100);
            scene.overlay().showText(90)
                    .sharedText("tier_label", CrystalItem.tierName(tier).getString(), percent)
                    .placeNearTarget()
                    .pointAt(at);
            scene.idle(10);
        }
        scene.idle(80);

        scene.overlay().showText(80)
                .attachKeyFrame()
                .text("The blank crystal determines how likely the crystal is to resonate")
                .placeNearTarget()
                .pointAt(util.vector().topOf(util.grid().at(7, 1, 7)));
        scene.idle(90);

        scene.markAsFinished();
    }

    /** Tutorial 2: infusing a blank crystal with a material. */
    public static void crystalInfusing(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("crystal_infusing", "Infusing Crystals");
        scene.configureBasePlate(0, 0, 15);
        scene.scaleSceneView(0.6f);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().layer(0), Direction.UP);
        scene.idle(10);
        scene.world().showSection(util.select().layer(1), Direction.DOWN);
        scene.idle(15);

        BlockPos infuser = util.grid().at(5, 1, 7);
        List<Holder.Reference<CrystalTier>> tiers = tiers();
        scene.overlay().showControls(util.vector().topOf(infuser), Pointing.DOWN, 60).rightClick();
        scene.overlay().showText(80)
                .attachKeyFrame()
                .text("A Blank crystal doesn't do anything on its own, it needs to be infused")
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(infuser, Direction.EAST));
        scene.idle(90);

        // The inputs: copper ingots and a blank crystal go into the Infuser.
        scene.overlay().showControls(util.vector().of(4.7, 2.2, 7.5), Pointing.DOWN, 70)
                .withItem(new ItemStack(Items.COPPER_INGOT));
        if (!tiers.isEmpty()) {
            scene.overlay().showControls(util.vector().of(5.3, 2.2, 7.5), Pointing.DOWN, 70)
                    .withItem(CrystalItem.createBlank(tiers.get(0)));
        }
        scene.idle(80);

        // "Many materials" — show a spread of infused crystals (base tier, common ores).
        scene.overlay().showText(70)
                .attachKeyFrame()
                .text("Many materials can be infused into crystals")
                .pointAt(util.vector().of(7.5, 1.6, 9.5));
        if (!tiers.isEmpty()) {
            String[] materials = {"iron", "copper", "gold", "diamond"};
            for (int i = 0; i < materials.length; i++) {
                double x = 7.5 + (i - (materials.length - 1) / 2.0) * 1.3;
                CrystalResource resource = new OreTagResource(oresTag(materials[i]));
                scene.world().createItemEntity(util.vector().of(x, 1.4, 9.5), util.vector().of(0, 0, 0),
                        CrystalItem.create(tiers.get(0), resource));
            }
        }
        scene.idle(80);

        scene.overlay().showText(90)
                .attachKeyFrame()
                .text("You cannot infuse a crystal with multiple materials")
                .colored(PonderPalette.RED)
                .placeNearTarget()
                .pointAt(util.vector().topOf(infuser));
        scene.idle(100);

        scene.markAsFinished();
    }

    /**
     * Every player-facing crystal tier, sorted by level, from the client's registry (empty if
     * unavailable). Tiers flagged {@code hidden} (creative/testing-only) are excluded from the count
     * and from every scene that lists tiers.
     */
    private static List<Holder.Reference<CrystalTier>> tiers() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return List.of();
        }
        return mc.level.registryAccess().registryOrThrow(ModRegistries.CRYSTAL_TIER_KEY).holders()
                .filter(h -> !h.value().hidden())
                .sorted(Comparator.comparingInt(h -> h.value().level()))
                .toList();
    }

    private static TagKey<net.minecraft.world.level.block.Block> oresTag(String material) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "ores/" + material));
    }
}
