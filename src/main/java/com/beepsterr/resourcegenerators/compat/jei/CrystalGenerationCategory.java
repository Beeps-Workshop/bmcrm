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

/** JEI category: a resource crystal (any tier) + a Resonator → the item it generates. */
public class CrystalGenerationCategory implements IRecipeCategory<CrystalGenerationRecipe> {

    public static final RecipeType<CrystalGenerationRecipe> TYPE =
            RecipeType.create(BeepsResourceGenerators.MOD_ID, "crystal_generation", CrystalGenerationRecipe.class);

    private final IDrawable icon;

    public CrystalGenerationCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemLike(ModBlocks.RESONATOR.get());
    }

    @Override
    public RecipeType<CrystalGenerationRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.beepsresourcegenerators.resonator");
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
    public void setRecipe(IRecipeLayoutBuilder builder, CrystalGenerationRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 5, 4).setStandardSlotBackground().addItemStacks(recipe.crystals());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 71, 4).setOutputSlotBackground().addItemStacks(recipe.outputs());
    }
}
