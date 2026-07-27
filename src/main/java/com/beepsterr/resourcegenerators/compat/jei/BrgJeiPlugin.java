package com.beepsterr.resourcegenerators.compat.jei;

import com.beepsterr.resourcegenerators.BeepsResourceGenerators;
import com.beepsterr.resourcegenerators.Config;
import com.beepsterr.resourcegenerators.crystal.CrystalCharge;
import com.beepsterr.resourcegenerators.crystal.CrystalData;
import com.beepsterr.resourcegenerators.crystal.CrystalResource;
import com.beepsterr.resourcegenerators.crystal.CrystalTier;
import com.beepsterr.resourcegenerators.crystal.OreTagResource;
import com.beepsterr.resourcegenerators.item.CrystalItem;
import com.beepsterr.resourcegenerators.registry.ModBlocks;
import com.beepsterr.resourcegenerators.registry.ModDataComponents;
import com.beepsterr.resourcegenerators.registry.ModItems;
import com.beepsterr.resourcegenerators.registry.ModRecipes;
import com.beepsterr.resourcegenerators.registry.ModRegistries;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

/** JEI integration: shows Former, Infuser, Resonator and Crucible recipes, generated from data. */
@JeiPlugin
public class BrgJeiPlugin implements IModPlugin {

    private static final ResourceLocation ID = BeepsResourceGenerators.rl("jei");
    private static final List<String> FAMILIES = List.of("ingots", "gems", "dusts", "raw_materials");

