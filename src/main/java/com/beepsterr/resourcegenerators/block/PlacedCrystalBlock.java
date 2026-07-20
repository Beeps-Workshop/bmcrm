package com.beepsterr.resourcegenerators.block;

import com.beepsterr.resourcegenerators.crystal.CrystalData;
import com.beepsterr.resourcegenerators.registry.ModDataComponents;
import com.beepsterr.resourcegenerators.registry.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A crystal placed in the world (the crystal item is a BlockItem for this). Attaches to whichever face
 * it's placed against and needs that block as support (see {@link FaceAttachedBlock}); solid, with the
 * collision matching its outline. The {@link PlacedCrystalBlockEntity} holds the crystal's tier/resource.
 */
public class PlacedCrystalBlock extends FaceAttachedBlock implements EntityBlock {

    public static final MapCodec<PlacedCrystalBlock> CODEC = simpleCodec(PlacedCrystalBlock::new);

    /** Crystal silhouette authored pointing UP; the base class rotates it to the real facing. */
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);

    public PlacedCrystalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends PlacedCrystalBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getUpShape(BlockState state) {
        return SHAPE;
    }

    /** Solid: collision matches the visible outline. */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlacedCrystalBlockEntity(pos, state);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && !oldState.is(state.getBlock())) {
            CrystalScanTracker.markDirty();
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            CrystalScanTracker.markDirty();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        CrystalData data = stack.get(ModDataComponents.CRYSTAL_DATA.get());
        if (data != null && level.getBlockEntity(pos) instanceof PlacedCrystalBlockEntity be) {
            be.setCrystalData(data);
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof PlacedCrystalBlockEntity be && be.getCrystalData() != null) {
            ItemStack stack = new ItemStack(ModItems.CRYSTAL.get());
            stack.set(ModDataComponents.CRYSTAL_DATA.get(), be.getCrystalData());
            return stack;
        }
        return super.getCloneItemStack(level, pos, state);
    }
}
