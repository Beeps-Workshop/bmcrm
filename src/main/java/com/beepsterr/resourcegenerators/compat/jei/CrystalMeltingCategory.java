package com.beepsterr.resourcegenerators.compat.jei;

import com.beepsterr.resourcegenerators.BeepsResourceGenerators;
import com.beepsterr.resourcegenerators.registry.ModBlocks;
import com.beepsterr.resourcegenerators.registry.ModFluids;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** JEI category: a charged crystal melted in the Crucible → the Liquid Resonance it was holding. */
public class CrystalMeltingCategory implements IRecipeCategory<CrystalMeltingRecipe> {

    public static final RecipeType<CrystalMeltingRecipe> TYPE =
            RecipeType.create(BeepsResourceGenerators.MOD_ID, "crystal_melting", CrystalMeltingRecipe.class);

    private final IDrawable icon;

    public CrystalMeltingCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemLike(ModBlocks.CRYSTAL_CRUCIBLE.get());
    }

    @Override
    public RecipeType<CrystalMeltingRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("bmcrm.jei.crystal_melting");
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
    public void setRecipe(IRecipeLayoutBuilder builder, CrystalMeltingRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 5, 4)
                .setStandardSlotBackground()
                .addItemStacks(recipe.crystals());
        // The amount shown is what a saturated crystal yields; a half-charged one gives half as much,
        // so the tooltip spells that out rather than letting the number read as a guarantee.
        builder.addSlot(RecipeIngredientRole.OUTPUT, 71, 4)
                .setOutputSlotBackground()
                .addFluidStack(ModFluids.LIQUID_RESONANCE.get(), recipe.capacity())
                .setFluidRenderer(Math.max(recipe.capacity(), 1), false, 16, 16)
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(
                        Component.translatable("bmcrm.jei.crystal_melting.yield", recipe.capacity())
                                .withStyle(ChatFormatting.GRAY)));
    }
}
