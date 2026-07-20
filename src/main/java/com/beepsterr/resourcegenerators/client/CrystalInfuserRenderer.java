package com.beepsterr.resourcegenerators.client;

import com.beepsterr.resourcegenerators.block.CrystalInfuserBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Draws the two items "in use" inside the infuser: the crystal being infused sitting upright on the
 * base, and the material laid flat on the grate above it. Both come from the (client-synced)
 * block entity inventory; the crystal renders tinted by its resource via the item colour handler.
 */
public class CrystalInfuserRenderer implements BlockEntityRenderer<CrystalInfuserBlockEntity> {

    private static final float CRYSTAL_Y = 5.0f / 16.0f;
    private static final float CRYSTAL_SCALE = 0.8f;
    private static final float MATERIAL_Y = 14.0f / 16.0f;
    private static final float MATERIAL_SCALE = 0.6f;

    public CrystalInfuserRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CrystalInfuserBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        IItemHandler inv = be.getInventory();
        Level level = be.getLevel();
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

        // Crystal, upright on the base.
        ItemStack crystal = inv.getStackInSlot(CrystalInfuserBlockEntity.SLOT_CRYSTAL);
        if (!crystal.isEmpty()) {
            pose.pushPose();
            pose.translate(0.5, CRYSTAL_Y, 0.5);
            pose.scale(CRYSTAL_SCALE, CRYSTAL_SCALE, CRYSTAL_SCALE);
            itemRenderer.renderStatic(crystal, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
                    pose, buffers, level, 0);
            pose.popPose();
        }

        // Material, laid flat on the grate.
        ItemStack material = inv.getStackInSlot(CrystalInfuserBlockEntity.SLOT_MATERIAL);
        if (!material.isEmpty()) {
            pose.pushPose();
            pose.translate(0.5, MATERIAL_Y, 0.5);
            pose.mulPose(Axis.XP.rotationDegrees(90.0f)); // lie flat, facing up
            pose.scale(MATERIAL_SCALE, MATERIAL_SCALE, MATERIAL_SCALE);
            itemRenderer.renderStatic(material, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
                    pose, buffers, level, 0);
            pose.popPose();
        }
    }
}
