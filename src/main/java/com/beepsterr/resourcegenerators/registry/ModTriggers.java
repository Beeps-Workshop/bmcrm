package com.beepsterr.resourcegenerators.registry;

import com.beepsterr.resourcegenerators.BeepsResourceGenerators;
import com.beepsterr.resourcegenerators.advancement.ResonatorNearbyTrigger;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** Custom advancement criterion triggers. */
public final class ModTriggers {

    private ModTriggers() {}

    public static final DeferredRegister<CriterionTrigger<?>> REGISTER =
            DeferredRegister.create(Registries.TRIGGER_TYPE, BeepsResourceGenerators.MOD_ID);

    /** Player is near a resonator that just fired a cycle; carries its crystal/modulator counts. */
    public static final Supplier<ResonatorNearbyTrigger> RESONATOR_NEARBY =
            REGISTER.register("resonator_nearby", ResonatorNearbyTrigger::new);
}
