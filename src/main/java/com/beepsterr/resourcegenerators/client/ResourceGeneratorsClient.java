package com.beepsterr.resourcegenerators.client;

import com.beepsterr.resourcegenerators.BeepsResourceGenerators;
import com.beepsterr.resourcegenerators.block.PlacedCrystalBlockEntity;
import com.beepsterr.resourcegenerators.client.ResonatorScreen;
import com.beepsterr.resourcegenerators.client.CrystalFormerScreen;
import com.beepsterr.resourcegenerators.client.CrystalInfuserScreen;
import com.beepsterr.resourcegenerators.crystal.CrystalData;
import com.beepsterr.resourcegenerators.crystal.CrystalResource;
import com.beepsterr.resourcegenerators.registry.ModBlockEntities;
import com.beepsterr.resourcegenerators.registry.ModBlocks;
import com.beepsterr.resourcegenerators.registry.ModDataComponents;
import com.beepsterr.resourcegenerators.registry.ModItems;
import com.beepsterr.resourcegenerators.registry.ModMenus;
import com.beepsterr.resourcegenerators.registry.ModParticles;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/** Client-only setup: colours the crystal per its resource so variants look distinct. */
@EventBusSubscriber(modid = BeepsResourceGenerators.MOD_ID, value = Dist.CLIENT)
public final class ResourceGeneratorsClient {

    private ResourceGeneratorsClient() {}

    @SubscribeEvent
    static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
            if (tintIndex != 0) {
                return -1;
            }
            CrystalData data = stack.get(ModDataComponents.CRYSTAL_DATA.get());
            if (data == null) {
                return -1;
            }
            // Infused crystals tint by their resource; blanks tint by their tier colour.
            int rgb = data.resource().map(CrystalColors::display).orElse(data.tier().value().color());
            return 0xFF000000 | rgb;
        }, ModItems.CRYSTAL.get());
    }

    @SubscribeEvent
    static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
            if (level == null || pos == null) {
                return -1;
            }
            if (level.getBlockEntity(pos) instanceof PlacedCrystalBlockEntity be && be.getCrystalData() != null) {
                CrystalData data = be.getCrystalData();
                return data.resource().map(CrystalColors::display).orElse(data.tier().value().color());
            }
            return -1;
        }, ModBlocks.PLACED_CRYSTAL.get());
    }

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.RESONATOR.get(), ResonatorRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.CRYSTAL_INFUSER.get(), CrystalInfuserRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.CRYSTAL_FORMER.get(), CrystalFormerRenderer::new);
    }

    @SubscribeEvent
    static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.RESONANCE_RING.get(), ResonanceRingParticle.Provider::new);
    }

    @SubscribeEvent
    static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        // Standalone models drawn by the block-entity renderers (not referenced by any blockstate).
        event.register(ResonatorRenderer.RING);
        event.register(ResonatorRenderer.FLUID);
        event.register(CrystalFormerRenderer.FLUID);
    }

    @SubscribeEvent
    static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.CRYSTAL_FORMER.get(), CrystalFormerScreen::new);
        event.register(ModMenus.CRYSTAL_INFUSER.get(), CrystalInfuserScreen::new);
        event.register(ModMenus.RESONATOR.get(), ResonatorScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Ponder is a soft dependency: only register scenes when it's actually installed. The guard
        // keeps PonderCompat (and every Ponder class it touches) from being loaded when it's absent.
        if (ModList.get().isLoaded("ponder")) {
            event.enqueueWork(com.beepsterr.resourcegenerators.compat.ponder.PonderCompat::register);
        }
    }
}
