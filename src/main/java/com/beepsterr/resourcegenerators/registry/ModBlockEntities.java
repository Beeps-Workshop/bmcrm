package com.beepsterr.resourcegenerators.registry;

import com.beepsterr.resourcegenerators.BeepsResourceGenerators;
import com.beepsterr.resourcegenerators.block.ResonatorBlockEntity;
import com.beepsterr.resourcegenerators.block.CrystalFormerBlockEntity;
import com.beepsterr.resourcegenerators.block.CrystalInfuserBlockEntity;
import com.beepsterr.resourcegenerators.block.ModulatorBlockEntity;
import com.beepsterr.resourcegenerators.block.PlacedCrystalBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** Block entity type registrations. */
public final class ModBlockEntities {

    private ModBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> REGISTER =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BeepsResourceGenerators.MOD_ID);

    public static final Supplier<BlockEntityType<CrystalFormerBlockEntity>> CRYSTAL_FORMER =
            REGISTER.register("crystal_former", () -> BlockEntityType.Builder
                    .of(CrystalFormerBlockEntity::new, ModBlocks.CRYSTAL_FORMER.get())
                    .build(null));

    public static final Supplier<BlockEntityType<CrystalInfuserBlockEntity>> CRYSTAL_INFUSER =
            REGISTER.register("crystal_infuser", () -> BlockEntityType.Builder
                    .of(CrystalInfuserBlockEntity::new, ModBlocks.CRYSTAL_INFUSER.get())
                    .build(null));

    public static final Supplier<BlockEntityType<PlacedCrystalBlockEntity>> PLACED_CRYSTAL =
            REGISTER.register("placed_crystal", () -> BlockEntityType.Builder
                    .of(PlacedCrystalBlockEntity::new, ModBlocks.PLACED_CRYSTAL.get())
                    .build(null));

    public static final Supplier<BlockEntityType<ResonatorBlockEntity>> RESONATOR =
            REGISTER.register("resonator", () -> BlockEntityType.Builder
                    .of(ResonatorBlockEntity::new, ModBlocks.RESONATOR.get())
                    .build(null));

    /** One BE type shared by all Modulator blocks (add more blocks to the .of(...) as they're added). */
    public static final Supplier<BlockEntityType<ModulatorBlockEntity>> MODULATOR =
            REGISTER.register("modulator", () -> BlockEntityType.Builder
                    .of(ModulatorBlockEntity::new, ModBlocks.SILK_TOUCH_MODULATOR.get())
                    .build(null));
}
