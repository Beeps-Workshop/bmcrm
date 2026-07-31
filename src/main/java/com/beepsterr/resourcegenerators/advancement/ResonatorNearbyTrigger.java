package com.beepsterr.resourcegenerators.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Fires for every player near a resonator that has just completed a work cycle, carrying how much
 * that resonator is currently driving: placed crystals in range that hold a resource, and modulators
 * in range that carry a modulation.
 *
 * <p>Both counts are what the resonator itself caches, so "in range" here means exactly what it means
 * to the machine — a modulator counts whether or not its footprint actually covers a crystal, and the
 * inert {@code modulator_base} never counts because it has no block entity to read a modulation from.
 */
public class ResonatorNearbyTrigger extends SimpleCriterionTrigger<ResonatorNearbyTrigger.Instance> {

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public void trigger(ServerPlayer player, int crystals, int modulators) {
        this.trigger(player, instance -> instance.matches(crystals, modulators));
    }

    /** Thresholds are inclusive minimums; both default to 0 so either may be omitted. */
    public record Instance(Optional<ContextAwarePredicate> player, int minCrystals, int minModulators)
            implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                Codec.INT.optionalFieldOf("min_crystals", 0).forGetter(Instance::minCrystals),
                Codec.INT.optionalFieldOf("min_modulators", 0).forGetter(Instance::minModulators)
        ).apply(builder, Instance::new));

        public boolean matches(int crystals, int modulators) {
            return crystals >= minCrystals && modulators >= minModulators;
        }
    }
}
