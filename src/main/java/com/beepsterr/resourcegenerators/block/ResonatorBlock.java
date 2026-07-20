package com.beepsterr.resourcegenerators.block;

import com.beepsterr.resourcegenerators.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

/**
 * The resonator machine block. It stands two blocks tall — the antenna reaches into the space above,
 * so the block reserves that cell (you can't build into it). The {@code LOWER} half holds the
 * {@link ResonatorBlockEntity} and renders the whole model; the {@code UPPER} half is an empty
 * space-reserver that carries collision for the antenna. Right-click either half to open the GUI.
 */
public class ResonatorBlock extends Block implements EntityBlock {

    public static final MapCodec<ResonatorBlock> CODEC = simpleCodec(ResonatorBlock::new);
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    // Fitted silhouette (base slab + plate + tank + pillar for the lower half; pillar + spinner
    // sweep for the upper). Doubles as the collision shape.
    private static final VoxelShape LOWER_SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 2, 16),   // base slab
            Block.box(3, 2, 3, 13, 4, 13),   // infuser plate
            Block.box(4, 4, 4, 8, 13, 8),    // fuel tank
            Block.box(7, 4, 7, 9, 16, 9));   // central pillar (lower section)
    private static final VoxelShape UPPER_SHAPE = Shapes.or(
            Block.box(7, 0, 7, 9, 10, 9),    // central pillar (upper section, world y16-26)
            Block.box(3, 1, 3, 13, 7, 13));  // spinner sweep (world y17-23)

    public ResonatorBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected MapCodec<? extends ResonatorBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? LOWER_SHAPE : UPPER_SHAPE;
    }

    // --- Two-tall placement/survival (door/tall-plant pattern) ---

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        // Need an empty cell above for the antenna; refuse the placement otherwise.
        if (pos.getY() < level.getMaxBuildHeight() - 1
                && level.getBlockState(pos.above()).canBeReplaced(context)) {
            return defaultBlockState();
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockState below = level.getBlockState(pos.below());
            return below.is(this) && below.getValue(HALF) == DoubleBlockHalf.LOWER;
        }
        return true; // lower half floats fine (it's a machine, no ground requirement)
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        // Only the upper half depends on the lower: if its lower is gone, it breaks. The lower half is
        // self-sufficient (so a lone lower — e.g. a Ponder schematic or a pre-2-tall world — survives
        // and still renders/works). Breaking either half via a player is handled in playerWillDestroy.
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER && direction == Direction.DOWN
                && !(neighborState.is(this) && neighborState.getValue(HALF) == DoubleBlockHalf.LOWER)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // Breaking the upper half also removes the lower; suppress the lower's block loot (the upper's
        // break already yields the item) — its onRemove still drops the buffer contents. Breaking the
        // lower instead lets the upper break itself via updateShape.
        if (!level.isClientSide && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockPos belowPos = pos.below();
            BlockState below = level.getBlockState(belowPos);
            if (below.is(this) && below.getValue(HALF) == DoubleBlockHalf.LOWER) {
                level.setBlock(belowPos, Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_SUPPRESS_DROPS | Block.UPDATE_ALL);
                level.levelEvent(player, 2001, belowPos, Block.getId(below));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new ResonatorBlockEntity(pos, state) : null;
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
        if (level.isClientSide || state.getValue(HALF) != DoubleBlockHalf.LOWER) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.RESONATOR.get(), ResonatorBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            // Clicking either half opens the GUI; the block entity lives on the lower half.
            BlockPos lowerPos = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
            if (level.getBlockEntity(lowerPos) instanceof ResonatorBlockEntity resonator) {
                player.openMenu(resonator, buf -> buf.writeBlockPos(lowerPos));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        // Re-evaluate crystal ownership (new overlaps / reclaims) when a resonator appears.
        if (!level.isClientSide && !oldState.is(state.getBlock()) && state.getValue(HALF) == DoubleBlockHalf.LOWER) {
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
