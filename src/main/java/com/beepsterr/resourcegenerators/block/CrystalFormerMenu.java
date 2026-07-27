package com.beepsterr.resourcegenerators.block;

import com.beepsterr.resourcegenerators.inventory.MachineLayout;
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

    private static final int MACHINE_SLOTS = 4;

    private final CrystalFormerBlockEntity blockEntity;
    private final ContainerData data;

    /** Client constructor — resolves the block entity from the synced position. */
    public CrystalFormerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, resolve(playerInventory, buf.readBlockPos()), new SimpleContainerData(4));
    }

    /** Server constructor. */
    public CrystalFormerMenu(int containerId, Inventory playerInventory, CrystalFormerBlockEntity blockEntity, ContainerData data) {
        super(ModMenus.CRYSTAL_FORMER.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = data;

        // Layout authored in the GUI editor: two inputs up top, big vertical arrow (fills top to
        // bottom) down to the output, fuel bottom-right. Add order matches inventory indices.
        var inv = blockEntity.getInventory();
        addSlot(new SlotItemHandler(inv, CrystalFormerBlockEntity.SLOT_BASE, 56, 17));      // input1
        addSlot(new SlotItemHandler(inv, CrystalFormerBlockEntity.SLOT_CATALYST, 92, 17));  // input2
        addSlot(new SlotItemHandler(inv, CrystalFormerBlockEntity.SLOT_OUTPUT, 92, 52));    // output
        addSlot(new SlotItemHandler(inv, CrystalFormerBlockEntity.SLOT_FUEL,
                MachineLayout.FUEL_SLOT_X, MachineLayout.FUEL_SLOT_Y));   // fuel, shared spot

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

    /** Remaining flame height in pixels (0..maxPixels) for the current fuel item. */
    public int getScaledFlame(int maxPixels) {
        int litTime = data.get(2);
        int litDuration = data.get(3);
        return (litDuration == 0 || litTime == 0) ? 0 : litTime * maxPixels / litDuration;
    }

    /** Packed 0xRRGGBB colour of the tier being formed (for tinting the vessel fill), or -1 if none. */
    public int getFormingColor() {
        return blockEntity != null ? blockEntity.getFormingColor() : -1;
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
                // Player inventory -> fuel slot if it burns, otherwise the two input slots.
                boolean moved = MachineFuel.isFuel(stack)
                        && moveItemStackTo(stack, CrystalFormerBlockEntity.SLOT_FUEL, CrystalFormerBlockEntity.SLOT_FUEL + 1, false);
                if (!moved && !moveItemStackTo(stack, CrystalFormerBlockEntity.SLOT_BASE, CrystalFormerBlockEntity.SLOT_CATALYST + 1, false)) {
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
