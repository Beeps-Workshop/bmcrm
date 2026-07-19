package com.beepsterr.resourcegenerators.block;

import com.beepsterr.resourcegenerators.crystal.CrystalTier;
import com.beepsterr.resourcegenerators.inventory.SidedItemHandler;
import com.beepsterr.resourcegenerators.item.CrystalItem;
import com.beepsterr.resourcegenerators.registry.ModBlockEntities;
import com.beepsterr.resourcegenerators.registry.ModRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Block entity for the Crystal Former. Three slots — base (0), catalyst (1), output (2) — and
 * a progress timer. Each tick it looks for a tier whose base+catalyst ingredients match the
 * input slots, and once progress completes, consumes one of each and outputs a blank crystal
 * of that tier.
 */
public class CrystalFormerBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOT_BASE = 0;
    public static final int SLOT_CATALYST = 1;
    public static final int SLOT_OUTPUT = 2;

    private static final int DEFAULT_MAX_PROGRESS = 100;

    private final ItemStackHandler inventory = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            // Output slot is code-filled only; players/hoppers may not insert into it.
            return slot != SLOT_OUTPUT;
        }
    };

    private int progress = 0;
    private int maxProgress = DEFAULT_MAX_PROGRESS;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? progress : maxProgress;
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                progress = value;
            } else {
                maxProgress = value;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public CrystalFormerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRYSTAL_FORMER.get(), pos, state);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    // Cached per-face views: base in from the top, catalyst in from the sides, output out the bottom.
    private SidedItemHandler topInsert;    // base only
    private SidedItemHandler sideInsert;   // catalyst only
    private SidedItemHandler bottomOutput; // output only

    /** The automation view for a given face; null side (internal access) sees the full inventory. */
    @Nullable
    public IItemHandler getInventoryForSide(@Nullable Direction side) {
        if (side == null) {
            return inventory;
        }
        return switch (side) {
            case UP -> topInsert != null ? topInsert
                    : (topInsert = new SidedItemHandler(inventory,
                    new boolean[]{true, false, false}, new boolean[]{false, false, false}, null));
            case DOWN -> bottomOutput != null ? bottomOutput
                    : (bottomOutput = new SidedItemHandler(inventory,
                    new boolean[]{false, false, false}, new boolean[]{false, false, true}, null));
            default -> sideInsert != null ? sideInsert
                    : (sideInsert = new SidedItemHandler(inventory,
                    new boolean[]{false, true, false}, new boolean[]{false, false, false}, null));
        };
    }

    public ContainerData getContainerData() {
        return data;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CrystalFormerBlockEntity be) {
        Holder<CrystalTier> match = be.findMatchingTier(level);
        ItemStack result = match == null ? ItemStack.EMPTY : CrystalItem.createBlank(match);

        if (match == null || !be.canOutput(result)) {
            if (be.progress != 0) {
                be.progress = 0;
                be.setChanged();
            }
            return;
        }

        be.progress++;
        if (be.progress >= be.maxProgress) {
            be.inventory.extractItem(SLOT_BASE, 1, false);
            be.inventory.extractItem(SLOT_CATALYST, 1, false);
            be.pushOutput(result);
            be.progress = 0;
        }
        be.setChanged();
    }

    /** First tier whose base + catalyst ingredients both match the input slots, else null. */
    @Nullable
    private Holder<CrystalTier> findMatchingTier(Level level) {
        ItemStack base = inventory.getStackInSlot(SLOT_BASE);
        ItemStack catalyst = inventory.getStackInSlot(SLOT_CATALYST);
        if (base.isEmpty() || catalyst.isEmpty()) {
            return null;
        }
        var registry = level.registryAccess().registryOrThrow(ModRegistries.CRYSTAL_TIER_KEY);
        for (Holder.Reference<CrystalTier> tier : registry.holders().toList()) {
            CrystalTier value = tier.value();
            if (value.base().test(base) && value.catalyst().test(catalyst)) {
                return tier;
            }
        }
        return null;
    }

    private boolean canOutput(ItemStack result) {
        ItemStack out = inventory.getStackInSlot(SLOT_OUTPUT);
        if (out.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(out, result)
                && out.getCount() + result.getCount() <= out.getMaxStackSize();
    }

    private void pushOutput(ItemStack result) {
        ItemStack out = inventory.getStackInSlot(SLOT_OUTPUT);
        if (out.isEmpty()) {
            inventory.setStackInSlot(SLOT_OUTPUT, result.copy());
        } else {
            out.grow(result.getCount());
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.putInt("Progress", progress);
        tag.putInt("MaxProgress", maxProgress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        progress = tag.getInt("Progress");
        maxProgress = tag.contains("MaxProgress") ? tag.getInt("MaxProgress") : DEFAULT_MAX_PROGRESS;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.beepsresourcegenerators.crystal_former");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CrystalFormerMenu(containerId, playerInventory, this, data);
    }
}
