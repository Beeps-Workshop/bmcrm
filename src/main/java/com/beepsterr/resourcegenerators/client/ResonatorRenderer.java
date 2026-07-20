package com.beepsterr.resourcegenerators.client;

import com.beepsterr.resourcegenerators.BeepsResourceGenerators;
import com.beepsterr.resourcegenerators.block.ResonatorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;

/**
 * Draws the resonator's animated pieces on top of its (static) block model: the gold fuel tank that
 * fills with the work cycle and the spinner ring that rotates — slow while charging, spiking on each
 * generation cycle then winding down. Motion is driven by the cycle-clock synced on
 * {@link ResonatorBlockEntity}. The `body` group is the block's own model; `ring` and `fluid` are the
 * standalone models drawn here, positioned exactly as authored (no offset).
 */
public class ResonatorRenderer implements BlockEntityRenderer<ResonatorBlockEntity> {

    public static final ModelResourceLocation RING =
            ModelResourceLocation.standalone(BeepsResourceGenerators.rl("block/resonator_ring"));
    public static final ModelResourceLocation FLUID =
            ModelResourceLocation.standalone(BeepsResourceGenerators.rl("block/resonator_fluid"));

    /** The fluid model's base height (y=4px) — the tank scales up from here as it fills. */
    private static final float FLUID_BASE = 4.0f / 16.0f;

    public ResonatorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ResonatorBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        ModelBlockRenderer renderer = mc.getBlockRenderer().getModelRenderer();

        // Fuel tank: authored full (y4..13), scaled vertically about its base by the fill fraction.
        // Rendered full-bright so the fluid glows regardless of the dark tank interior.
        float fill = be.fillFraction(partialTick);
        if (fill > 0.001f) {
            pose.pushPose();
            pose.translate(0.0f, FLUID_BASE, 0.0f);
            pose.scale(1.0f, fill, 1.0f);
            pose.translate(0.0f, -FLUID_BASE, 0.0f);
            BakedModel fluid = mc.getModelManager().getModel(FLUID);
            VertexConsumer vc = buffers.getBuffer(RenderType.cutout());
            renderer.renderModel(pose.last(), vc, null, fluid, 1.0f, 1.0f, 1.0f,
                    LightTexture.FULL_BRIGHT, overlay);
            pose.popPose();
        }

        // Spinner ring: spins around the block's vertical centre axis (X8/Z8).
        float angle = be.advanceSpin(partialTick);
        pose.pushPose();
        pose.translate(0.5f, 0.0f, 0.5f);
        pose.mulPose(Axis.YP.rotationDegrees(angle));
        pose.translate(-0.5f, 0.0f, -0.5f);
        BakedModel ring = mc.getModelManager().getModel(RING);
        VertexConsumer vc = buffers.getBuffer(RenderType.cutout());
        renderer.renderModel(pose.last(), vc, null, ring, 1.0f, 1.0f, 1.0f, light, overlay);
        pose.popPose();
    }

    /** Cap the effect's render distance so a wall of resonators can't tank framerate. */
    @Override
    public int getViewDistance() {
        return 48;
    }
}
