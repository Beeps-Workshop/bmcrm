package com.beepsterr.resourcegenerators.compat.jei;

import com.beepsterr.resourcegenerators.BeepsResourceGenerators;
import com.beepsterr.resourcegenerators.registry.ModBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;

public class CrystalFormingCategory implements IRecipeCategory<CrystalFormingRecipe> {

    public static final RecipeType<CrystalFormingRecipe> TYPE =
            RecipeType.create(BeepsResourceGenerators.MOD_ID, "crystal_forming", CrystalFormingRecipe.class);

    private final IDrawable icon;

    public CrystalFormingCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemLike(ModBlocks.CRYSTAL_FORMER.get());
    }

    @Override
    public RecipeType<CrystalFormingRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.beepsresourcegenerators.crystal_former");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return 90;
    }

    @Override
    public int getHeight() {
        return 42;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CrystalFormingRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 5, 3).setStandardSlotBackground().addIngredients(recipe.catalyst());
        builder.addSlot(RecipeIngredientRole.INPUT, 5, 23).setStandardSlotBackground().addIngredients(recipe.base());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 67, 13).setOutputSlotBackground().addItemStack(recipe.result());
    }
}
