package com.beepsterr.resourcegenerators;

import com.beepsterr.resourcegenerators.registry.ModBlockEntities;
import com.beepsterr.resourcegenerators.registry.ModBlocks;
import com.beepsterr.resourcegenerators.registry.ModCapabilities;
import com.beepsterr.resourcegenerators.registry.ModCreativeTabs;
import com.beepsterr.resourcegenerators.registry.ModDataComponents;
import com.beepsterr.resourcegenerators.registry.ModDataMaps;
import com.beepsterr.resourcegenerators.registry.ModItems;
import com.beepsterr.resourcegenerators.registry.ModMenus;
import com.beepsterr.resourcegenerators.registry.ModParticles;
import com.beepsterr.resourcegenerators.registry.ModRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entrypoint for Beep's Mean Crystalline Resource Machine.
 *
 * <p>The mod concept: a central "resonator" block that periodically rolls loot tables
 * contributed by "crystal" items/blocks placed within range — a loose multiblock in the
 * same spirit as the vanilla enchanting table / bookshelf scan.
 *
 * <p>The crystal is a single data-driven item (tier + resource + enchantments); see the
 * {@code crystal} package for its data model.
 */
@Mod(BeepsResourceGenerators.MOD_ID)
public class BeepsResourceGenerators {

    public static final String MOD_ID = "bmcrm";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public BeepsResourceGenerators(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Beep's Mean Crystalline Resource Machine loading");

        // Datapack registries and data maps must be declared during mod construction.
        modEventBus.addListener(ModRegistries::onNewDataPackRegistry);
        modEventBus.addListener(ModDataMaps::register);

        // Deferred registers.
        ModDataComponents.REGISTER.register(modEventBus);
        ModBlocks.REGISTER.register(modEventBus);
        ModItems.REGISTER.register(modEventBus);
        ModBlockEntities.REGISTER.register(modEventBus);
        ModMenus.REGISTER.register(modEventBus);
        ModCreativeTabs.REGISTER.register(modEventBus);
        ModParticles.REGISTER.register(modEventBus);

        // Capabilities (item handler for automation).
        modEventBus.addListener(ModCapabilities::register);

        // Config (ore blacklist).
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, Config.SPEC);
    }

    /** Convenience: a ResourceLocation under this mod's namespace. */
    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
