package com.beepsterr.resourcegenerators.crystal;

import com.beepsterr.resourcegenerators.BeepsResourceGenerators;
import com.beepsterr.resourcegenerators.registry.ModRecipes;
import com.beepsterr.resourcegenerators.registry.ModRegistries;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * A Crystal Former recipe written by a datapack: base + catalyst becomes any item at all.
 *
 * <p>The Former's built-in job — a tier's base + catalyst becoming a blank crystal of that tier —
 * still comes from the {@code bmcrm:crystal_tier} registry and is matched <em>first</em>. These
 * recipes are for everything else a pack wants the machine to make; an input pair already claimed
 * by a tier can never reach one, which {@link #warnAboutShadowed} reports at server start.
 *
 * <pre>{@code
 * { "type": "bmcrm:forming",
 *   "base_ingredient": { "item": "minecraft:copper_ingot" },
 *   "catalyst_ingredient": { "tag": "c:gems/amethyst" },
 *   "result": { "id": "bmcrm:resonating_gem", "count": 2 },
 *   "duration": 600 }
 * }</pre>
 */
public record FormingRecipe(Ingredient base, Ingredient catalyst, ItemStack result, int duration)
        implements Recipe<FormingRecipe.Input> {

    /** Forming takes a long time — ~120 seconds — unless a recipe says otherwise. */
    public static final int DEFAULT_DURATION = 2400;

    /** The Former's two input slots, in the order the machine holds them. */
    public record Input(ItemStack baseStack, ItemStack catalystStack) implements RecipeInput {

        @Override
        public ItemStack getItem(int slot) {
            return switch (slot) {
                case 0 -> baseStack;
                case 1 -> catalystStack;
                default -> ItemStack.EMPTY;
            };
        }

        @Override
        public int size() {
            return 2;
        }
    }

    @Override
    public boolean matches(Input input, Level level) {
        return base.test(input.baseStack()) && catalyst.test(input.catalystStack());
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
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
        return ModRecipes.FORMING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.FORMING.get();
    }

    /**
     * Logs every recipe whose input pair a crystal tier also accepts. Tiers win, so such a recipe
     * would just never craft — silently, and only for the pairs that overlap, which is miserable to
     * work out from in-game behaviour alone.
     */
    public static void warnAboutShadowed(MinecraftServer server) {
        var tiers = ModRegistries.loadedTiers(server.registryAccess());
        List<String> shadowed = new ArrayList<>();
        for (RecipeHolder<FormingRecipe> holder : server.getRecipeManager().getAllRecipesFor(ModRecipes.FORMING.get())) {
            FormingRecipe recipe = holder.value();
            for (Holder.Reference<CrystalTier> tier : tiers) {
                boolean overlaps = anyMatch(tier.value().base(), recipe.base())
                        && anyMatch(tier.value().catalyst(), recipe.catalyst());
                if (overlaps) {
                    shadowed.add(holder.id() + " (shadowed by tier " + tier.key().location() + ")");
                    break;
                }
            }
        }
        if (!shadowed.isEmpty()) {
            BeepsResourceGenerators.LOGGER.warn(
                    "{} Crystal Former recipe(s) share an input pair with a crystal tier and will never "
                    + "craft, because tiers are matched first: {}",
                    shadowed.size(), String.join(", ", shadowed));
        }
    }

    /** Whether {@code tierSide} accepts anything {@code recipeSide} does. */
    private static boolean anyMatch(Ingredient tierSide, Ingredient recipeSide) {
        for (ItemStack stack : recipeSide.getItems()) {
            if (tierSide.test(stack)) {
                return true;
            }
        }
        return false;
    }

    public static class Serializer implements RecipeSerializer<FormingRecipe> {

        private static final MapCodec<FormingRecipe> CODEC =
                RecordCodecBuilder.mapCodec(inst -> inst.group(
                        Ingredient.CODEC_NONEMPTY.fieldOf("base_ingredient").forGetter(FormingRecipe::base),
                        Ingredient.CODEC_NONEMPTY.fieldOf("catalyst_ingredient").forGetter(FormingRecipe::catalyst),
                        ItemStack.CODEC.fieldOf("result").forGetter(FormingRecipe::result),
                        Codec.INT.optionalFieldOf("duration", DEFAULT_DURATION).forGetter(FormingRecipe::duration)
                ).apply(inst, FormingRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, FormingRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        Ingredient.CONTENTS_STREAM_CODEC, FormingRecipe::base,
                        Ingredient.CONTENTS_STREAM_CODEC, FormingRecipe::catalyst,
                        ItemStack.STREAM_CODEC, FormingRecipe::result,
                        ByteBufCodecs.VAR_INT, FormingRecipe::duration,
                        FormingRecipe::new);

        @Override
        public MapCodec<FormingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FormingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
