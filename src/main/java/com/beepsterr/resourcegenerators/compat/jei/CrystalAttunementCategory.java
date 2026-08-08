package com.beepsterr.resourcegenerators.compat.jei;

import com.beepsterr.resourcegenerators.BeepsResourceGenerators;
import com.beepsterr.resourcegenerators.registry.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public class CrystalAttunementCategory implements IRecipeCategory<CrystalAttunementRecipe> {

    public static final RecipeType<CrystalAttunementRecipe> TYPE =
            RecipeType.create(BeepsResourceGenerators.MOD_ID, "crystal_attunement", CrystalAttunementRecipe.class);

    private final IDrawable icon;

    public CrystalAttunementCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemLike(ModItems.CRYSTAL.get());
    }

    @Override
    public RecipeType<CrystalAttunementRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("bmcrm.jei.crystal_attunement");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    /** Wide enough for the two how-to lines to wrap sensibly rather than run off the panel. */
    private static final int WIDTH = 150;
    private static final int TEXT_TOP = 26;

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return 62;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CrystalAttunementRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 5, 4).setStandardSlotBackground().addItemStack(recipe.mobEgg());
        builder.addSlot(RecipeIngredientRole.INPUT, 27, 4).setStandardSlotBackground().addItemStacks(recipe.blanks());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 71, 4).setOutputSlotBackground().addItemStacks(recipe.results());
    }

    /** The two routes, spelled out under the slots — neither involves a machine. */
    @Override
    public void draw(CrystalAttunementRecipe recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,
                     GuiGraphics graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        int y = TEXT_TOP;
        y = drawWrapped(graphics, font,
                Component.translatable("bmcrm.jei.crystal_attunement.kills", recipe.kills()), y);
        drawWrapped(graphics, font,
                Component.translatable("bmcrm.jei.crystal_attunement.spawner"), y);
    }

    /** Draw {@code text} wrapped to the category width, returning the y below the last line. */
    private static int drawWrapped(GuiGraphics graphics, Font font, Component text, int y) {
        for (FormattedCharSequence line : font.split(text, WIDTH)) {
            graphics.drawString(font, line, 0, y, 0x404040, false);
            y += font.lineHeight;
        }
        return y;
    }
}
