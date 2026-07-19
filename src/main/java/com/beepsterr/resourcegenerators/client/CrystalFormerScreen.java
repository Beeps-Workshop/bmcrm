package com.beepsterr.resourcegenerators.client;

import com.beepsterr.resourcegenerators.BeepsResourceGenerators;
import com.beepsterr.resourcegenerators.block.CrystalFormerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** GUI for the Crystal Former. */
public class CrystalFormerScreen extends AbstractContainerScreen<CrystalFormerMenu> {

    private static final ResourceLocation TEXTURE = BeepsResourceGenerators.rl("textures/gui/crystal_former.png");

    public CrystalFormerScreen(CrystalFormerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        // Downward progress bar in the arrow between the two inputs and the output.
        int progress = menu.getScaledProgress(12);
        if (progress > 0) {
            guiGraphics.fill(leftPos + 86, topPos + 37, leftPos + 90, topPos + 37 + progress, 0xFF64C8FF);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Only the title — the vertical slot column leaves no room for the "Inventory" label.
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
