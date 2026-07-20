package com.beepsterr.resourcegenerators.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The resonator's shockwave: a flat, ground-parallel ring that expands out of the machine and fades.
 * Its radius grows at the same rate the pulse timing uses, so the visible front reaches each crystal
 * right as that crystal plings. Rendered as a single horizontal quad (not a camera billboard).
 */
public class ResonanceRingParticle extends TextureSheetParticle {

    /** Matches WAVE_TICKS * WAVE_SPEED on the resonator so the ring stays in sync with the plings. */
    private static final float MAX_RADIUS = 7.2f;

    protected ResonanceRingParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        this.lifetime = 12;
        this.gravity = 0.0f;
        this.hasPhysics = false;
        this.rCol = 1.0f;
        this.gCol = 1.0f;
        this.bCol = 1.0f;
        setSprite(sprites.get(0, 1));
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        float t = Math.min(1.0f, (this.age + partialTicks) / this.lifetime);
        float radius = MAX_RADIUS * t;
        int alpha = (int) ((1.0f - t) * 0.8f * 255.0f);
        if (alpha <= 0) {
            return;
        }

        Vec3 cam = camera.getPosition();
        float cx = (float) (Mth.lerp(partialTicks, this.xo, this.x) - cam.x());
        float cy = (float) (Mth.lerp(partialTicks, this.yo, this.y) - cam.y()) + 0.05f;
        float cz = (float) (Mth.lerp(partialTicks, this.zo, this.z) - cam.z());

        float u0 = getU0(), u1 = getU1(), v0 = getV0(), v1 = getV1();
        int r = (int) (this.rCol * 255.0f), g = (int) (this.gCol * 255.0f), b = (int) (this.bCol * 255.0f);
        int light = LightTexture.FULL_BRIGHT;

        // Horizontal quad centred on the particle; particles aren't back-face culled, so winding is moot.
        buffer.addVertex(cx - radius, cy, cz - radius).setUv(u0, v0).setColor(r, g, b, alpha).setLight(light);
        buffer.addVertex(cx - radius, cy, cz + radius).setUv(u0, v1).setColor(r, g, b, alpha).setLight(light);
        buffer.addVertex(cx + radius, cy, cz + radius).setUv(u1, v1).setColor(r, g, b, alpha).setLight(light);
        buffer.addVertex(cx + radius, cy, cz - radius).setUv(u1, v0).setColor(r, g, b, alpha).setLight(light);
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z, double dx, double dy, double dz) {
            return new ResonanceRingParticle(level, x, y, z, sprites);
        }
    }
}
