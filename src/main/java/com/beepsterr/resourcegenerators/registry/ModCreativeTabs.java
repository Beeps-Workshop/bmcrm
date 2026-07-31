package com.beepsterr.resourcegenerators.registry;

import com.beepsterr.resourcegenerators.BeepsResourceGenerators;
import com.beepsterr.resourcegenerators.Config;
import com.beepsterr.resourcegenerators.crystal.CrystalTier;
import com.beepsterr.resourcegenerators.crystal.OreTagResource;
import com.beepsterr.resourcegenerators.item.CrystalItem;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

/**
 * Creative tab. Lists the machines, the blank crystals per tier, and one infused crystal per ore
 * material (every {@code c:ores/<mat>} tag — so vanilla and modded ores both show up), skipping
 * anything on the config blacklist.
 */
public final class ModCreativeTabs {

    private ModCreativeTabs() {}

    public static final DeferredRegister<CreativeModeTab> REGISTER =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BeepsResourceGenerators.MOD_ID);

    public static final Supplier<CreativeModeTab> MAIN = REGISTER.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.bmcrm"))
            .icon(() -> new ItemStack(ModItems.CRYSTAL.get()))
            .withSearchBar()
            .displayItems((params, output) -> {
                output.accept(ModItems.RESONATOR.get());
                output.accept(ModItems.CRYSTAL_FORMER.get());
                output.accept(ModItems.CRYSTAL_INFUSER.get());
                output.accept(ModItems.CRYSTAL_CRUCIBLE.get());
                output.accept(ModItems.MODULATOR_BASE.get());
                output.accept(ModItems.SILK_TOUCH_MODULATOR.get());
                output.accept(ModItems.FORTUNE_MODULATOR.get());
                output.accept(ModItems.AUTO_SMELT_MODULATOR.get());
                output.accept(ModItems.TUNING_FORK.get());
                output.accept(ModItems.LIQUID_RESONANCE_BUCKET.get());
                output.accept(ModItems.ACTIVATED_LAPIS_LAZULI.get());
                output.accept(ModItems.SHINING_CRYSTALS.get());
                output.accept(ModItems.PULSING_PEARL.get());
                output.accept(ModItems.RESONATING_GEM.get());

                var tiersOpt = params.holders().lookup(ModRegistries.CRYSTAL_TIER_KEY);
                var blocksOpt = params.holders().lookup(Registries.BLOCK);
                if (tiersOpt.isEmpty() || blocksOpt.isEmpty()) {
                    return;
                }
                HolderLookup.RegistryLookup<Block> blocks = blocksOpt.get();
                List<Holder.Reference<CrystalTier>> tiers = tiersOpt.get().listElements()
                        .sorted(Comparator.comparingInt(t -> t.value().level()))
                        .toList();

                // Blank crystals, one per tier.
                for (Holder.Reference<CrystalTier> tier : tiers) {
                    output.accept(CrystalItem.createBlank(tier));
                }

                // Ore materials: every c:ores/<mat> tag with at least one non-blacklisted block.
                List<TagKey<Block>> oreMaterials = blocks.listTagIds()
                        .filter(t -> t.location().getNamespace().equals("c"))
                        .filter(t -> t.location().getPath().startsWith("ores/")
                                && !t.location().getPath().substring("ores/".length()).contains("/"))
                        .filter(t -> blocks.get(t)
                                .map(set -> set.stream().anyMatch(h -> !Config.isBlacklisted(h)))
                                .orElse(false))
                        .sorted(Comparator.comparing(t -> t.location().toString()))
                        .toList();

                // Grouped by resource: all tiers of one material together, then the next material.
                for (TagKey<Block> oreTag : oreMaterials) {
                    for (Holder.Reference<CrystalTier> tier : tiers) {
                        output.accept(CrystalItem.create(tier, new OreTagResource(oreTag)));
                    }
                }
            })
            .build());
}
