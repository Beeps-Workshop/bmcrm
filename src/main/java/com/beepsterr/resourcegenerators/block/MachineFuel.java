package com.beepsterr.resourcegenerators.block;

import com.beepsterr.resourcegenerators.Config;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Shared power supply for the crystal machines. A machine burns fuel exactly like a furnace: once an
 * item (or a lump of energy) is lit it burns down every tick — whether or not there's work — and a tick
 * only makes progress when it's both lit and has work. Fresh fuel is lit only when there's work to
 * justify it, so idling never <em>starts</em> a burn, but an already-lit fuel is spent regardless.
 *
 * <p>RF/FE is wireless fuel. A small buffer — exposed as a receive-only {@link IEnergyStorage} so an
 * adjacent cable "just works" — fills from cables and, once full, is spent to grant a fixed stretch of
 * burn time ({@code machineEnergyBurnTicks}), preferred over consuming a solid fuel item. The GUI shows
 * only the single furnace-style flame; there is no separate energy bar.
 */
public class MachineFuel implements IEnergyStorage {

    private int energy = 0;
    /** Ticks of solid-fuel burn left on the currently lit item (0 = nothing burning). */
    private int litTime = 0;
    /** Burn time the lit item started with — drives the flame gauge. */
    private int litDuration = 0;

    /** The vanilla furnace burn time of a stack (0 if it isn't fuel). */
    public static int burnDuration(ItemStack stack) {
        return stack.getBurnTime(RecipeType.SMELTING);
    }

    public static boolean isFuel(ItemStack stack) {
        return burnDuration(stack) > 0;
    }

    /**
     * Advance one tick, furnace-style. Any lit fuel burns down regardless of {@code workAvailable};
     * fresh fuel is only lit when there's work. Returns whether this tick can make progress (lit + work).
     * Must be called every tick.
     */
    public boolean tick(boolean workAvailable, ItemStackHandler inv, int fuelSlot) {
        // Active fuel burns down every tick — even with nothing to do, exactly like a furnace.
        if (litTime > 0) {
            litTime--;
        }
        // Light fresh fuel only when burnt out and there's work to justify it: a full energy buffer
        // (wireless fuel, preferred), otherwise a solid fuel item.
        if (litTime == 0 && workAvailable) {
            ignite(inv, fuelSlot);
        }
        return litTime > 0 && workAvailable;
    }

    private void ignite(ItemStackHandler inv, int fuelSlot) {
        int capacity = getMaxEnergyStored();
        if (capacity > 0 && energy >= capacity) {
            energy = 0;
            litDuration = Config.MACHINE_ENERGY_BURN_TICKS.get();
            litTime = litDuration;
            return;
        }
        ItemStack fuel = inv.getStackInSlot(fuelSlot);
        int burn = burnDuration(fuel);
        if (burn > 0) {
            ItemStack remainder = fuel.getCraftingRemainingItem();
            inv.extractItem(fuelSlot, 1, false);
            if (!remainder.isEmpty() && inv.getStackInSlot(fuelSlot).isEmpty()) {
                inv.setStackInSlot(fuelSlot, remainder); // e.g. lava bucket -> empty bucket
            }
            litDuration = burn;
            litTime = burn;
        }
    }

    public boolean isLit() {
        return litTime > 0;
    }

    /** 0..1 remaining burn of the lit fuel item, for the flame gauge. */
    public float litFraction() {
        return litDuration <= 0 ? 0f : (float) litTime / litDuration;
    }

    public int getLitTime() {
        return litTime;
    }

    public int getLitDuration() {
        return litDuration;
    }

    public void setLit(int litTime, int litDuration) {
        this.litTime = litTime;
        this.litDuration = litDuration;
    }

    public void save(CompoundTag tag) {
        tag.putInt("Energy", energy);
        tag.putInt("LitTime", litTime);
        tag.putInt("LitDuration", litDuration);
    }

    public void load(CompoundTag tag) {
        energy = tag.getInt("Energy");
        litTime = tag.getInt("LitTime");
        litDuration = tag.getInt("LitDuration");
    }

    // --- IEnergyStorage: receive-only. The machine spends the buffer internally via tick(). ---

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int capacity = getMaxEnergyStored();
        int limit = Config.MACHINE_ENERGY_MAX_RECEIVE.get();
        int accepted = Math.min(capacity - energy, limit > 0 ? Math.min(maxReceive, limit) : maxReceive);
        if (accepted > 0 && !simulate) {
            energy += accepted;
        }
        return Math.max(0, accepted);
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return energy;
    }

    @Override
    public int getMaxEnergyStored() {
        return Config.MACHINE_ENERGY_CAPACITY.get();
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return true;
    }
}
