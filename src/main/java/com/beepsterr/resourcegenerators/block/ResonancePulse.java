package com.beepsterr.resourcegenerators.block;

import com.beepsterr.resourcegenerators.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-side scheduler for the resonator's resonance pulse — a purely presentational wave that sweeps
 * out on each work cycle. Every crystal the wave reaches "plings" (pitched up with distance so the
 * sweep reads as an arpeggio), then generators fire a resource burst a beat later. Gameplay is already
 * resolved instantly in {@link ResonatorBlockEntity#doWork}; this only schedules the visuals/sound and
 * drains them on later ticks. State is transient — nothing here is saved.
 */
public class ResonancePulse {

    private static final float WAVE_SPEED = 0.6f;        // blocks the wave front travels per tick
    private static final int WAVE_TICKS = 12;            // how long the visible ring keeps expanding;
                                                         // ResonanceRingParticle's lifetime tracks this
    private static final int BURST_GAP = 3;              // ticks between a crystal's pling and its burst
    private static final float PITCH_MIN = 0.7f;
    private static final float PITCH_MAX = 2.0f;
    private static final float PITCH_PER_BLOCK = 0.14f;  // farther crystals pling higher -> arpeggio sweep
    private static final float PLING_VOLUME = 0.3f;

    private enum Kind { PLING, BURST }

    private record Effect(int fireTick, Kind kind, BlockPos pos, ItemStack sample, int color, float pitch) {}

    /** Transient queue of scheduled effects, drained in {@link #tick}; not saved. */
    private final List<Effect> pending = new ArrayList<>();

    /** Kick off the shockwave — one self-expanding ring particle from the resonator's core. */
    public void shockwave(ServerLevel level, BlockPos center) {
        level.sendParticles(ModParticles.RESONANCE_RING.get(),
                center.getX() + 0.5, center.getY() + 0.7, center.getZ() + 0.5, 1, 0.0, 0.0, 0.0, 0.0);
    }

    /** Schedule a crystal's "pling" — pitched up with distance so the sweep reads as an arpeggio. */
    public void schedulePling(int nowTick, BlockPos center, BlockPos crystal) {
        double dist = Math.sqrt(center.distSqr(crystal));
        float pitch = Mth.clamp(PITCH_MIN + (float) dist * PITCH_PER_BLOCK, PITCH_MIN, PITCH_MAX);
        pending.add(new Effect(nowTick + hitDelay(center, crystal), Kind.PLING, crystal, ItemStack.EMPTY, 0, pitch));
    }

    /** Schedule a crystal's resource burst (colored dust + a stream to the resonator), a beat after its pling. */
    public void scheduleBurst(int nowTick, BlockPos center, BlockPos crystal, ItemStack sample, int color) {
        pending.add(new Effect(nowTick + hitDelay(center, crystal) + BURST_GAP, Kind.BURST, crystal, sample, color, 0f));
    }

    /** Drain any scheduled effects whose moment has arrived. Bursts fling their items toward {@code resonator}. */
    public void tick(ServerLevel level, int nowTick, BlockPos resonator) {
        if (pending.isEmpty()) {
            return;
        }
        pending.removeIf(ev -> {
            if (ev.fireTick() - nowTick <= 0) {
                fire(level, ev, resonator);
                return true;
            }
            return false;
        });
    }

    /** Fire one scheduled effect (server-side, broadcast to nearby players). */
    private static void fire(ServerLevel level, Effect ev, BlockPos resonator) {
        switch (ev.kind()) {
            case PLING -> {
                level.playSound(null, ev.pos(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS,
                        PLING_VOLUME, ev.pitch());
                level.sendParticles(ParticleTypes.END_ROD,
                        ev.pos().getX() + 0.5, ev.pos().getY() + 0.7, ev.pos().getZ() + 0.5,
                        2, 0.06, 0.06, 0.06, 0.005);
            }
            case BURST -> {
                spawnGenerateParticles(level, ev.pos(), ev.color());
                if (!ev.sample().isEmpty()) {
                    spawnFlingParticles(level, ev.pos(), resonator, ev.sample());
                }
            }
        }
    }

    /** Ticks for the pulse front to reach a crystal (≥1), from its distance to the resonator. */
    private static int hitDelay(BlockPos center, BlockPos crystal) {
        double dist = Math.sqrt(center.distSqr(crystal));
        return Math.max(1, Math.round((float) (dist / WAVE_SPEED)));
    }

    /** A small colored dust burst at a crystal that just generated. */
    private static void spawnGenerateParticles(ServerLevel level, BlockPos crystal, int rgb) {
        Vector3f color = new Vector3f(
                ((rgb >> 16) & 0xFF) / 255.0f,
                ((rgb >> 8) & 0xFF) / 255.0f,
                (rgb & 0xFF) / 255.0f);
        level.sendParticles(new DustParticleOptions(color, 1.2f),
                crystal.getX() + 0.5, crystal.getY() + 0.6, crystal.getZ() + 0.5,
                6, 0.22, 0.28, 0.22, 0.0);
    }

    /** A short trail of item-break particles from the crystal toward the resonator. */
    private static void spawnFlingParticles(ServerLevel level, BlockPos crystal, BlockPos resonator, ItemStack stack) {
        ItemParticleOption particle = new ItemParticleOption(ParticleTypes.ITEM, stack);
        double fx = crystal.getX() + 0.5, fy = crystal.getY() + 0.5, fz = crystal.getZ() + 0.5;
        double tx = resonator.getX() + 0.5, ty = resonator.getY() + 0.5, tz = resonator.getZ() + 0.5;
        int steps = 6;
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            level.sendParticles(particle,
                    Mth.lerp(t, fx, tx), Mth.lerp(t, fy, ty), Mth.lerp(t, fz, tz),
                    1, 0.02, 0.02, 0.02, 0.0);
        }
    }
}
