package com.beepsterr.resourcegenerators.block;

import com.beepsterr.resourcegenerators.crystal.AreaShape;
import com.beepsterr.resourcegenerators.crystal.Modulation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Modulator block: a resonator scans for these in range, and each projects its {@link Modulation}
 * over a box ({@link #horizontalRadius()} on X/Z, {@link #verticalRadius()} on Y) centered on itself,
 * affecting the subset of crystals inside. The modulation and radii are fixed per registered block.
 * Its BE implements {@link AreaPreview} so the Tuning Fork shows the affected area.
 */
public class ModulatorBlock extends Block implements EntityBlock {

    public static final MapCodec<ModulatorBlock> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Modulation.CODEC.fieldOf("modulation").forGetter(ModulatorBlock::modulation),
            AreaShape.CODEC.fieldOf("shape").forGetter(ModulatorBlock::shape),
            Codec.intRange(0, 16).fieldOf("horizontal_radius").forGetter(ModulatorBlock::horizontalRadius),
            Codec.intRange(0, 16).fieldOf("vertical_radius").forGetter(ModulatorBlock::verticalRadius),
            propertiesCodec()
    ).apply(inst, ModulatorBlock::new));

    private final Modulation modulation;
    private final AreaShape shape;
    private final int horizontalRadius;
    private final int verticalRadius;

    public ModulatorBlock(Modulation modulation, AreaShape shape, int horizontalRadius, int verticalRadius,
                          Properties properties) {
        super(properties);
        this.modulation = modulation;
        this.shape = shape;
        this.horizontalRadius = horizontalRadius;
        this.verticalRadius = verticalRadius;
    }

    public Modulation modulation() {
        return modulation;
    }

    public AreaShape shape() {
        return shape;
    }

    /** Reach on the X/Z axes (radius 1 -> 3 wide). */
    public int horizontalRadius() {
        return horizontalRadius;
    }

    /** Reach on the Y axis (radius 0 -> 1 tall, flat). */
    public int verticalRadius() {
        return verticalRadius;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ModulatorBlockEntity(pos, state);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && !oldState.is(state.getBlock())) {
            CrystalScanTracker.markDirty(); // a new modulator changes what resonators apply
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            CrystalScanTracker.markDirty();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
