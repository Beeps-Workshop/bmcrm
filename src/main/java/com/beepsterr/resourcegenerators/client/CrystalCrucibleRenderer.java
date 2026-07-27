package com.beepsterr.resourcegenerators.client;

import com.beepsterr.resourcegenerators.BeepsResourceGenerators;
import com.beepsterr.resourcegenerators.block.CrystalCrucibleBlockEntity;
import com.beepsterr.resourcegenerators.registry.ModFluids;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.Mth;

/**
 * Draws the pool of Liquid Resonance inside the crucible's bowl, rising with the tank, so how full
 * it is reads at a glance without opening the GUI.
 */
public class CrystalCrucibleRenderer implements BlockEntityRenderer<CrystalCrucibleBlockEntity> {

    public static final ModelResourceLocation FLUID =
            ModelResourceLocation.standalone(BeepsResourceGenerators.rl("block/crucible_fluid"));

    /** The bowl's inner floor, matching the model (the fill sits between y7 and y14). */
    private static final float FLOOR = 7.0f / 16.0f;

    public CrystalCrucibleRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CrystalCrucibleBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        float fill = Mth.clamp(be.fillFraction(), 0f, 1f);

        // The pool: the fill model spans the whole bowl, scaled down from the floor to the level the
        // tank is actually at. Full-bright so it glows like the crystals it came from.
        if (fill > 0.001f) {
            ModelBlockRenderer renderer = mc.getBlockRenderer().getModelRenderer();
            BakedModel fluid = mc.getModelManager().getModel(FLUID);
            VertexConsumer vc = buffers.getBuffer(RenderType.cutout());
            int color = ModFluids.RESONANCE_COLOR;
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;
            pose.pushPose();
            pose.translate(0.0f, FLOOR, 0.0f);
            pose.scale(1.0f, fill, 1.0f);
            pose.translate(0.0f, -FLOOR, 0.0f);
            renderer.renderModel(pose.last(), vc, null, fluid, r, g, b, LightTexture.FULL_BRIGHT, overlay);
            pose.popPose();
        }

    }
}
