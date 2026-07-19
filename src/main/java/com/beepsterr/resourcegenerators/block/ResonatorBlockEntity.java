package com.beepsterr.resourcegenerators.block;

import com.beepsterr.resourcegenerators.Config;
import com.beepsterr.resourcegenerators.crystal.CrystalData;
import com.beepsterr.resourcegenerators.crystal.Modulation;
import com.beepsterr.resourcegenerators.crystal.CrystalResource;
import com.beepsterr.resourcegenerators.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The resonator: periodically scans a radius for placed crystals and rolls each one's resource
 * (a block's break loot table, using a synthetic netherite pickaxe so ores yield their raw drops)
 * into an output buffer. Crystals are cached between scans; tier scales rolls per cycle.
 */
public class ResonatorBlockEntity extends BlockEntity implements MenuProvider, AreaPreview {

    public static final int OUTPUT_SLOTS = 15;

    /** Horizontal + upward reach from the resonator. */
    private static final int RADIUS = 5;
    /** Downward reach is shallower — the resonator shouldn't dig far below itself. */
    private static final int RADIUS_DOWN = 2;
    private static final int RESCAN_INTERVAL = 200;
    /** Ticks between work cycles. Kept slow — each crystal only *maybe* generates (tier roll chance). */
    private static final int WORK_INTERVAL = 100;
    private static final int MAX_CRYSTALS = 512;

    /** Whether AlmostUnified is present — gates loading the compat class (soft dependency). */
    private static final boolean ALMOST_UNIFIED = net.neoforged.fml.ModList.get().isLoaded("almostunified");

    private final ItemStackHandler output = new ItemStackHandler(OUTPUT_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false; // output only — code fills it, hoppers may extract but not insert
        }
    };

    private List<BlockPos> cachedPositions = List.of();
    private List<ModulatorEntry> cachedModulators = List.of();
    private int tickCounter = 0;

    /** A modulator block found in range: its position, kind, and the box (h/v radius) it projects. */
    private record ModulatorEntry(BlockPos pos, Modulation modulation, int horizontalRadius, int verticalRadius) {}
    private int workProgress = 0;
    /** Last global crystal revision we scanned at (transient — forces a scan on first tick after load). */
    private long lastRevision = -1;

    /** [0] = progress into the current work cycle, [1] = ticks per cycle. Drives the GUI work bar. */
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? workProgress : WORK_INTERVAL;
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                workProgress = value;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public ResonatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RESONATOR.get(), pos, state);
    }

    public ItemStackHandler getOutput() {
        return output;
    }

    public ContainerData getContainerData() {
        return data;
    }

    public int getCrystalCount() {
        return cachedPositions.size();
    }

    /** The scan volume, as a world-space box — same reach the rescan uses (5 out/up, 2 down). */
    @Override
    public AABB getPreviewArea() {
        BlockPos c = worldPosition;
        return new AABB(
                c.getX() - RADIUS, c.getY() - RADIUS_DOWN, c.getZ() - RADIUS,
                c.getX() + RADIUS + 1, c.getY() + RADIUS + 1, c.getZ() + RADIUS + 1);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ResonatorBlockEntity be) {
        be.tickCounter++;
        // Event-driven: rescan whenever a crystal was placed/broken anywhere; the periodic scan is a
        // safety net for crystals that load into range without a place/break (e.g. chunk loading).
        long revision = CrystalScanTracker.revision();
        if (revision != be.lastRevision || be.tickCounter % RESCAN_INTERVAL == 0) {
            be.rescan(level, pos);
            be.lastRevision = revision;
        }
        if (level instanceof ServerLevel serverLevel) {
            // Charge the work bar only while there's something to do; it pauses when idle or full.
            if (!be.cachedPositions.isEmpty() && !be.isOutputFull()) {
                be.workProgress++;
                if (be.workProgress >= WORK_INTERVAL) {
                    be.workProgress = 0;
                    be.doWork(serverLevel, pos);
                }
            }
        }
    }

    /** Cache the positions of placed crystals in range that carry a resource. Ownership and the
     *  "resonated twice → break" decision are made later, at roll time, in {@link #doWork}. */
    private void rescan(Level level, BlockPos center) {
        List<BlockPos> positions = new ArrayList<>();
        List<ModulatorEntry> modulators = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(
                center.offset(-RADIUS, -RADIUS_DOWN, -RADIUS), center.offset(RADIUS, RADIUS, RADIUS))) {
            if (p.equals(center) || !level.isLoaded(p)) {
                continue;
            }
            if (level.getBlockEntity(p) instanceof PlacedCrystalBlockEntity crystal) {
                if (positions.size() >= MAX_CRYSTALS) {
                    continue;
                }
                CrystalData data = crystal.getCrystalData();
                if (data != null && data.resource().isPresent()) {
                    positions.add(p.immutable());
                }
            } else if (level.getBlockEntity(p) instanceof ModulatorBlockEntity modulator) {
                Modulation type = modulator.getModulation();
                if (type != null) {
                    modulators.add(new ModulatorEntry(p.immutable(), type,
                            modulator.getHorizontalRadius(), modulator.getVerticalRadius()));
                }
            }
        }
        this.cachedPositions = positions;
        this.cachedModulators = modulators;
    }

    /**
     * Try to resonate each crystal in range. This is where ownership is resolved: an unowned or
     * orphaned crystal is claimed; a crystal owned by another <em>live</em> resonator is being
     * resonated twice, so it's broken (dropped as an item). A claimed crystal then generates its
     * resource if it passes its tier's roll chance.
     */
    private void doWork(ServerLevel level, BlockPos center) {
        if (cachedPositions.isEmpty()) {
            return;
        }
        MinecraftServer server = level.getServer();
        HolderLookup.Provider registries = level.registryAccess();
        RandomSource random = level.getRandom();
        ItemStack plainTool = new ItemStack(Items.NETHERITE_PICKAXE); // proper tier, no silk -> raw drops
        ItemStack silkTool = null; // built lazily only if a Silk Touch modulator is actually in range

        // Rain penalty: a crystal exposed to the sky in the rain — or any crystal when the resonator
        // itself is rained on — rolls at reduced efficiency. Skipped entirely in clear weather.
        double rainEfficiency = Config.RAIN_EFFICIENCY.get();
        boolean weatherPenalty = rainEfficiency < 1.0 && level.isRaining();
        boolean resonatorRained = weatherPenalty && level.isRainingAt(center);

        for (BlockPos p : cachedPositions) {
            if (!(level.getBlockEntity(p) instanceof PlacedCrystalBlockEntity crystal)) {
                continue;
            }
            CrystalData data = crystal.getCrystalData();
            if (data == null || data.resource().isEmpty()) {
                continue;
            }

            // Ownership resolved at the moment of resonating.
            BlockPos owner = crystal.getOwner();
            if (owner != null && !owner.equals(center)
                    && level.getBlockEntity(owner) instanceof ResonatorBlockEntity) {
                level.destroyBlock(p, true); // resonated twice -> break it (drops as an item)
                continue;
            }
            if (owner == null || !owner.equals(center)) {
                crystal.setOwner(center); // claim (was unowned or orphaned)
            }

            float rollChance = data.tier().value().rollChance();
            if (weatherPenalty && (resonatorRained || level.isRainingAt(p))) {
                rollChance *= (float) rainEfficiency;
            }
            if (random.nextFloat() >= rollChance) {
                continue; // this crystal didn't generate this cycle
            }
            CrystalResource resource = data.resource().get();
            Optional<Holder<Block>> picked = resource.pickBlock(registries, random);
            if (picked.isEmpty()) {
                continue;
            }
            Block block = picked.get().value();

            // A crystal covered by a Silk Touch modulator is rolled with a silk tool -> ore block form.
            ItemStack tool = plainTool;
            if (isCovered(p, Modulation.SILK_TOUCH)) {
                if (silkTool == null) {
                    silkTool = buildSilkTool(registries);
                }
                tool = silkTool;
            }

            LootTable table = server.reloadableRegistries().getLootTable(block.getLootTable());
            LootParams params = new LootParams.Builder(level)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(center))
                    .withParameter(LootContextParams.BLOCK_STATE, block.defaultBlockState())
                    .withParameter(LootContextParams.TOOL, tool)
                    .create(LootContextParamSets.BLOCK);

            List<ItemStack> drops = table.getRandomItems(params);
            if (drops.isEmpty()) {
                continue;
            }
            for (ItemStack drop : drops) {
                ItemStack out = drop.copy();
                if (ALMOST_UNIFIED) {
                    // Collapse duplicate/disabled ore variants to the pack's preferred item.
                    out = com.beepsterr.resourcegenerators.compat.AlmostUnifiedCompat.unify(out);
                }
                insertOutput(out);
            }
            // Visual flair: a burst of dust at the crystal + a stream of the item to the resonator.
            spawnGenerateParticles(level, p, resource.color());
            spawnFlingParticles(level, p, center, drops.get(0));
        }
        setChanged();
    }

    /** Whether a modulator of the given kind projects over this crystal (h/v box, no stacking). */
    private boolean isCovered(BlockPos crystal, Modulation type) {
        for (ModulatorEntry m : cachedModulators) {
            if (m.modulation() != type) {
                continue;
            }
            BlockPos mp = m.pos();
            if (Math.abs(crystal.getX() - mp.getX()) <= m.horizontalRadius()
                    && Math.abs(crystal.getZ() - mp.getZ()) <= m.horizontalRadius()
                    && Math.abs(crystal.getY() - mp.getY()) <= m.verticalRadius()) {
                return true;
            }
        }
        return false;
    }

    /** A netherite pickaxe enchanted with Silk Touch, for rolling covered crystals into ore blocks. */
    private static ItemStack buildSilkTool(HolderLookup.Provider registries) {
        ItemStack pick = new ItemStack(Items.NETHERITE_PICKAXE);
        registries.lookupOrThrow(Registries.ENCHANTMENT).get(Enchantments.SILK_TOUCH)
                .ifPresent(holder -> pick.enchant(holder, 1));
        return pick;
    }

    /** A small colored dust burst at a crystal that just generated. */
    private static void spawnGenerateParticles(ServerLevel level, BlockPos crystal, int rgb) {
        Vector3f color = new Vector3f(
                ((rgb >> 16) & 0xFF) / 255.0f,
                ((rgb >> 8) & 0xFF) / 255.0f,
                (rgb & 0xFF) / 255.0f);
        level.sendParticles(new DustParticleOptions(color, 1.2f),
                crystal.getX() + 0.5, crystal.getY() + 0.6, crystal.getZ() + 0.5,
                6, 0.22, 0.28, 0.22, 0.0);
    }

    /** A short trail of item-break particles from the crystal toward the resonator. */
    private static void spawnFlingParticles(ServerLevel level, BlockPos crystal, BlockPos resonator, ItemStack stack) {
        ItemParticleOption particle = new ItemParticleOption(ParticleTypes.ITEM, stack);
        double fx = crystal.getX() + 0.5, fy = crystal.getY() + 0.5, fz = crystal.getZ() + 0.5;
        double tx = resonator.getX() + 0.5, ty = resonator.getY() + 0.5, tz = resonator.getZ() + 0.5;
        int steps = 6;
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            level.sendParticles(particle,
                    Mth.lerp(t, fx, tx), Mth.lerp(t, fy, ty), Mth.lerp(t, fz, tz),
                    1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    private boolean isOutputFull() {
        for (int i = 0; i < output.getSlots(); i++) {
            ItemStack s = output.getStackInSlot(i);
            if (s.isEmpty() || s.getCount() < s.getMaxStackSize()) {
                return false;
            }
        }
        return true;
    }

    /** Distribute a stack into the output buffer (bypasses isItemValid). Overflow is dropped. */
    private void insertOutput(ItemStack stack) {
        for (int i = 0; i < output.getSlots() && !stack.isEmpty(); i++) {
            ItemStack inSlot = output.getStackInSlot(i);
            if (inSlot.isEmpty()) {
                int move = Math.min(stack.getCount(), stack.getMaxStackSize());
                output.setStackInSlot(i, stack.split(move));
            } else if (ItemStack.isSameItemSameComponents(inSlot, stack)
                    && inSlot.getCount() < inSlot.getMaxStackSize()) {
                int move = Math.min(stack.getCount(), inSlot.getMaxStackSize() - inSlot.getCount());
                inSlot.grow(move);
                stack.shrink(move);
                output.setStackInSlot(i, inSlot);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Output", output.serializeNBT(registries));
        tag.putInt("Tick", tickCounter);
        tag.putInt("Work", workProgress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        output.deserializeNBT(registries, tag.getCompound("Output"));
        tickCounter = tag.getInt("Tick");
        workProgress = tag.getInt("Work");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.beepsresourcegenerators.resonator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ResonatorMenu(containerId, playerInventory, this, data);
    }
}
