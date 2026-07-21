package com.beepsterr.resourcegenerators.client;

import com.beepsterr.resourcegenerators.BeepsResourceGenerators;
import com.beepsterr.resourcegenerators.block.CrystalInfuserMenu;
import com.beepsterr.resourcegenerators.client.gui.MachineGauges;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** GUI for the Crystal Infuser. Fill progress is shown on the crystal item's durability bar. */
public class CrystalInfuserScreen extends AbstractContainerScreen<CrystalInfuserMenu> {

    private static final ResourceLocation TEXTURE = BeepsResourceGenerators.rl("textures/gui/crystal_infuser.png");

    public CrystalInfuserScreen(CrystalInfuserMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        // Furnace-style flame above the bottom-right fuel slot.
        MachineGauges.renderFlame(guiGraphics, leftPos + 141, topPos + 37, menu.getScaledFlame(MachineGauges.FLAME_HEIGHT));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
