package com.beepsterr.resourcegenerators.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The shared crafting frame each Modulator is built on: a flat 2px pad that attaches to a face like
 * the modulators do (see {@link FaceAttachedBlock}).
 */
public class ModulatorBaseBlock extends FaceAttachedBlock {

    public static final MapCodec<ModulatorBaseBlock> CODEC = simpleCodec(ModulatorBaseBlock::new);

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 2, 16);

    public ModulatorBaseBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends ModulatorBaseBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getUpShape(BlockState state) {
        return SHAPE;
    }
}
