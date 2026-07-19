package com.beepsterr.resourcegenerators.item;

import com.beepsterr.resourcegenerators.crystal.CrystalData;
import com.beepsterr.resourcegenerators.crystal.CrystalInfusion;
import com.beepsterr.resourcegenerators.crystal.CrystalResource;
import com.beepsterr.resourcegenerators.crystal.CrystalTier;
import com.beepsterr.resourcegenerators.registry.ModDataComponents;
import com.beepsterr.resourcegenerators.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * The single, data-driven crystal item. Its identity (tier + optional resource) lives in the
 * {@link ModDataComponents#CRYSTAL_DATA} component, so one registered item represents every
 * possible crystal; name, tint and tooltip are derived from that component. A crystal with no
 * resource is a "blank" (produced by the Former, resource filled in by the Infuser).
 */
public class CrystalItem extends BlockItem {

    public CrystalItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Nullable
    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        // A crystal mid-infusion only carries its resource when placed, so its fill progress would
        // be lost on break — refuse to place it until it's finished.
        if (context.getItemInHand().has(ModDataComponents.CRYSTAL_INFUSION.get())) {
            return null;
        }
        return super.getPlacementState(context);
    }

    /** Build an infused crystal stack for the given tier + resource. */
    public static ItemStack create(Holder<CrystalTier> tier, CrystalResource resource) {
        return withData(new CrystalData(tier, java.util.Optional.of(resource)));
    }

    /** Build a blank (uninfused) crystal stack of the given tier. */
    public static ItemStack createBlank(Holder<CrystalTier> tier) {
        return withData(CrystalData.blank(tier));
    }

    private static ItemStack withData(CrystalData data) {
        ItemStack stack = new ItemStack(ModItems.CRYSTAL.get());
        stack.set(ModDataComponents.CRYSTAL_DATA.get(), data);
        return stack;
    }

    /** Title-cased display name of a tier, derived from its registry id (e.g. "Ender"). */
    public static Component tierName(Holder<CrystalTier> tier) {
        String path = tier.unwrapKey().map(key -> key.location().getPath()).orElse("unknown");
        String pretty = path.isEmpty() ? path
                : Character.toUpperCase(path.charAt(0)) + path.substring(1).toLowerCase(Locale.ROOT);
        return Component.literal(pretty);
    }

    @Override
    public Component getName(ItemStack stack) {
        CrystalData data = stack.get(ModDataComponents.CRYSTAL_DATA.get());
        if (data == null) {
            return super.getName(stack);
        }
        if (data.resource().isPresent()) {
            return Component.translatable("item.beepsresourcegenerators.crystal.named",
                    data.resource().get().displayName(), tierName(data.tier()));
        }
        return Component.translatable("item.beepsresourcegenerators.crystal.blank_named", tierName(data.tier()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CrystalData data = stack.get(ModDataComponents.CRYSTAL_DATA.get());
        if (data == null) {
            tooltip.add(Component.translatable("tooltip.beepsresourcegenerators.crystal.blank")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        CrystalTier tier = data.tier().value();
        int rollPercent = Math.round(tier.rollChance() * 100);
        // "<Tier> Crystal (X%)" — the whole line tinted the tier's colour.
        tooltip.add(Component.translatable("tooltip.beepsresourcegenerators.tier_line",
                tierName(data.tier()), rollPercent).withColor(tier.color()));

        CrystalInfusion infusion = stack.get(ModDataComponents.CRYSTAL_INFUSION.get());
        if (data.resource().isPresent()) {
            CrystalResource resource = data.resource().get();
            Component produced = Component.literal(resource.displayName().getString()).withColor(resourceColor(resource));
            tooltip.add(Component.translatable("tooltip.beepsresourcegenerators.produces", produced)
                    .withStyle(ChatFormatting.GRAY));
        } else if (infusion != null) {
            tooltip.add(Component.translatable("tooltip.beepsresourcegenerators.infusing",
                    infusion.target().displayName(), infusion.amount(), infusion.required())
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            tooltip.add(Component.translatable("tooltip.beepsresourcegenerators.uninfused")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /** Resource colour matching the item icon on the client; a server-safe fallback otherwise. */
    private static int resourceColor(CrystalResource resource) {
        if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
            return com.beepsterr.resourcegenerators.client.CrystalColors.display(resource);
        }
        return resource.color();
    }

    // --- Durability bar shows infusion fill progress on a blank crystal ---

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.has(ModDataComponents.CRYSTAL_INFUSION.get());
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        CrystalInfusion infusion = stack.get(ModDataComponents.CRYSTAL_INFUSION.get());
        return infusion == null ? 0 : Mth.clamp(Math.round(13.0f * infusion.fraction()), 0, 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        CrystalInfusion infusion = stack.get(ModDataComponents.CRYSTAL_INFUSION.get());
        return infusion == null ? 0xFFFFFF : infusion.target().color();
    }
}
