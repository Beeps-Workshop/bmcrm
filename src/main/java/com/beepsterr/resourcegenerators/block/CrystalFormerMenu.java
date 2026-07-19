package com.beepsterr.resourcegenerators.block;

import com.beepsterr.resourcegenerators.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Container menu for the Crystal Former: base + catalyst inputs, an output slot, and a synced
 * progress value for the GUI arrow.
 */
public class CrystalFormerMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOTS = 3;

    private final CrystalFormerBlockEntity blockEntity;
    private final ContainerData data;

    /** Client constructor — resolves the block entity from the synced position. */
    public CrystalFormerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, resolve(playerInventory, buf.readBlockPos()), new SimpleContainerData(2));
    }

    /** Server constructor. */
    public CrystalFormerMenu(int containerId, Inventory playerInventory, CrystalFormerBlockEntity blockEntity, ContainerData data) {
        super(ModMenus.CRYSTAL_FORMER.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = data;

        // Triangle: two inputs side by side up top -> arrow -> centered output below.
        // Add order stays base/catalyst/output so menu slot indices match inventory indices.
        var inv = blockEntity.getInventory();
        addSlot(new SlotItemHandler(inv, CrystalFormerBlockEntity.SLOT_BASE, 61, 17));      // top-left
        addSlot(new SlotItemHandler(inv, CrystalFormerBlockEntity.SLOT_CATALYST, 99, 17));  // top-right
        addSlot(new SlotItemHandler(inv, CrystalFormerBlockEntity.SLOT_OUTPUT, 80, 53));    // bottom-center

        // Player inventory (3 rows) + hotbar.
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

    private static CrystalFormerBlockEntity resolve(Inventory playerInventory, BlockPos pos) {
        if (playerInventory.player.level().getBlockEntity(pos) instanceof CrystalFormerBlockEntity former) {
            return former;
        }
        throw new IllegalStateException("No CrystalFormerBlockEntity at " + pos);
    }

    /** Progress arrow width in pixels (0..maxPixels). */
    public int getScaledProgress(int maxPixels) {
        int progress = data.get(0);
        int maxProgress = data.get(1);
        return (maxProgress == 0 || progress == 0) ? 0 : progress * maxPixels / maxProgress;
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
                // Machine -> player inventory.
                if (!moveItemStackTo(stack, playerStart, playerEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Player inventory -> the two input slots only.
                if (!moveItemStackTo(stack, CrystalFormerBlockEntity.SLOT_BASE, CrystalFormerBlockEntity.SLOT_CATALYST + 1, false)) {
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
