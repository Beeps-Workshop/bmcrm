package com.beepsterr.resourcegenerators.block;

import com.beepsterr.resourcegenerators.crystal.CrystalData;
import com.beepsterr.resourcegenerators.crystal.CrystalInfusion;
import com.beepsterr.resourcegenerators.crystal.CrystalInfusionMap;
import com.beepsterr.resourcegenerators.crystal.CrystalResource;
import com.beepsterr.resourcegenerators.registry.ModBlockEntities;
import com.beepsterr.resourcegenerators.registry.ModDataComponents;
import com.beepsterr.resourcegenerators.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Block entity for the Crystal Infuser. Two slots — material (0) and crystal (1) — and no output:
 * it feeds the material into the blank crystal in place, one unit every {@link #TICKS_PER_UNIT}
 * ticks, until the crystal's infusion reaches {@link #INFUSION_COST}, at which point its resource
 * is set and the fill state removed. The fill state lives on the crystal item (durability bar).
 */
public class CrystalInfuserBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOT_MATERIAL = 0;
    public static final int SLOT_CRYSTAL = 1;

    /** Units of material needed to fully infuse a crystal. */
    public static final int INFUSION_COST = 32;
    /** Ticks between consuming one unit of material. */
    private static final int TICKS_PER_UNIT = 10;

    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_MATERIAL -> !isCrystal(stack);      // materials only (route crystals to the other slot)
                case SLOT_CRYSTAL -> isBlankCrystal(stack);   // blank / still-infusing crystals only
                default -> false;
            };
        }
    };

    private int feedTimer = 0;

    public CrystalInfuserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRYSTAL_INFUSER.get(), pos, state);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    static boolean isCrystal(ItemStack stack) {
        return stack.is(ModItems.CRYSTAL.get());
    }

    static boolean isBlankCrystal(ItemStack stack) {
        if (!isCrystal(stack)) {
            return false;
        }
        CrystalData data = stack.get(ModDataComponents.CRYSTAL_DATA.get());
        return data != null && data.resource().isEmpty();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CrystalInfuserBlockEntity be) {
        ItemStack crystal = be.inventory.getStackInSlot(SLOT_CRYSTAL);
        ItemStack material = be.inventory.getStackInSlot(SLOT_MATERIAL);

        Optional<CrystalResource> target = be.currentTarget(level, crystal, material);
        if (target.isEmpty()) {
            if (be.feedTimer != 0) {
                be.feedTimer = 0;
                be.setChanged();
            }
            return;
        }

        be.feedTimer++;
        if (be.feedTimer >= TICKS_PER_UNIT) {
            be.feedTimer = 0;
            be.feedOneUnit(crystal, material, target.get());
        }
        be.setChanged();
    }

    /** The resource this pairing would produce, or empty if it can't currently infuse. */
    private Optional<CrystalResource> currentTarget(Level level, ItemStack crystal, ItemStack material) {
        if (!isBlankCrystal(crystal) || material.isEmpty()) {
            return Optional.empty();
        }
        Optional<CrystalResource> target = CrystalInfusionMap.resolve(level.registryAccess(), material);
        if (target.isEmpty()) {
            return Optional.empty();
        }
        // If already infusing, the material must match what it locked onto.
        CrystalInfusion inProgress = crystal.get(ModDataComponents.CRYSTAL_INFUSION.get());
        if (inProgress != null && !inProgress.target().equals(target.get())) {
            return Optional.empty();
        }
        return target;
    }

    private void feedOneUnit(ItemStack crystalRef, ItemStack material, CrystalResource target) {
        ItemStack crystal = crystalRef.copy();
        CrystalInfusion infusion = crystal.get(ModDataComponents.CRYSTAL_INFUSION.get());
        if (infusion == null) {
            infusion = new CrystalInfusion(target, 0, INFUSION_COST);
        }
        infusion = infusion.plus(1);

        if (infusion.isComplete()) {
            CrystalData data = crystal.get(ModDataComponents.CRYSTAL_DATA.get());
            crystal.set(ModDataComponents.CRYSTAL_DATA.get(), data.withResource(target));
            crystal.remove(ModDataComponents.CRYSTAL_INFUSION.get());
        } else {
            crystal.set(ModDataComponents.CRYSTAL_INFUSION.get(), infusion);
        }

        inventory.setStackInSlot(SLOT_CRYSTAL, crystal);
        inventory.extractItem(SLOT_MATERIAL, 1, false);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.putInt("FeedTimer", feedTimer);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        feedTimer = tag.getInt("FeedTimer");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.beepsresourcegenerators.crystal_infuser");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CrystalInfuserMenu(containerId, playerInventory, this);
    }
}
