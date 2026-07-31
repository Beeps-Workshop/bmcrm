package com.beepsterr.resourcegenerators.block;

import com.beepsterr.resourcegenerators.inventory.MachineLayout;
import com.beepsterr.resourcegenerators.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Container menu for the Crystal Crucible: the crystal being melted, an empty container in, the filled
 * container out, and fuel. Synced data drives the melt progress, the flame, and the tank gauge.
 */
public class CrystalCrucibleMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOTS = 4;

    private final CrystalCrucibleBlockEntity blockEntity;
    private final ContainerData data;

    /** Client constructor — resolves the block entity from the synced position. */
    public CrystalCrucibleMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, resolve(playerInventory, buf.readBlockPos()), new SimpleContainerData(5));
    }

    /** Server constructor. */
    public CrystalCrucibleMenu(int containerId, Inventory playerInventory, CrystalCrucibleBlockEntity blockEntity, ContainerData data) {
        super(ModMenus.CRYSTAL_CRUCIBLE.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = data;

        // Left to right: crystal -> arrow -> tank -> containers, with the fuel slot in the shared
        // bottom-right spot every machine uses. Mirrored by CrystalCrucibleScreen, which draws them.
        var inv = blockEntity.getInventory();
        addSlot(new SlotItemHandler(inv, CrystalCrucibleBlockEntity.SLOT_CRYSTAL, 20, 17));
        addSlot(new SlotItemHandler(inv, CrystalCrucibleBlockEntity.SLOT_INPUT, 104, 17));
        addSlot(new SlotItemHandler(inv, CrystalCrucibleBlockEntity.SLOT_OUTPUT, 104, 52));
        addSlot(new SlotItemHandler(inv, CrystalCrucibleBlockEntity.SLOT_FUEL,
                MachineLayout.FUEL_SLOT_X, MachineLayout.FUEL_SLOT_Y));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        addDataSlots(data);
    }

    private static CrystalCrucibleBlockEntity resolve(Inventory playerInventory, BlockPos pos) {
        if (playerInventory.player.level().getBlockEntity(pos) instanceof CrystalCrucibleBlockEntity crucible) {
            return crucible;
        }
        throw new IllegalStateException("No CrystalCrucibleBlockEntity at " + pos);
    }

    /** Melt progress in pixels (0..maxPixels). */
    public int getScaledProgress(int maxPixels) {
        int progress = data.get(0);
        int maxProgress = data.get(1);
        return (maxProgress == 0 || progress == 0) ? 0 : progress * maxPixels / maxProgress;
    }

    /** Remaining flame height in pixels (0..maxPixels) for the current fuel item. */
    public int getScaledFlame(int maxPixels) {
        int litTime = data.get(2);
        int litDuration = data.get(3);
        return (litDuration == 0 || litTime == 0) ? 0 : litTime * maxPixels / litDuration;
    }

    /** Resonance currently in the tank, in mB. */
    public int getTankAmount() {
        return data.get(4);
    }

    public int getTankCapacity() {
        return CrystalCrucibleBlockEntity.TANK_CAPACITY;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int playerStart = MACHINE_SLOTS;
            int playerEnd = this.slots.size();
            if (index < MACHINE_SLOTS) {
                if (!moveItemStackTo(stack, playerStart, playerEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Player inventory -> fuel if it burns, else crystal/container (slot validity routes it).
                boolean moved = MachineFuel.isFuel(stack)
                        && moveItemStackTo(stack, CrystalCrucibleBlockEntity.SLOT_FUEL, CrystalCrucibleBlockEntity.SLOT_FUEL + 1, false);
                if (!moved && !moveItemStackTo(stack, CrystalCrucibleBlockEntity.SLOT_CRYSTAL, CrystalCrucibleBlockEntity.SLOT_OUTPUT, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity != null
                && !blockEntity.isRemoved()
                && player.distanceToSqr(blockEntity.getBlockPos().getCenter()) <= 64.0;
    }
}