    @Override
    public ResourceLocation getPluginUid() {
        return ID;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        // Make each crystal (tier + resource) a distinct JEI entry instead of one "base" crystal.
        registration.registerSubtypeInterpreter(ModItems.CRYSTAL.get(), new ISubtypeInterpreter<>() {
            @Override
            public Object getSubtypeData(ItemStack stack, UidContext context) {
                return crystalSubtype(stack);
            }

            @Override
            public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
                return crystalSubtype(stack);
            }
        });
    }

    private static String crystalSubtype(ItemStack stack) {
        CrystalData data = stack.get(ModDataComponents.CRYSTAL_DATA.get());
        if (data == null) {
            return "";
        }
        String tier = data.tier().unwrapKey().map(k -> k.location().toString()).orElse("?");
        String resource = data.resource().map(CrystalResource::subtypeKey).orElse("blank");
        return tier + "|" + resource;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new CrystalFormingCategory(guiHelper),
                new CrystalInfusingCategory(guiHelper),
                new CrystalGenerationCategory(guiHelper),
                new ResonanceInfusionCategory(guiHelper),
                new CrystalMeltingCategory(guiHelper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return; // registries/tags not available yet; JEI reloads on world join
        }
        RegistryAccess access = mc.level.registryAccess();
        registration.addRecipes(CrystalFormingCategory.TYPE, forming(access));
        registration.addRecipes(CrystalInfusingCategory.TYPE, infusing(access));
        registration.addRecipes(CrystalGenerationCategory.TYPE, generation(access));
        registration.addRecipes(CrystalMeltingCategory.TYPE, melting(access));
        // Crucible infusions come from the recipe manager — they're real datapack recipes.
        registration.addRecipes(ResonanceInfusionCategory.TYPE, mc.level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.RESONANCE_INFUSION.get()).stream()
                .map(net.minecraft.world.item.crafting.RecipeHolder::value)
                .toList());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(ModBlocks.CRYSTAL_FORMER.get(), CrystalFormingCategory.TYPE);
        registration.addRecipeCatalyst(ModBlocks.CRYSTAL_INFUSER.get(), CrystalInfusingCategory.TYPE);
        registration.addRecipeCatalyst(ModBlocks.RESONATOR.get(), CrystalGenerationCategory.TYPE);
        registration.addRecipeCatalyst(ModBlocks.CRYSTAL_CRUCIBLE.get(), ResonanceInfusionCategory.TYPE);
        registration.addRecipeCatalyst(ModBlocks.CRYSTAL_CRUCIBLE.get(), CrystalMeltingCategory.TYPE);
    }

    private static List<Holder.Reference<CrystalTier>> sortedTiers(RegistryAccess access) {
        return access.registryOrThrow(ModRegistries.CRYSTAL_TIER_KEY).holders()
                .sorted(Comparator.comparingInt(h -> h.value().level()))
                .toList();
    }

    private static List<CrystalFormingRecipe> forming(RegistryAccess access) {
        List<CrystalFormingRecipe> recipes = new ArrayList<>();
        for (Holder.Reference<CrystalTier> tier : sortedTiers(access)) {
            recipes.add(new CrystalFormingRecipe(
                    tier.value().base(), tier.value().catalyst(), CrystalItem.createBlank(tier)));
        }
        return recipes;
    }

    private static List<CrystalInfusingRecipe> infusing(RegistryAccess access) {
        HolderLookup.RegistryLookup<Item> items = access.lookupOrThrow(Registries.ITEM);
        HolderLookup.RegistryLookup<Block> blocks = access.lookupOrThrow(Registries.BLOCK);
        List<Holder.Reference<CrystalTier>> tiers = sortedTiers(access);

        // Collect every material name that appears under one of the material family tags.
        TreeSet<String> materials = new TreeSet<>();
        items.listTagIds().forEach(tag -> {
            ResourceLocation id = tag.location();
            if (!id.getNamespace().equals("c")) {
                return;
            }
            for (String family : FAMILIES) {
                String prefix = family + "/";
                if (id.getPath().startsWith(prefix)) {
                    materials.add(id.getPath().substring(prefix.length()));
                }
            }
        });

        List<CrystalInfusingRecipe> recipes = new ArrayList<>();
        for (String mat : materials) {
            TagKey<Block> ores = TagKey.create(Registries.BLOCK,
                    ResourceLocation.fromNamespaceAndPath("c", "ores/" + mat));
            boolean hasUsableOre = blocks.get(ores)
                    .map(set -> set.stream().anyMatch(h -> !Config.isBlacklisted(h)))
                    .orElse(false);
            if (!hasUsableOre) {
                continue; // no (non-blacklisted) ore for this material -> not infusable
            }

            List<ItemStack> inputs = new ArrayList<>();
            for (String family : FAMILIES) {
                TagKey<Item> formTag = TagKey.create(Registries.ITEM,
                        ResourceLocation.fromNamespaceAndPath("c", family + "/" + mat));
                items.get(formTag).ifPresent(set -> set.forEach(h -> inputs.add(new ItemStack(h))));
            }
            if (inputs.isEmpty()) {
                continue;
            }

            OreTagResource resource = new OreTagResource(ores);
            List<ItemStack> blanks = new ArrayList<>();
            List<ItemStack> results = new ArrayList<>();
            for (Holder.Reference<CrystalTier> tier : tiers) {
                blanks.add(CrystalItem.createBlank(tier));
                results.add(CrystalItem.create(tier, resource));
            }
            recipes.add(new CrystalInfusingRecipe(List.copyOf(inputs), blanks, results));
        }
        return recipes;
    }

    /**
     * One entry per tier that can hold resonance: every crystal of that tier melts to the same
     * amount, since the yield comes from the tier's capacity rather than the infused material.
     */
    private static List<CrystalMeltingRecipe> melting(RegistryAccess access) {
        List<CrystalMeltingRecipe> recipes = new ArrayList<>();
        for (Holder.Reference<CrystalTier> tier : sortedTiers(access)) {
            if (tier.value().hidden() || tier.value().resonanceCapacity() <= 0) {
                continue;
            }
            List<ItemStack> crystals = new ArrayList<>();
            for (TagKey<Block> oreTag : oreMaterialTags(access)) {
                // Shown saturated: the charge component fills the durability bar and puts the exact
                // "Resonance: n/n" on the tooltip, so the entry looks like a crystal worth melting
                // rather than a fresh one. The JEI subtype key is tier+resource only, so this doesn't
                // split entries or stop "show recipes" matching a player's own crystal.
                ItemStack crystal = CrystalItem.create(tier, new OreTagResource(oreTag));
                CrystalCharge.set(crystal, tier.value().resonanceCapacity());
                crystals.add(crystal);
            }
            if (crystals.isEmpty()) {
                continue;
            }
            recipes.add(new CrystalMeltingRecipe(List.copyOf(crystals), tier.value().resonanceCapacity()));
        }
        return recipes;
    }

    /** Every {@code c:ores/<mat>} tag that has at least one non-blacklisted block behind it. */
    private static List<TagKey<Block>> oreMaterialTags(RegistryAccess access) {
        HolderLookup.RegistryLookup<Block> blocks = access.lookupOrThrow(Registries.BLOCK);
        return blocks.listTagIds()
                .filter(t -> t.location().getNamespace().equals("c")
                        && t.location().getPath().startsWith("ores/")
                        && !t.location().getPath().substring("ores/".length()).contains("/"))
                .filter(t -> blocks.get(t)
                        .map(set -> set.stream().anyMatch(h -> !Config.isBlacklisted(h)))
                        .orElse(false))
                .sorted(Comparator.comparing(t -> t.location().toString()))
                .toList();
    }

    /** Output forms an ore actually drops (raw material, gem, or dust) — never an ingot. */
    private static final List<String> OUTPUT_FAMILIES = List.of("raw_materials", "gems", "dusts");

    private static List<CrystalGenerationRecipe> generation(RegistryAccess access) {
        HolderLookup.RegistryLookup<Item> items = access.lookupOrThrow(Registries.ITEM);
        List<Holder.Reference<CrystalTier>> tiers = sortedTiers(access);

        List<CrystalGenerationRecipe> recipes = new ArrayList<>();
        oreMaterialTags(access)
                .forEach(oreTag -> {
                    String mat = oreTag.location().getPath().substring("ores/".length());

                    List<ItemStack> outputs = new ArrayList<>();
                    for (String family : OUTPUT_FAMILIES) {
                        TagKey<Item> formTag = TagKey.create(Registries.ITEM,
                                ResourceLocation.fromNamespaceAndPath("c", family + "/" + mat));
                        var set = items.get(formTag);
                        if (set.isPresent() && set.get().size() > 0) {
                            set.get().forEach(h -> outputs.add(new ItemStack(h)));
                            break; // an ore drops one form; take the first that exists
                        }
                    }
                    if (outputs.isEmpty()) {
                        return; // e.g. coal has no raw/gem/dust form
                    }

                    OreTagResource resource = new OreTagResource(oreTag);
                    List<ItemStack> crystals = new ArrayList<>();
                    for (Holder.Reference<CrystalTier> tier : tiers) {
                        crystals.add(CrystalItem.create(tier, resource));
                    }
                    recipes.add(new CrystalGenerationRecipe(List.copyOf(crystals), List.copyOf(outputs)));
                });
        return recipes;
    }
}
