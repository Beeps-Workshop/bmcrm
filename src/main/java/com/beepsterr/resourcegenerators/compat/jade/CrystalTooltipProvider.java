package com.beepsterr.resourcegenerators.compat.jade;

import com.beepsterr.resourcegenerators.BeepsResourceGenerators;
import com.beepsterr.resourcegenerators.block.PlacedCrystalBlockEntity;
import com.beepsterr.resourcegenerators.crystal.CrystalData;
import com.beepsterr.resourcegenerators.crystal.CrystalResource;
import com.beepsterr.resourcegenerators.crystal.CrystalTier;
import com.beepsterr.resourcegenerators.item.CrystalItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Adds "Tier" and "Produces" (or infusion state) lines to a placed crystal's Jade tooltip, so the
 * in-world block conveys the same identity the item tooltip does. Reads the client-synced
 * {@link CrystalData} off the block entity.
 */
public enum CrystalTooltipProvider implements IBlockComponentProvider {
    INSTANCE;

    private static final ResourceLocation UID = BeepsResourceGenerators.rl("crystal_info");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        BlockEntity be = accessor.getBlockEntity();
        if (!(be instanceof PlacedCrystalBlockEntity crystal)) {
            return;
        }
        CrystalData data = crystal.getCrystalData();
        if (data == null) {
            tooltip.add(Component.translatable("tooltip.bmcrm.crystal.blank")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        CrystalTier tier = data.tier().value();
        int rollPercent = Math.round(tier.rollChance() * 100);
        tooltip.add(Component.translatable("tooltip.bmcrm.tier_line",
                CrystalItem.tierName(data.tier()), rollPercent).withColor(tier.color()));

        if (data.resource().isPresent()) {
            CrystalResource resource = data.resource().get();
            Component produced = resource.displayName().copy().withColor(resource.color());
            tooltip.add(Component.translatable("tooltip.bmcrm.produces", produced)
                    .withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.bmcrm.uninfused")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
