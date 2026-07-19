package com.beepsterr.resourcegenerators.block;

import com.beepsterr.resourcegenerators.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * The resonator machine block. Holds an {@link ResonatorBlockEntity} that scans nearby placed
 * crystals and generates resources; right-click opens its output GUI.
 */
public class ResonatorBlock extends Block implements EntityBlock {

    public static final MapCodec<ResonatorBlock> CODEC = simpleCodec(ResonatorBlock::new);

    public ResonatorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends ResonatorBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ResonatorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.RESONATOR.get(), ResonatorBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof ResonatorBlockEntity resonator) {
                player.openMenu(resonator, buf -> buf.writeBlockPos(pos));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        // Re-evaluate crystal ownership (new overlaps / reclaims) when a resonator appears.
        if (!level.isClientSide && !oldState.is(state.getBlock())) {
            CrystalScanTracker.markDirty();
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof ResonatorBlockEntity resonator) {
                IItemHandler out = resonator.getOutput();
                for (int i = 0; i < out.getSlots(); i++) {
                    popResource(level, pos, out.getStackInSlot(i));
                }
            }
            // A removed resonator orphans its crystals; let others reclaim them promptly.
            if (!level.isClientSide) {
                CrystalScanTracker.markDirty();
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> type, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == type ? (BlockEntityTicker<A>) ticker : null;
    }
}
