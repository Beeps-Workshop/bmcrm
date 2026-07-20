package com.beepsterr.resourcegenerators.registry;

import com.beepsterr.resourcegenerators.BeepsResourceGenerators;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** Custom particles. */
public final class ModParticles {

    private ModParticles() {}

    public static final DeferredRegister<ParticleType<?>> REGISTER =
            DeferredRegister.create(Registries.PARTICLE_TYPE, BeepsResourceGenerators.MOD_ID);

    /** The resonator's shockwave: a flat ring that expands out of the machine on each work cycle. */
    public static final Supplier<SimpleParticleType> RESONANCE_RING =
            REGISTER.register("resonance_ring", () -> new SimpleParticleType(false));
}
