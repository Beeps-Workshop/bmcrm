package com.beepsterr.resourcegenerators.registry;

import com.beepsterr.resourcegenerators.BeepsResourceGenerators;
import com.beepsterr.resourcegenerators.crystal.CrystalData;
import com.beepsterr.resourcegenerators.crystal.CrystalInfusion;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Custom data components. */
public final class ModDataComponents {

    private ModDataComponents() {}

    public static final DeferredRegister.DataComponents REGISTER =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, BeepsResourceGenerators.MOD_ID);

    /** Tier + resource identity carried by a crystal item stack. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CrystalData>> CRYSTAL_DATA =
            REGISTER.registerComponentType("crystal_data", builder -> builder
                    .persistent(CrystalData.CODEC)
                    .networkSynchronized(CrystalData.STREAM_CODEC));

    /** In-progress infusion state on a blank crystal (drives the durability bar). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CrystalInfusion>> CRYSTAL_INFUSION =
            REGISTER.registerComponentType("crystal_infusion", builder -> builder
                    .persistent(CrystalInfusion.CODEC)
                    .networkSynchronized(CrystalInfusion.STREAM_CODEC));
}
