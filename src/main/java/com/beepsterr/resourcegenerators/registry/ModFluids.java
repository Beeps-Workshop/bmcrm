package com.beepsterr.resourcegenerators.registry;

import com.beepsterr.resourcegenerators.BeepsResourceGenerators;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Liquid Resonance: the fluid a crystal melts down into, and the currency the Former spends to make
 * its higher tiers. Registered as a real fluid (rather than an internal number) so tanks, pipes and
 * pumps from any other mod can move it without compat code.
 */
public final class ModFluids {

    private ModFluids() {}

    /** One bucket's worth, and the unit every capacity in the mod is expressed in. */
    public static final int BUCKET = 1000;

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, BeepsResourceGenerators.MOD_ID);

    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, BeepsResourceGenerators.MOD_ID);

    /** Tint applied to the (placeholder water) fluid textures; also the crystal's charge-bar colour. */
    public static final int RESONANCE_COLOR = 0xFF5FD8E8;

    public static final DeferredHolder<FluidType, FluidType> LIQUID_RESONANCE_TYPE =
            FLUID_TYPES.register("liquid_resonance", () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid.bmcrm.liquid_resonance")
                    .density(1400)      // heavier and slower than water — it pours like a syrup
                    .viscosity(2500)
                    .lightLevel(7)      // it glows faintly, like the crystals it came from
                    .canConvertToSource(false)
                    .canSwim(true)
                    .canDrown(true)
                    .supportsBoating(false)));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> LIQUID_RESONANCE =
            FLUIDS.register("liquid_resonance", () -> new BaseFlowingFluid.Source(properties()));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_LIQUID_RESONANCE =
            FLUIDS.register("flowing_liquid_resonance", () -> new BaseFlowingFluid.Flowing(properties()));

    /** The in-world block form. Registered here (not ModBlocks) to keep the fluid's parts together. */
    public static final DeferredBlock<LiquidBlock> LIQUID_RESONANCE_BLOCK =
            ModBlocks.REGISTER.register("liquid_resonance",
                    () -> new LiquidBlock(LIQUID_RESONANCE.get(), BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_CYAN)
                            .replaceable()
                            .noCollission()
                            .strength(100.0f)
                            .pushReaction(PushReaction.DESTROY)
                            .noLootTable()
                            .liquid()
                            .lightLevel(s -> 7)));

    /**
     * Shared properties for the source/flowing pair. Built fresh per fluid (the properties object is
     * consumed by the fluid it's handed to) and wired with suppliers, since the bucket and block are
     * registered after the fluids themselves.
     */
    private static BaseFlowingFluid.Properties properties() {
        return new BaseFlowingFluid.Properties(LIQUID_RESONANCE_TYPE, LIQUID_RESONANCE, FLOWING_LIQUID_RESONANCE)
                .bucket(ModItems.LIQUID_RESONANCE_BUCKET)
                .block(LIQUID_RESONANCE_BLOCK)
                .slopeFindDistance(2)
                .levelDecreasePerBlock(2)
                .tickRate(10);
    }
}
