package com.beepsterr.resourcegenerators.block;

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
 * Container menu for the resonator: a 3x5 read-only output grid (players extract, can't insert)
 * plus the player inventory.
 */
public class ResonatorMenu extends AbstractContainerMenu {

    private static final int COLS = 5;
    private static final int ROWS = 3;
    private static final int OUTPUT_SLOTS = COLS * ROWS;

    private final ResonatorBlockEntity blockEntity;
    private final ContainerData data;

    public ResonatorMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, resolve(playerInventory, buf.readBlockPos()), new SimpleContainerData(2));
    }

    public ResonatorMenu(int containerId, Inventory playerInventory, ResonatorBlockEntity blockEntity, ContainerData data) {
        super(ModMenus.RESONATOR.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = data;

        var out = blockEntity.getOutput();
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                addSlot(new SlotItemHandler(out, col + row * COLS, 44 + col * 18, 18 + row * 18));
            }
        }

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

    /** Work-bar fill in pixels (0..maxPixels). */
    public int getScaledProgress(int maxPixels) {
        int progress = data.get(0);
        int max = data.get(1);
        return (max == 0) ? 0 : progress * maxPixels / max;
    }

    private static ResonatorBlockEntity resolve(Inventory playerInventory, BlockPos pos) {
        if (playerInventory.player.level().getBlockEntity(pos) instanceof ResonatorBlockEntity resonator) {
            return resonator;
        }
        throw new IllegalStateException("No ResonatorBlockEntity at " + pos);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < OUTPUT_SLOTS) {
                // Output -> player inventory.
                if (!moveItemStackTo(stack, OUTPUT_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Player inventory slots don't feed the (output-only) machine; just move within inventory.
                return ItemStack.EMPTY;
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
