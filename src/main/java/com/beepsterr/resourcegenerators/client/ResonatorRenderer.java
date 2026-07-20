package com.beepsterr.resourcegenerators.client;

import com.beepsterr.resourcegenerators.BeepsResourceGenerators;
import com.beepsterr.resourcegenerators.block.ResonatorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;

/**
 * Draws the resonator's animated pieces on top of its (static) block model: a fuel tank that fills
 * with the work cycle and a ring that spins — slow while charging, spiking on each generation cycle
 * then winding down. All motion is driven by the cycle-clock synced on {@link ResonatorBlockEntity}.
 *
 * <p>PLACEHOLDER: the ring and tank float just above the block so they're visible over the current
 * solid resonator model. Once the real Blockbench model exists (with a hollow tank shell) these
 * pieces move down into it — only the positions here change, not the animation logic.
 */
public class ResonatorRenderer implements BlockEntityRenderer<ResonatorBlockEntity> {

    public static final ModelResourceLocation RING =
            ModelResourceLocation.standalone(BeepsResourceGenerators.rl("block/resonator_ring"));
    public static final ModelResourceLocation FLUID =
            ModelResourceLocation.standalone(BeepsResourceGenerators.rl("block/resonator_fluid"));

    /** Placeholder lift so the pieces sit on top of the (opaque) resonator block. */
    private static final float FLOAT = 1.0f;

    public ResonatorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ResonatorBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        ModelBlockRenderer renderer = mc.getBlockRenderer().getModelRenderer();

        // Fuel tank: authored full-height from y=0, scaled vertically by the fill fraction (grows up).
        float fill = be.fillFraction(partialTick);
        if (fill > 0.001f) {
            pose.pushPose();
            pose.translate(0.0f, FLOAT, 0.0f);
            pose.scale(1.0f, fill, 1.0f);
            BakedModel fluid = mc.getModelManager().getModel(FLUID);
            VertexConsumer vc = buffers.getBuffer(RenderType.translucent());
            renderer.renderModel(pose.last(), vc, null, fluid, 1.0f, 1.0f, 1.0f, light, overlay);
            pose.popPose();
        }

        // Ring: spins around the block's vertical centre axis.
        float angle = be.advanceSpin(partialTick);
        pose.pushPose();
        pose.translate(0.0f, FLOAT, 0.0f);
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
