package com.beepsterr.resourcegenerators.registry;

import com.beepsterr.resourcegenerators.BeepsResourceGenerators;
import com.beepsterr.resourcegenerators.crystal.ResonanceInfusionRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** Recipe type + serializer registrations. */
public final class ModRecipes {

    private ModRecipes() {}

    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, BeepsResourceGenerators.MOD_ID);

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, BeepsResourceGenerators.MOD_ID);

    /** Crucible infusion: ingredient + Liquid Resonance -> resonant component. */
    public static final Supplier<RecipeType<ResonanceInfusionRecipe>> RESONANCE_INFUSION =
            TYPES.register("resonance_infusion", () -> RecipeType.simple(
                    BeepsResourceGenerators.rl("resonance_infusion")));

    public static final Supplier<RecipeSerializer<ResonanceInfusionRecipe>> RESONANCE_INFUSION_SERIALIZER =
            SERIALIZERS.register("resonance_infusion", ResonanceInfusionRecipe.Serializer::new);
}
