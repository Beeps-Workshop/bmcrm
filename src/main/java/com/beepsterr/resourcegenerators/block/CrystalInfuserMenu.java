package com.beepsterr.resourcegenerators.block;

import com.beepsterr.resourcegenerators.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Container menu for the Crystal Infuser: a material slot and a crystal slot (no output — the
 * crystal fills in place, shown by its durability bar).
 */
public class CrystalInfuserMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOTS = 2;

    private final CrystalInfuserBlockEntity blockEntity;

    /** Client constructor. */
    public CrystalInfuserMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, resolve(playerInventory, buf.readBlockPos()));
    }

    /** Server constructor. */
    public CrystalInfuserMenu(int containerId, Inventory playerInventory, CrystalInfuserBlockEntity blockEntity) {
        super(ModMenus.CRYSTAL_INFUSER.get(), containerId);
        this.blockEntity = blockEntity;

        var inv = blockEntity.getInventory();
        addSlot(new SlotItemHandler(inv, CrystalInfuserBlockEntity.SLOT_MATERIAL, 44, 35));
        addSlot(new SlotItemHandler(inv, CrystalInfuserBlockEntity.SLOT_CRYSTAL, 116, 35));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    private static CrystalInfuserBlockEntity resolve(Inventory playerInventory, BlockPos pos) {
        if (playerInventory.player.level().getBlockEntity(pos) instanceof CrystalInfuserBlockEntity infuser) {
            return infuser;
        }
        throw new IllegalStateException("No CrystalInfuserBlockEntity at " + pos);
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
                // Player inventory -> machine slots (slot validity routes crystals vs materials).
                if (!moveItemStackTo(stack, 0, MACHINE_SLOTS, false)) {
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
