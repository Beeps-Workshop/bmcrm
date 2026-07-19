package com.beepsterr.resourcegenerators.client;

import com.beepsterr.resourcegenerators.BeepsResourceGenerators;
import com.beepsterr.resourcegenerators.block.ResonatorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** GUI for the resonator: shows the output buffer. */
public class ResonatorScreen extends AbstractContainerScreen<ResonatorMenu> {

    private static final ResourceLocation TEXTURE = BeepsResourceGenerators.rl("textures/gui/resonator.png");

    public ResonatorScreen(ResonatorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        // Left vertical work bar, fills bottom-up toward the next generation cycle.
        int filled = menu.getScaledProgress(50);
        if (filled > 0) {
            int bottom = topPos + 70;
            guiGraphics.fill(leftPos + 21, bottom - filled, leftPos + 29, bottom, 0xFFFFC24A);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
