package com.beepsterr.resourcegenerators.compat.ponder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.ParticleEmitter;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Ponder storyboards for the Resonator. The starting layouts are authored in-world and loaded from
 * {@code assets/beepsresourcegenerators/ponder/resonator/*.nbt}; the crystals carry their
 * {@code CrystalData}, so they render with their real resource tints. Blocks live in the schematic
 * and are revealed over time with {@code showSection}.
 */
public final class BrgPonderScenes {

    private BrgPonderScenes() {}

    // --- resonator_base.nbt layout ---
    private static final int[] BASE_RESONATOR = {7, 1, 10};
    private static final int[][] BASE_CRYSTALS = {{5, 1, 7}, {7, 1, 7}, {9, 1, 7}};

    // --- resonator_multiple.nbt layout ---
    private static final int[] MULTI_RESONATOR_A = {7, 1, 10};
    private static final int[] MULTI_RESONATOR_B = {7, 1, 4};
    private static final int[][] MULTI_CRYSTALS = {{5, 1, 7}, {7, 1, 7}, {9, 1, 7}};
    private static final int[] MULTI_CONTESTED = {7, 1, 7};

    private static BlockPos pos(SceneBuildingUtil util, int[] p) {
        return util.grid().at(p[0], p[1], p[2]);
    }

    /** Scene 1: a Resonator surrounded by crystals, generating resources. */
    public static void resonatorBase(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("resonator_base", "Generating resources with the Resonator");
        scene.configureBasePlate(0, 0, 15);
        scene.scaleSceneView(0.6f);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().layer(0), Direction.UP);
        scene.idle(15);

        BlockPos resonator = pos(util, BASE_RESONATOR);
        scene.world().showSection(util.select().position(resonator), Direction.DOWN);
        scene.idle(5);
        scene.overlay().showText(70)
                .attachKeyFrame()
                .text("The Resonator generates resources all on its own")
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(resonator, Direction.WEST));
        scene.idle(80);

        for (int[] c : BASE_CRYSTALS) {
            scene.world().showSection(util.select().position(pos(util, c)), Direction.DOWN);
            scene.idle(4);
        }
        scene.idle(10);
        scene.overlay().showText(80)
                .attachKeyFrame()
                .text("Surround it with crystals — each one adds its resource to the pool")
                .placeNearTarget()
                .pointAt(util.vector().topOf(pos(util, BASE_CRYSTALS[0])));
        scene.idle(90);

        ParticleEmitter spark = scene.effects()
                .simpleParticleEmitter(ParticleTypes.ENCHANT, util.vector().of(0, 0.2, 0));
        scene.effects().emitParticles(util.vector().topOf(resonator), spark, 5, 40);
        scene.idle(20);
        scene.overlay().showText(90)
                .attachKeyFrame()
                .text("Every so often it rolls a crystal and outputs that resource")
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(resonator, Direction.EAST));
        scene.idle(100);

        scene.markAsFinished();
    }

    /** Scene 2: a crystal can only serve one Resonator; one caught between two breaks. */
    public static void resonatorMultiple(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("resonator_multiple", "One crystal, one Resonator");
        scene.configureBasePlate(0, 0, 15);
        scene.scaleSceneView(0.6f);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().layer(0), Direction.UP);
        scene.idle(15);

        // First Resonator and its crystals.
        BlockPos resonatorA = pos(util, MULTI_RESONATOR_A);
        scene.world().showSection(util.select().position(resonatorA), Direction.DOWN);
        scene.idle(5);
        for (int[] c : MULTI_CRYSTALS) {
            scene.world().showSection(util.select().position(pos(util, c)), Direction.DOWN);
            scene.idle(4);
        }
        scene.idle(5);
        scene.overlay().showText(80)
                .attachKeyFrame()
                .text("A crystal is claimed by the first Resonator that resonates it")
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(resonatorA, Direction.EAST));
        scene.idle(90);

        // A second Resonator reaches the same crystals.
        BlockPos resonatorB = pos(util, MULTI_RESONATOR_B);
        scene.world().showSection(util.select().position(resonatorB), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(80)
                .attachKeyFrame()
                .text("A second Resonator must not share an already-claimed crystal")
                .colored(PonderPalette.RED)
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(resonatorB, Direction.EAST));
        scene.idle(90);

        // The contested crystal breaks.
        BlockPos contested = pos(util, MULTI_CONTESTED);
        Selection contestedSel = util.select().position(contested);
        scene.overlay().showOutline(PonderPalette.RED, "contested", contestedSel, 40);
        scene.idle(45);
        scene.world().destroyBlock(contested);
        scene.idle(10);
        scene.overlay().showText(90)
                .attachKeyFrame()
                .text("So a crystal caught between two Resonators simply breaks and drops")
                .placeNearTarget()
                .pointAt(util.vector().topOf(contested));
        scene.idle(100);

        scene.markAsFinished();
    }

    /** Scene 3: a Silk Touch Modulator makes covered crystals yield the ore block. */
    public static void modulatorSilkTouch(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("modulator_silk_touch", "Modulator: Silk Touch");
        scene.configureBasePlate(0, 0, 15);
        scene.scaleSceneView(0.6f);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().layer(0), Direction.UP);
        scene.idle(15);

        // Resonator plus two crystal patches.
        BlockPos resonator = util.grid().at(7, 1, 7);
        scene.world().showSection(util.select().position(resonator), Direction.DOWN);
        scene.idle(5);
        Selection leftPatch = util.select().fromTo(3, 1, 6, 5, 1, 8);
        Selection rightCrystals = util.select().fromTo(9, 1, 6, 11, 1, 8)
                .substract(util.select().position(util.grid().at(10, 1, 7)));
        scene.world().showSection(leftPatch, Direction.DOWN);
        scene.world().showSection(rightCrystals, Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(80)
                .attachKeyFrame()
                .text("Crystals around a Resonator yield raw ore")
                .placeNearTarget()
                .pointAt(util.vector().topOf(util.grid().at(4, 1, 7)));
        scene.overlay().showControls(util.vector().centerOf(util.grid().at(4, 2, 7)), Pointing.DOWN, 80)
                .withItem(new ItemStack(Items.RAW_COPPER));
        scene.idle(90);

        // The Modulator drops in at the centre of the right patch.
        BlockPos modulator = util.grid().at(10, 1, 7);
        scene.world().showSection(util.select().position(modulator), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(80)
                .attachKeyFrame()
                .text("A Modulator changes how nearby crystals behave")
                .placeNearTarget()
                .pointAt(util.vector().topOf(modulator));
        scene.idle(90);

        // Highlight its flat 3x3 footprint.
        Selection area = util.select().fromTo(9, 1, 6, 11, 1, 8);
        scene.overlay().showOutline(PonderPalette.BLUE, "silk_area", area, 100);
        scene.overlay().showText(90)
                .attachKeyFrame()
                .text("Silk Touch: crystals in its area yield the ore block itself")
                .placeNearTarget()
                .pointAt(util.vector().topOf(util.grid().at(10, 1, 8)));
        scene.overlay().showControls(util.vector().centerOf(util.grid().at(10, 2, 7)), Pointing.DOWN, 90)
                .withItem(new ItemStack(Items.COPPER_ORE));
        scene.idle(100);

        scene.markAsFinished();
    }
}
