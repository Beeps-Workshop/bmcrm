package com.beepsterr.resourcegenerators.registry;

import com.beepsterr.resourcegenerators.BeepsResourceGenerators;
import com.beepsterr.resourcegenerators.item.CrystalItem;
import com.beepsterr.resourcegenerators.item.TuningForkItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
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

    /** BlockItem for the Crystal Crucible. */
    public static final DeferredItem<BlockItem> CRYSTAL_CRUCIBLE =
            REGISTER.registerSimpleBlockItem("crystal_crucible", ModBlocks.CRYSTAL_CRUCIBLE);

    /** BlockItem for the Resonator. */
    public static final DeferredItem<BlockItem> RESONATOR =
            REGISTER.registerSimpleBlockItem("resonator", ModBlocks.RESONATOR);

    /** BlockItem for the Modulator Base frame. */
    public static final DeferredItem<BlockItem> MODULATOR_BASE =
            REGISTER.registerSimpleBlockItem("modulator_base", ModBlocks.MODULATOR_BASE);

    /** BlockItem for the Silk Touch Modulator. */
    public static final DeferredItem<BlockItem> SILK_TOUCH_MODULATOR =
            REGISTER.registerSimpleBlockItem("silk_touch_modulator", ModBlocks.SILK_TOUCH_MODULATOR);

    /** BlockItem for the Fortune Modulator. */
    public static final DeferredItem<BlockItem> FORTUNE_MODULATOR =
            REGISTER.registerSimpleBlockItem("fortune_modulator", ModBlocks.FORTUNE_MODULATOR);

    /** BlockItem for the Auto-Smelt Modulator. */
    public static final DeferredItem<BlockItem> AUTO_SMELT_MODULATOR =
            REGISTER.registerSimpleBlockItem("auto_smelt_modulator", ModBlocks.AUTO_SMELT_MODULATOR);

    /** Tuning Fork: a diagnostic wrench that toggles area overlays (see {@link TuningForkItem}). */
    public static final DeferredItem<Item> TUNING_FORK =
            REGISTER.registerItem("tuning_fork", TuningForkItem::new, new Item.Properties().stacksTo(1));

    /**
     * A bucket of Liquid Resonance. Deliberately a plain {@link BucketItem} — NeoForge only attaches
     * the fluid-handler capability to items whose class is exactly {@code BucketItem}, and that
     * capability is what lets other mods' tanks and pipes fill and empty it.
     */
    public static final DeferredItem<BucketItem> LIQUID_RESONANCE_BUCKET =
            REGISTER.registerItem("liquid_resonance_bucket",
                    props -> new BucketItem(ModFluids.LIQUID_RESONANCE.get(), props),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1));

    // Resonant components: ordinary materials steeped in Liquid Resonance by the Crucible. They are
    // plain crafting ingredients — the resonance cost is baked into the item, so the machines that
    // use them need no plumbing of their own. What each costs lives in its infusion recipe JSON.

    public static final DeferredItem<Item> ACTIVATED_LAPIS_LAZULI =
            REGISTER.registerSimpleItem("activated_lapis_lazuli");

    public static final DeferredItem<Item> SHINING_CRYSTALS =
            REGISTER.registerSimpleItem("shining_crystals");

    public static final DeferredItem<Item> PULSING_PEARL =
            REGISTER.registerSimpleItem("pulsing_pearl");

    public static final DeferredItem<Item> RESONATING_GEM =
            REGISTER.registerSimpleItem("resonating_gem");
}
