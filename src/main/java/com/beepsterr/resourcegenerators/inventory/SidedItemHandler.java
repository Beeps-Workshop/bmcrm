package com.beepsterr.resourcegenerators.inventory;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * A per-face view over another {@link IItemHandler} that restricts which slots may be inserted into
 * or extracted from, with an optional predicate gating extraction (e.g. "only pull a finished
 * crystal"). All slots stay visible for reads; only the insert/extract operations are gated.
 */
public final class SidedItemHandler implements IItemHandler {

    /** Gate on extraction beyond the slot mask — receives the slot and its current contents. */
    @FunctionalInterface
    public interface ExtractFilter {
        boolean canExtract(int slot, ItemStack current);
    }

    private final IItemHandler base;
    private final boolean[] insertable;
    private final boolean[] extractable;
    @Nullable
    private final ExtractFilter extractFilter;

    public SidedItemHandler(IItemHandler base, boolean[] insertable, boolean[] extractable,
                            @Nullable ExtractFilter extractFilter) {
        this.base = base;
        this.insertable = insertable;
        this.extractable = extractable;
        this.extractFilter = extractFilter;
    }

    @Override
    public int getSlots() {
        return base.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return base.getStackInSlot(slot);
    }

    @Override
    public int getSlotLimit(int slot) {
        return base.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return insertable[slot] && base.isItemValid(slot, stack);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!insertable[slot]) {
            return stack; // face rejects insertion into this slot
        }
        return base.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (!extractable[slot]) {
            return ItemStack.EMPTY;
        }
        if (extractFilter != null && !extractFilter.canExtract(slot, base.getStackInSlot(slot))) {
            return ItemStack.EMPTY;
        }
        return base.extractItem(slot, amount, simulate);
    }
}
