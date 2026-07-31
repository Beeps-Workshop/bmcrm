package com.beepsterr.resourcegenerators.client;

import com.beepsterr.resourcegenerators.block.CrystalCrucibleMenu;
import com.beepsterr.resourcegenerators.client.gui.MachineGauges;
import com.beepsterr.resourcegenerators.inventory.MachineLayout;
import com.beepsterr.resourcegenerators.registry.ModFluids;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * GUI for the Crystal Crucible. Laid out left to right the way the machine works: the crystal (with its
 * fuel beneath) melts along the arrow into the tank, and the tank fills the containers on the right.
 *
 * <p>Placeholder art — the background, slots and tank are drawn in code in the vanilla panel style
 * rather than blitted from a texture. Swapping in a real texture means blitting it in
 * {@link #renderBg} and deleting the panel/slot calls; the coordinates stay as they are.
 */
public class CrystalCrucibleScreen extends AbstractContainerScreen<CrystalCrucibleMenu> {

    // Machine widgets. Slot coordinates are the top-left of the 16x16 content area and must match
    // the slot positions in CrystalCrucibleMenu. The fuel slot and flame are not listed here — they
    // live at the shared MachineGauges.FUEL_SLOT_*/FLAME_* position every machine uses.
    private static final int CRYSTAL_X = 20, CRYSTAL_Y = 17;
    private static final int ARROW_X = 44, ARROW_Y = 20;
    private static final int TANK_X = 76, TANK_Y = 17, TANK_W = 16, TANK_H = 51;
    private static final int CONTAINER_IN_X = 104, CONTAINER_IN_Y = 17;
    private static final int CONTAINER_OUT_X = 104, CONTAINER_OUT_Y = 52;

    public CrystalCrucibleScreen(CrystalCrucibleMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = MachineLayout.WIDTH;
        this.imageHeight = MachineLayout.HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        MachineGauges.renderPanel(guiGraphics, leftPos, topPos, imageWidth, imageHeight);
        MachineGauges.renderPlayerInventorySlots(guiGraphics, leftPos + MachineLayout.INVENTORY_X, topPos + MachineLayout.INVENTORY_Y);

        MachineGauges.renderSlot(guiGraphics, leftPos + CRYSTAL_X, topPos + CRYSTAL_Y);
        MachineGauges.renderSlot(guiGraphics, leftPos + CONTAINER_IN_X, topPos + CONTAINER_IN_Y);
        MachineGauges.renderSlot(guiGraphics, leftPos + CONTAINER_OUT_X, topPos + CONTAINER_OUT_Y);
        MachineGauges.renderSlot(guiGraphics, leftPos + MachineLayout.FUEL_SLOT_X, topPos + MachineLayout.FUEL_SLOT_Y);

        MachineGauges.renderTank(guiGraphics, leftPos + TANK_X, topPos + TANK_Y, TANK_W, TANK_H,
                menu.getTankAmount(), menu.getTankCapacity(), ModFluids.RESONANCE_COLOR);
        MachineGauges.renderArrow(guiGraphics, leftPos + ARROW_X, topPos + ARROW_Y,
                menu.getScaledProgress(MachineGauges.ARROW_WIDTH));
        MachineGauges.renderFlame(guiGraphics, leftPos + MachineLayout.FLAME_X, topPos + MachineLayout.FLAME_Y,
                menu.getScaledFlame(MachineGauges.FLAME_HEIGHT));
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        // Hovering the tank reports its exact contents, the way any fluid gauge should.
        if (MachineGauges.isOverTank(leftPos + TANK_X, topPos + TANK_Y, TANK_W, TANK_H, mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font,
                    Component.translatable("tooltip.bmcrm.tank", menu.getTankAmount(), menu.getTankCapacity()),
                    mouseX, mouseY);
        }
    }
}
