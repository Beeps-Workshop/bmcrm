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
 * Container menu for the Crystal Infuser: a material slot, a crystal slot, and a fuel slot (no output
 * — the crystal fills in place, shown by its durability bar). Synced data drives the flame + RF gauges.
 */
public class CrystalInfuserMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOTS = 3;

    private final CrystalInfuserBlockEntity blockEntity;
    private final ContainerData data;

    /** Client constructor. */
    public CrystalInfuserMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, resolve(playerInventory, buf.readBlockPos()), new SimpleContainerData(2));
    }

    /** Server constructor. */
    public CrystalInfuserMenu(int containerId, Inventory playerInventory, CrystalInfuserBlockEntity blockEntity, ContainerData data) {
        super(ModMenus.CRYSTAL_INFUSER.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = data;

        // Layout authored in the GUI editor: input up top -> (infusion bubbles) -> output below,
        // with the fuel slot bottom-right. Add order matches inventory indices.
        var inv = blockEntity.getInventory();
        addSlot(new SlotItemHandler(inv, CrystalInfuserBlockEntity.SLOT_MATERIAL, 75, 18));   // input
        addSlot(new SlotItemHandler(inv, CrystalInfuserBlockEntity.SLOT_CRYSTAL, 75, 60));    // output
        addSlot(new SlotItemHandler(inv, CrystalInfuserBlockEntity.SLOT_FUEL, 140, 53));      // fuel, bottom-right

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

    private static CrystalInfuserBlockEntity resolve(Inventory playerInventory, BlockPos pos) {
        if (playerInventory.player.level().getBlockEntity(pos) instanceof CrystalInfuserBlockEntity infuser) {
            return infuser;
        }
        throw new IllegalStateException("No CrystalInfuserBlockEntity at " + pos);
    }

    /** Remaining flame height in pixels (0..maxPixels) for the current fuel item. */
    public int getScaledFlame(int maxPixels) {
        int litTime = data.get(0);
        int litDuration = data.get(1);
        return (litDuration == 0 || litTime == 0) ? 0 : litTime * maxPixels / litDuration;
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
                // Player inventory -> fuel slot if it burns, else material/crystal (slot validity routes those).
                boolean moved = MachineFuel.isFuel(stack)
                        && moveItemStackTo(stack, CrystalInfuserBlockEntity.SLOT_FUEL, CrystalInfuserBlockEntity.SLOT_FUEL + 1, false);
                if (!moved && !moveItemStackTo(stack, CrystalInfuserBlockEntity.SLOT_MATERIAL, CrystalInfuserBlockEntity.SLOT_FUEL, false)) {
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
