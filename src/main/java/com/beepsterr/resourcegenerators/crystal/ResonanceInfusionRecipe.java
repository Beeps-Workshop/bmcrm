package com.beepsterr.resourcegenerators.crystal;

import com.beepsterr.resourcegenerators.registry.ModRecipes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/**
 * A Crucible infusion: one ingredient steeped in {@code resonance} mB of Liquid Resonance becomes
 * {@code result}. This is how the fluid is spent — the cost ends up baked into an item, so the
 * machines that consume the result (the Former, the modulator crafting recipes) need no tank.
 *
 * <p>Data-driven, so packs can add or rebalance components without touching code. Example:
 * <pre>{@code
 * { "type": "bmcrm:resonance_infusion",
 *   "ingredient": { "item": "minecraft:lapis_lazuli" },
 *   "resonance": 25,
 *   "result": { "id": "bmcrm:activated_lapis_lazuli", "count": 1 } }
 * }</pre>
 */
public record ResonanceInfusionRecipe(Ingredient ingredient, int resonance, ItemStack result)
        implements Recipe<SingleRecipeInput> {

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.RESONANCE_INFUSION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.RESONANCE_INFUSION.get();
    }

    public static class Serializer implements RecipeSerializer<ResonanceInfusionRecipe> {

        private static final MapCodec<ResonanceInfusionRecipe> CODEC =
                RecordCodecBuilder.mapCodec(inst -> inst.group(
                        Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(ResonanceInfusionRecipe::ingredient),
                        com.mojang.serialization.Codec.INT.fieldOf("resonance").forGetter(ResonanceInfusionRecipe::resonance),
                        ItemStack.CODEC.fieldOf("result").forGetter(ResonanceInfusionRecipe::result)
                ).apply(inst, ResonanceInfusionRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, ResonanceInfusionRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        Ingredient.CONTENTS_STREAM_CODEC, ResonanceInfusionRecipe::ingredient,
                        ByteBufCodecs.VAR_INT, ResonanceInfusionRecipe::resonance,
                        ItemStack.STREAM_CODEC, ResonanceInfusionRecipe::result,
                        ResonanceInfusionRecipe::new);

        @Override
        public MapCodec<ResonanceInfusionRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ResonanceInfusionRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
