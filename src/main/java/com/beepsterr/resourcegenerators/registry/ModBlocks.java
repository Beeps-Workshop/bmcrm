package com.beepsterr.resourcegenerators.registry;

import com.beepsterr.resourcegenerators.BeepsResourceGenerators;
import com.beepsterr.resourcegenerators.block.ResonatorBlock;
import com.beepsterr.resourcegenerators.block.CrystalFormerBlock;
import com.beepsterr.resourcegenerators.block.CrystalInfuserBlock;
import com.beepsterr.resourcegenerators.block.ModulatorBlock;
import com.beepsterr.resourcegenerators.block.PlacedCrystalBlock;
import com.beepsterr.resourcegenerators.crystal.AreaShape;
import com.beepsterr.resourcegenerators.crystal.Modulation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Block registrations. */
public final class ModBlocks {

    private ModBlocks() {}

    public static final DeferredRegister.Blocks REGISTER =
            DeferredRegister.createBlocks(BeepsResourceGenerators.MOD_ID);

    public static final DeferredBlock<CrystalFormerBlock> CRYSTAL_FORMER =
            REGISTER.registerBlock("crystal_former", CrystalFormerBlock::new,
                    // Non-cube model with glass panels: don't occlude neighbours or cull interior faces.
                    BlockBehaviour.Properties.of().strength(3.5f).noOcclusion());

    public static final DeferredBlock<CrystalInfuserBlock> CRYSTAL_INFUSER =
            REGISTER.registerBlock("crystal_infuser", CrystalInfuserBlock::new,
                    BlockBehaviour.Properties.of().strength(3.5f).noOcclusion());

    public static final DeferredBlock<ResonatorBlock> RESONATOR =
            REGISTER.registerBlock("resonator", ResonatorBlock::new,
                    BlockBehaviour.Properties.of().strength(4.0f));

    /** Shared crafting frame that each Modulator is built on top of. */
    public static final DeferredBlock<Block> MODULATOR_BASE =
            REGISTER.registerBlock("modulator_base", Block::new,
                    BlockBehaviour.Properties.of().strength(3.5f).sound(SoundType.METAL));

    /** Silk Touch modulator: a flat 3x3x1 box (covered crystals yield ore blocks). Does not stack. */
    public static final DeferredBlock<ModulatorBlock> SILK_TOUCH_MODULATOR =
            REGISTER.registerBlock("silk_touch_modulator",
                    props -> new ModulatorBlock(Modulation.SILK_TOUCH, AreaShape.BOX, 1, 0, props),
                    BlockBehaviour.Properties.of().strength(3.5f));

    /** Fortune modulator: a flat "+" (arm 2). Stacks; suppressed by Silk Touch on the same crystal. */
    public static final DeferredBlock<ModulatorBlock> FORTUNE_MODULATOR =
            REGISTER.registerBlock("fortune_modulator",
                    props -> new ModulatorBlock(Modulation.FORTUNE, AreaShape.PLUS, 2, 0, props),
                    BlockBehaviour.Properties.of().strength(3.5f));

    /** Auto-Smelt modulator: beam-based (no area) — smelts a crystal whose beam to the resonator crosses it. */
    public static final DeferredBlock<ModulatorBlock> AUTO_SMELT_MODULATOR =
            REGISTER.registerBlock("auto_smelt_modulator",
                    props -> new ModulatorBlock(Modulation.AUTO_SMELT, AreaShape.BOX, 0, 0, props),
                    BlockBehaviour.Properties.of().strength(3.5f));

    /** A crystal placed in the world (the crystal item places this). Non-solid, walk-through. */
    public static final DeferredBlock<PlacedCrystalBlock> PLACED_CRYSTAL =
            REGISTER.registerBlock("placed_crystal", PlacedCrystalBlock::new,
                    BlockBehaviour.Properties.of()
                            .strength(0.3f)
                            .noOcclusion()
                            .noCollission()
                            .sound(SoundType.AMETHYST)
                            .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY));
}
