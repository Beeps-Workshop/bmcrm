package com.beepsterr.resourcegenerators.client;

import com.beepsterr.resourcegenerators.BeepsResourceGenerators;
import com.beepsterr.resourcegenerators.block.CrystalFormerBlockEntity;
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
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the former's "in use" contents inside its glass cylinder: while forming, a fill column that
 * rises with progress and is tinted the colour of the tier being made; once a crystal is finished
 * and sitting in the output, that crystal hangs upside-down in the cylinder instead.
 */
public class CrystalFormerRenderer implements BlockEntityRenderer<CrystalFormerBlockEntity> {

    public static final ModelResourceLocation FLUID =
            ModelResourceLocation.standalone(BeepsResourceGenerators.rl("block/former_fluid"));

    /** The fill model's base height (y=6px) — it scales up from here. */
    private static final float FILL_BASE = 6.0f / 16.0f;

    public CrystalFormerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CrystalFormerBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();

        // A finished crystal takes priority: hang it upside-down in the cylinder until it's collected.
        ItemStack output = be.getInventory().getStackInSlot(CrystalFormerBlockEntity.SLOT_OUTPUT);
        if (!output.isEmpty()) {
            ItemRenderer itemRenderer = mc.getItemRenderer();
            pose.pushPose();
            pose.translate(0.5, 11.0f / 16.0f, 0.5);
            pose.mulPose(Axis.ZP.rotationDegrees(180.0f)); // upside down
            pose.scale(0.6f, 0.6f, 0.6f);
            itemRenderer.renderStatic(output, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
                    pose, buffers, be.getLevel(), 0);
            pose.popPose();
            return;
        }

        // Otherwise, the fill column rising with progress, tinted the forming tier's colour and glowing.
        float fill = be.fillFraction(partialTick);
        int color = be.getFormingColor();
        if (fill > 0.001f && color >= 0) {
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;
            ModelBlockRenderer renderer = mc.getBlockRenderer().getModelRenderer();
            BakedModel fluid = mc.getModelManager().getModel(FLUID);
            VertexConsumer vc = buffers.getBuffer(RenderType.cutout());
            pose.pushPose();
            pose.translate(0.0f, FILL_BASE, 0.0f);
            pose.scale(1.0f, fill, 1.0f);
            pose.translate(0.0f, -FILL_BASE, 0.0f);
            renderer.renderModel(pose.last(), vc, null, fluid, r, g, b, LightTexture.FULL_BRIGHT, overlay);
            pose.popPose();
        }
    }
}
