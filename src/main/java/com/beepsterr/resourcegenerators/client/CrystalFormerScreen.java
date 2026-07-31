package com.beepsterr.resourcegenerators.client;

import com.beepsterr.resourcegenerators.BeepsResourceGenerators;
import com.beepsterr.resourcegenerators.block.CrystalFormerMenu;
import com.beepsterr.resourcegenerators.client.gui.MachineGauges;
import com.beepsterr.resourcegenerators.inventory.MachineLayout;
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

    // Texture is 256x256: the visible GUI lives in the top-left 176x166. The vessel's fill is drawn
    // from a "filled" sprite parked off-canvas at (FILL_U, FILL_V), revealed top-down over the empty
    // vessel at (ARROW_X, ARROW_Y). The sprite is white so it can be tinted to the crystal's colour.
    private static final int TEX = 256;
    private static final int ARROW_X = 79, ARROW_Y = 32, ARROW_W = 7, ARROW_H = 36;
    private static final int ARROW_FILL_U = 180, ARROW_FILL_V = 32;

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, TEX, TEX);
        // Vessel fill: reveal the top `progress` px of the filled sprite, tinted to the tier's colour
        // (white sprite × colour = colour). Fills top to bottom, masked to the painted shape.
        int progress = menu.getScaledProgress(ARROW_H);
        if (progress > 0) {
            int color = menu.getFormingColor();
            if (color >= 0) {
                guiGraphics.setColor(((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f, 1f);
            }
            guiGraphics.blit(TEXTURE, leftPos + ARROW_X, topPos + ARROW_Y,
                    ARROW_FILL_U, ARROW_FILL_V, ARROW_W, progress, TEX, TEX);
            guiGraphics.setColor(1f, 1f, 1f, 1f);
        }
        // Furnace-style flame above the bottom-right fuel slot.
        MachineGauges.renderFlame(guiGraphics, leftPos + MachineLayout.FLAME_X, topPos + MachineLayout.FLAME_Y, menu.getScaledFlame(MachineGauges.FLAME_HEIGHT));
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
