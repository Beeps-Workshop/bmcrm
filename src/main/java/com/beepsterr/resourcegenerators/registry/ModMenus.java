package com.beepsterr.resourcegenerators.registry;

import com.beepsterr.resourcegenerators.BeepsResourceGenerators;
import com.beepsterr.resourcegenerators.block.ResonatorMenu;
import com.beepsterr.resourcegenerators.block.CrystalFormerMenu;
import com.beepsterr.resourcegenerators.block.CrystalInfuserMenu;
import com.beepsterr.resourcegenerators.block.CrystalCrucibleMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** Menu (container) type registrations. */
public final class ModMenus {

    private ModMenus() {}

    public static final DeferredRegister<MenuType<?>> REGISTER =
            DeferredRegister.create(Registries.MENU, BeepsResourceGenerators.MOD_ID);

    public static final Supplier<MenuType<CrystalFormerMenu>> CRYSTAL_FORMER =
            REGISTER.register("crystal_former", () -> IMenuTypeExtension.create(CrystalFormerMenu::new));

    public static final Supplier<MenuType<CrystalInfuserMenu>> CRYSTAL_INFUSER =
            REGISTER.register("crystal_infuser", () -> IMenuTypeExtension.create(CrystalInfuserMenu::new));

    public static final Supplier<MenuType<CrystalCrucibleMenu>> CRYSTAL_CRUCIBLE =
            REGISTER.register("crystal_crucible", () -> IMenuTypeExtension.create(CrystalCrucibleMenu::new));

    public static final Supplier<MenuType<ResonatorMenu>> RESONATOR =
            REGISTER.register("resonator", () -> IMenuTypeExtension.create(ResonatorMenu::new));
}
