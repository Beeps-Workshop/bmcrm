package com.beepsterr.resourcegenerators.block;

import com.beepsterr.resourcegenerators.crystal.Modulation;
import com.beepsterr.resourcegenerators.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * Block entity for a {@link ModulatorBlock}. Holds no dynamic state — the modulation and radii come
 * from the block — but exists so the resonator can gather modulators and the Tuning Fork can draw
 * the affected area via {@link AreaPreview}.
 */
public class ModulatorBlockEntity extends BlockEntity implements AreaPreview {

    public ModulatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MODULATOR.get(), pos, state);
    }

    @Nullable
    public Modulation getModulation() {
        return getBlockState().getBlock() instanceof ModulatorBlock block ? block.modulation() : null;
    }

    public int getHorizontalRadius() {
        return getBlockState().getBlock() instanceof ModulatorBlock block ? block.horizontalRadius() : 0;
    }

    public int getVerticalRadius() {
        return getBlockState().getBlock() instanceof ModulatorBlock block ? block.verticalRadius() : 0;
    }

    @Override
    public AABB getPreviewArea() {
        int h = getHorizontalRadius();
        int v = getVerticalRadius();
        BlockPos p = worldPosition;
        return new AABB(
                p.getX() - h, p.getY() - v, p.getZ() - h,
                p.getX() + h + 1, p.getY() + v + 1, p.getZ() + h + 1);
    }

    @Override
    public int getPreviewColor() {
        Modulation modulation = getModulation();
        return modulation != null ? modulation.color() : 0xFFFFFF;
    }
}
