package com.beepsterr.resourcegenerators.compat.jei;

import com.beepsterr.resourcegenerators.BeepsResourceGenerators;
import com.beepsterr.resourcegenerators.crystal.ResonanceInfusionRecipe;
import com.beepsterr.resourcegenerators.registry.ModBlocks;
import com.beepsterr.resourcegenerators.registry.ModFluids;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.fluids.FluidStack;

/** Crucible infusion as shown in JEI: an item steeped in Liquid Resonance becomes a component. */
public class ResonanceInfusionCategory implements IRecipeCategory<ResonanceInfusionRecipe> {

    public static final RecipeType<ResonanceInfusionRecipe> TYPE =
            RecipeType.create(BeepsResourceGenerators.MOD_ID, "resonance_infusion", ResonanceInfusionRecipe.class);

    private final IDrawable icon;

    public ResonanceInfusionCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemLike(ModBlocks.CRYSTAL_CRUCIBLE.get());
    }

    @Override
    public RecipeType<ResonanceInfusionRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.bmcrm.crystal_crucible");
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
    public void setRecipe(IRecipeLayoutBuilder builder, ResonanceInfusionRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 5, 3)
                .setStandardSlotBackground()
                .addIngredients(recipe.ingredient());
        // The resonance cost, shown as the fluid it is — hovering reports the exact mB.
        builder.addSlot(RecipeIngredientRole.INPUT, 5, 23)
                .setStandardSlotBackground()
                .addFluidStack(ModFluids.LIQUID_RESONANCE.get(), recipe.resonance())
                .setFluidRenderer(Math.max(recipe.resonance(), 1), false, 16, 16);
        builder.addSlot(RecipeIngredientRole.OUTPUT, 67, 13)
                .setOutputSlotBackground()
                .addItemStack(recipe.result());
    }
}
