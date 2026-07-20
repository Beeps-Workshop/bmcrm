package com.beepsterr.resourcegenerators.block;

import com.beepsterr.resourcegenerators.crystal.AreaShape;
import com.beepsterr.resourcegenerators.crystal.Modulation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

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

    // Collision/outline fitted to each modulator's model.
    private static final VoxelShape SHAPE_SILK = Block.box(0, 0, 0, 16, 12, 16);
    private static final VoxelShape SHAPE_FORTUNE = Shapes.or(
            Block.box(0, 0, 0, 16, 2, 16),    // base plate
            Block.box(7, 2, 2, 9, 10, 14),    // "+" arm along Z
            Block.box(2, 2, 7, 14, 10, 9));   // "+" arm along X
    private static final VoxelShape SHAPE_PLATE = Block.box(0, 0, 0, 16, 2, 16); // auto-smelt: flat pad

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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (modulation) {
            case SILK_TOUCH -> SHAPE_SILK;
            case FORTUNE -> SHAPE_FORTUNE;
            case AUTO_SMELT -> SHAPE_PLATE;
        };
    }

    /** The Auto-Smelt modulator is a hot plate: stepping onto it burns (unless sneaking / fire-immune). */
    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (modulation == Modulation.AUTO_SMELT && !entity.isSteppingCarefully() && !entity.fireImmune()) {
            entity.hurt(level.damageSources().hotFloor(), 1.0F);
            entity.igniteForSeconds(3.0F);
        }
        super.stepOn(level, pos, state, entity);
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
