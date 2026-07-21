package com.beepsterr.resourcegenerators.client;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

/**
 * Client-side render/animation state for a resonator: the fuel-tank fill and the spinner ring
 * angle drawn by {@link ResonatorRenderer}. It interpolates the fill from the last cycle-clock sync
 * so the tank climbs smoothly, and integrates the (variable) spin speed over time.
 *
 * <p>Purely presentational. An instance lives on the block entity, but only the client ever feeds it
 * ({@link #read} is driven by the update packet) and reads from it. It touches no client-only classes,
 * so holding one on the server side is harmless — it simply never gets fed.
 */
public class ResonatorAnimator {

    // Spin curve (degrees/tick). Slow most of the charge, then an ease-in "wind-up" that accelerates
    // into a sharp spike on each cycle, which then decays away.
    private static final float CHARGE_MIN_SPIN = 2.0f;
    private static final float CHARGE_RISE = 13.0f;
    private static final float WINDUP_EXP = 2.5f;  // >1 keeps it slow early, ramping hard near full
    private static final float SPIKE_MAX_SPIN = 46.0f;
    private static final float SPIKE_TAU = 5.0f;   // decay time constant
    private static final float SPIKE_WINDOW = 40f; // ticks the spike is applied for after a cycle

    // Synced cycle-clock view (fed from the update packet).
    private int work = 0;
    private int interval = 0;
    private boolean active = false;
    private long lastCycle = Long.MIN_VALUE;
    private long syncGameTime = 0L;

    // Local render accumulators.
    private float spin = 0f;
    private float lastT = Float.NaN;

    /** Load synced cycle-clock state from an update tag; {@code gameTime} stamps when it arrived. */
    public void read(CompoundTag tag, long gameTime) {
        this.work = tag.getInt("Work");
        this.interval = tag.getInt("Interval");
        this.active = tag.getBoolean("Active");
        this.lastCycle = tag.contains("LastCycle") ? tag.getLong("LastCycle") : Long.MIN_VALUE;
        this.syncGameTime = gameTime;
    }

    /** 0..1 fill of the fuel tank, interpolated from the last sync so it climbs smoothly. */
    public float fillFraction(long gameTime, float partialTick) {
        if (interval <= 0) {
            return 0f;
        }
        float progress = work;
        if (active) {
            float elapsed = ((float) gameTime + partialTick) - syncGameTime;
            progress = Math.min(interval, work + Math.max(0f, elapsed));
        }
        return Mth.clamp(progress / (float) interval, 0f, 1f);
    }

    /** Advance and return the ring's spin angle, integrating the (variable) spin speed over time. */
    public float advanceSpin(long gameTime, float partialTick) {
        float now = (float) gameTime + partialTick;
        float dt = Float.isNaN(lastT) ? 0f : now - lastT;
        lastT = now;
        if (dt < 0f || dt > 5f) { // paused, rewound, or a big gap (chunk reload) — don't lurch
            dt = 0f;
        }
        spin = (spin + spinSpeed(now, fillFraction(gameTime, partialTick)) * dt) % 360f;
        return spin;
    }

    /** The last computed spin angle, without advancing — used when the level isn't available yet. */
    public float currentSpin() {
        return spin;
    }

    /** Current ring spin speed (deg/tick) — gentle rise with fill, sharp spike after each cycle. */
    private float spinSpeed(float now, float fill) {
        float windup = (float) Math.pow(fill, WINDUP_EXP);
        float charge = CHARGE_MIN_SPIN + windup * CHARGE_RISE;
        if (lastCycle != Long.MIN_VALUE) {
            float since = now - lastCycle;
            if (since >= 0f && since < SPIKE_WINDOW) {
                float k = (float) Math.exp(-since / SPIKE_TAU);
                return charge + (SPIKE_MAX_SPIN - charge) * k;
            }
        }
        return charge;
    }
}
