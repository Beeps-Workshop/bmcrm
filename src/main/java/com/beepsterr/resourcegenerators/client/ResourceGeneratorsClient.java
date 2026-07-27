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
import com.beepsterr.resourcegenerators.registry.ModFluids;
import com.beepsterr.resourcegenerators.registry.ModItems;
import com.beepsterr.resourcegenerators.registry.ModMenus;
import com.beepsterr.resourcegenerators.registry.ModParticles;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
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
        event.registerBlockEntityRenderer(ModBlockEntities.CRYSTAL_CRUCIBLE.get(), CrystalCrucibleRenderer::new);
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
        event.register(CrystalCrucibleRenderer.FLUID);
    }

    @SubscribeEvent
    static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.CRYSTAL_FORMER.get(), CrystalFormerScreen::new);
        event.register(ModMenus.CRYSTAL_INFUSER.get(), CrystalInfuserScreen::new);
        event.register(ModMenus.CRYSTAL_CRUCIBLE.get(), CrystalCrucibleScreen::new);
        event.register(ModMenus.RESONATOR.get(), ResonatorScreen::new);
    }

    /**
     * How Liquid Resonance draws in the world and in tanks. Placeholder art: vanilla's water sprites
     * tinted to the resonance colour, until the fluid gets textures of its own.
     */
    @SubscribeEvent
    static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            private static final ResourceLocation STILL = ResourceLocation.withDefaultNamespace("block/water_still");
            private static final ResourceLocation FLOWING = ResourceLocation.withDefaultNamespace("block/water_flow");

            @Override
            public ResourceLocation getStillTexture() {
                return STILL;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return FLOWING;
            }

            @Override
            public int getTintColor() {
                return ModFluids.RESONANCE_COLOR;
            }
        }, ModFluids.LIQUID_RESONANCE_TYPE.get());
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
