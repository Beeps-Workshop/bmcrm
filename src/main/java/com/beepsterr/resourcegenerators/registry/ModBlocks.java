package com.beepsterr.resourcegenerators.registry;

import com.beepsterr.resourcegenerators.BeepsResourceGenerators;
import com.beepsterr.resourcegenerators.block.ResonatorBlock;
import com.beepsterr.resourcegenerators.block.CrystalFormerBlock;
import com.beepsterr.resourcegenerators.block.CrystalInfuserBlock;
import com.beepsterr.resourcegenerators.block.ModulatorBlock;
import com.beepsterr.resourcegenerators.block.PlacedCrystalBlock;
import com.beepsterr.resourcegenerators.crystal.Modulation;
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
                    BlockBehaviour.Properties.of().strength(3.5f));

    public static final DeferredBlock<CrystalInfuserBlock> CRYSTAL_INFUSER =
            REGISTER.registerBlock("crystal_infuser", CrystalInfuserBlock::new,
                    BlockBehaviour.Properties.of().strength(3.5f));

    public static final DeferredBlock<ResonatorBlock> RESONATOR =
            REGISTER.registerBlock("resonator", ResonatorBlock::new,
                    BlockBehaviour.Properties.of().strength(4.0f));

    /** Silk Touch modulator: projects Silk Touch over a flat 3x3x1 area (covered crystals yield ore blocks). */
    public static final DeferredBlock<ModulatorBlock> SILK_TOUCH_MODULATOR =
            REGISTER.registerBlock("silk_touch_modulator",
                    props -> new ModulatorBlock(Modulation.SILK_TOUCH, 1, 0, props),
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
