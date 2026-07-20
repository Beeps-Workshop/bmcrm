package com.beepsterr.resourcegenerators.block;

import com.beepsterr.resourcegenerators.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

/**
 * The resonator machine block. A single (tall) block: its model reaches up into the space above, but
 * the block itself occupies one cell. Holds a {@link ResonatorBlockEntity} that scans nearby placed
 * crystals and generates resources; right-click opens its output GUI.
 */
public class ResonatorBlock extends Block implements EntityBlock {

    public static final MapCodec<ResonatorBlock> CODEC = simpleCodec(ResonatorBlock::new);

    // Fitted silhouette (base slab + plate + fuel tank + pillar); doubles as the collision shape.
    // The antenna above y16 is decorative — no collision up there.
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 2, 16),   // base slab
            Block.box(3, 2, 3, 13, 4, 13),   // infuser plate
            Block.box(4, 4, 4, 8, 13, 8),    // fuel tank
            Block.box(7, 4, 7, 9, 16, 9));   // central pillar

    public ResonatorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends ResonatorBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ResonatorBlockEntity(pos, state);
    }

    // Comparator output = how full the resource buffer is (standard container fullness, 0-15).
    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof ResonatorBlockEntity resonator) {
            return ItemHandlerHelper.calcRedstoneFromInventory(resonator.getOutput());
        }
        return 0;
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
