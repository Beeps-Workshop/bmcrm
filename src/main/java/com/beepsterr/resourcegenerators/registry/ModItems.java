package com.beepsterr.resourcegenerators.registry;

import com.beepsterr.resourcegenerators.BeepsResourceGenerators;
import com.beepsterr.resourcegenerators.item.CrystalItem;
import com.beepsterr.resourcegenerators.item.TuningForkItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Item registrations. */
public final class ModItems {

    private ModItems() {}

    public static final DeferredRegister.Items REGISTER =
            DeferredRegister.createItems(BeepsResourceGenerators.MOD_ID);

    /**
     * The one crystal item; its tier/resource identity is a data component. Unstackable — each
     * crystal carries unique per-item state (tier, resource, enchantments, in-progress infusion
     * shown on the durability bar), and stacking would let one batch of infuser material complete
     * a whole stack at once.
     */
    public static final DeferredItem<Item> CRYSTAL =
            REGISTER.registerItem("crystal",
                    props -> new CrystalItem(ModBlocks.PLACED_CRYSTAL.get(), props),
                    new Item.Properties().stacksTo(1));

    /** BlockItem for the Crystal Former. */
    public static final DeferredItem<BlockItem> CRYSTAL_FORMER =
            REGISTER.registerSimpleBlockItem("crystal_former", ModBlocks.CRYSTAL_FORMER);

    /** BlockItem for the Crystal Infuser. */
    public static final DeferredItem<BlockItem> CRYSTAL_INFUSER =
            REGISTER.registerSimpleBlockItem("crystal_infuser", ModBlocks.CRYSTAL_INFUSER);

    /** BlockItem for the Resonator. */
    public static final DeferredItem<BlockItem> RESONATOR =
            REGISTER.registerSimpleBlockItem("resonator", ModBlocks.RESONATOR);

    /** BlockItem for the Silk Touch Modulator. */
    public static final DeferredItem<BlockItem> SILK_TOUCH_MODULATOR =
            REGISTER.registerSimpleBlockItem("silk_touch_modulator", ModBlocks.SILK_TOUCH_MODULATOR);

    /** BlockItem for the Fortune Modulator. */
    public static final DeferredItem<BlockItem> FORTUNE_MODULATOR =
            REGISTER.registerSimpleBlockItem("fortune_modulator", ModBlocks.FORTUNE_MODULATOR);

    /** Tuning Fork: a diagnostic wrench that toggles area overlays (see {@link TuningForkItem}). */
    public static final DeferredItem<Item> TUNING_FORK =
            REGISTER.registerItem("tuning_fork", TuningForkItem::new, new Item.Properties().stacksTo(1));
}
