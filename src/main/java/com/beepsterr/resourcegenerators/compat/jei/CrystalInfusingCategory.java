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

public class CrystalInfusingCategory implements IRecipeCategory<CrystalInfusingRecipe> {

    public static final RecipeType<CrystalInfusingRecipe> TYPE =
            RecipeType.create(BeepsResourceGenerators.MOD_ID, "crystal_infusing", CrystalInfusingRecipe.class);

    private final IDrawable icon;

    public CrystalInfusingCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemLike(ModBlocks.CRYSTAL_INFUSER.get());
    }

    @Override
    public RecipeType<CrystalInfusingRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.beepsresourcegenerators.crystal_infuser");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return 94;
    }

    @Override
    public int getHeight() {
        return 26;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CrystalInfusingRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 5, 4).setStandardSlotBackground().addItemStacks(recipe.materials());
        builder.addSlot(RecipeIngredientRole.INPUT, 27, 4).setStandardSlotBackground().addItemStacks(recipe.blanks());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 71, 4).setOutputSlotBackground().addItemStacks(recipe.results());
    }
}
