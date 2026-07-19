package com.beepsterr.resourcegenerators.client;

import com.beepsterr.resourcegenerators.BeepsResourceGenerators;
import com.beepsterr.resourcegenerators.block.AreaPreview;
import com.beepsterr.resourcegenerators.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * Client-side state + rendering for the Tuning Fork's area overlays. Toggled boxes persist until
 * toggled off and only draw while the fork is held (main or off hand). A crystal->resonator "ping"
 * is a one-shot particle trail (see {@link #pingResonator}).
 */
@EventBusSubscriber(modid = BeepsResourceGenerators.MOD_ID, value = Dist.CLIENT)
public final class TuningForkOverlay {

    private TuningForkOverlay() {}

    /** Persistently-shown block positions (area overlays). */
    private static final Set<BlockPos> shown = new HashSet<>();

    /** Toggle a persistent overlay for a block. Returns true if it is now shown. */
    public static boolean toggle(BlockPos pos) {
        BlockPos p = pos.immutable();
        if (shown.remove(p)) {
            return false;
        }
        shown.add(p);
        return true;
    }

    /** One-shot particle trail from a crystal to the resonator that owns it, plus a burst at the end. */
    public static void pingResonator(Level level, BlockPos crystal, BlockPos resonator) {
        double fx = crystal.getX() + 0.5, fy = crystal.getY() + 0.5, fz = crystal.getZ() + 0.5;
        double tx = resonator.getX() + 0.5, ty = resonator.getY() + 0.5, tz = resonator.getZ() + 0.5;
        int steps = 20;
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            level.addParticle(ParticleTypes.END_ROD,
                    Mth.lerp(t, fx, tx), Mth.lerp(t, fy, ty), Mth.lerp(t, fz, tz),
                    0.0, 0.0, 0.0);
        }
        for (int i = 0; i < 14; i++) {
            level.addParticle(ParticleTypes.END_ROD, tx, ty + 0.1, tz,
                    (level.random.nextDouble() - 0.5) * 0.08,
                    level.random.nextDouble() * 0.12,
                    (level.random.nextDouble() - 0.5) * 0.08);
        }
    }

    private static boolean holdingFork(Player player) {
        return player.getMainHandItem().is(ModItems.TUNING_FORK.get())
                || player.getOffhandItem().is(ModItems.TUNING_FORK.get());
    }

    @SubscribeEvent
    static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || shown.isEmpty() || !holdingFork(mc.player)) {
            return;
        }

        PoseStack pose = event.getPoseStack();
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());

        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        for (BlockPos p : shown) {
            BlockEntity be = mc.level.getBlockEntity(p);
            if (be instanceof AreaPreview preview) {
                int color = preview.getPreviewColor();
                for (AABB box : preview.getPreviewBoxes()) {
                    drawBox(pose, lines, box, color);
                }
            }
        }
        pose.popPose();
        buffers.endBatch(RenderType.lines());
    }

    private static void drawBox(PoseStack pose, VertexConsumer lines, AABB box, int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255.0f;
        float g = ((rgb >> 8) & 0xFF) / 255.0f;
        float b = (rgb & 0xFF) / 255.0f;
        LevelRenderer.renderLineBox(pose, lines, box, r, g, b, 0.9f);
    }
}
