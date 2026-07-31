package com.beepsterr.resourcegenerators.block;

import com.beepsterr.resourcegenerators.Config;
import com.beepsterr.resourcegenerators.client.ResonatorAnimator;
import com.beepsterr.resourcegenerators.crystal.AreaShape;
import com.beepsterr.resourcegenerators.crystal.CrystalData;
import com.beepsterr.resourcegenerators.crystal.Modulation;
import com.beepsterr.resourcegenerators.crystal.CrystalResource;
import com.beepsterr.resourcegenerators.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    /** Note blocks in range — purely for the resonance ring's cute sympathetic chime. */
    private List<BlockPos> cachedNoteBlocks = List.of();
    private int tickCounter = 0;

    /** A modulator block found in range: its position, kind, footprint shape, and h/v radius. */
    private record ModulatorEntry(BlockPos pos, Modulation modulation, AreaShape shape,
                                  Direction facing, int horizontalRadius, int verticalRadius) {}
    private int workProgress = 0;
    /** Last global crystal revision we scanned at (transient — forces a scan on first tick after load). */
    private long lastRevision = -1;

    // --- World-render animation (the fuel-tank fill + spinning ring drawn by ResonatorRenderer) ---
    /** How often (ticks) an active resonator pushes an animation sync to nearby clients. */
    private static final int ANIM_SYNC_INTERVAL = 20;
    /** Server-side: whether the resonator is currently charging (has work to do and room to store it). */
    private boolean syncedActive = false;
    /** Server-side: game time the last work cycle fired, so clients can drive the "Bwomp" spin spike. */
    private long lastCycleGameTime = Long.MIN_VALUE;
    /** Client-side render state (fill interpolation + spin integration); fed by the update packet. */
    private final ResonatorAnimator animator = new ResonatorAnimator();

    /** Presentation-only resonance pulse: schedules/drains the pling sweep and resource bursts. */
    private final ResonancePulse pulse = new ResonancePulse();

    /** [0] = progress into the current work cycle, [1] = ticks per cycle. Drives the GUI work bar. */
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? workProgress : Config.WORK_INTERVAL.get();
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
    public List<AABB> getPreviewBoxes() {
        BlockPos c = worldPosition;
        return List.of(new AABB(
                c.getX() - RADIUS, c.getY() - RADIUS_DOWN, c.getZ() - RADIUS,
                c.getX() + RADIUS + 1, c.getY() + RADIUS + 1, c.getZ() + RADIUS + 1));
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
            boolean active = !be.cachedPositions.isEmpty() && !be.isOutputFull();
            boolean fired = false;
            if (active) {
                be.workProgress++;
                if (be.workProgress >= Config.WORK_INTERVAL.get()) {
                    be.workProgress = 0;
                    be.lastCycleGameTime = serverLevel.getGameTime();
                    be.doWork(serverLevel, pos);
                    fired = true;
                }
            }
            // Sync the world-render animation (fill/spin/Bwomp) to nearby clients: on any state change,
            // immediately on a cycle firing (so the spike lands on time), and a slow heartbeat while active.
            boolean activeChanged = active != be.syncedActive;
            if (fired || activeChanged || (active && be.tickCounter % ANIM_SYNC_INTERVAL == 0)) {
                be.syncedActive = active;
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            }

            // Drain any scheduled resonance-pulse effects whose moment has arrived.
            be.pulse.tick(serverLevel, be.tickCounter, pos);
        }
    }

    /** Cache the positions of placed crystals in range that carry a resource. Ownership and the
     *  "resonated twice → break" decision are made later, at roll time, in {@link #doWork}. */
    private void rescan(Level level, BlockPos center) {
        List<BlockPos> positions = new ArrayList<>();
        List<ModulatorEntry> modulators = new ArrayList<>();
        List<BlockPos> noteBlocks = new ArrayList<>();
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
                AreaShape shape = modulator.getShape();
                if (type != null && shape != null) {
                    modulators.add(new ModulatorEntry(p.immutable(), type, shape, modulator.getFacing(),
                            modulator.getHorizontalRadius(), modulator.getVerticalRadius()));
                }
            } else if (level.getBlockState(p).is(Blocks.NOTE_BLOCK)) {
                noteBlocks.add(p.immutable());
            }
        }
        this.cachedPositions = positions;
        this.cachedModulators = modulators;
        this.cachedNoteBlocks = noteBlocks;
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
        // Kick off the shockwave — one self-expanding ring particle from the resonator's core.
        pulse.shockwave(level, center);
        // Let the ring chime any note blocks it sweeps over, timed to the wave front reaching each.
        // Buried note blocks stay silent — same "needs air above" rule vanilla uses to play a note.
        for (BlockPos np : cachedNoteBlocks) {
            if (level.getBlockState(np).is(Blocks.NOTE_BLOCK) && level.getBlockState(np.above()).isAir()) {
                pulse.scheduleNote(tickCounter, center, np);
            }
        }
        MinecraftServer server = level.getServer();
        HolderLookup.Provider registries = level.registryAccess();
        RandomSource random = level.getRandom();
        ItemStack plainTool = new ItemStack(Items.NETHERITE_PICKAXE); // proper tier, no silk -> raw drops
        ItemStack silkTool = null; // built lazily only if a Silk Touch modulator is actually in range
        Map<Integer, ItemStack> fortuneTools = new HashMap<>(); // one per fortune level actually needed

        // Rain penalty: a crystal exposed to the sky in the rain — or any crystal when the resonator
        // itself is rained on — rolls at reduced efficiency. Skipped entirely in clear weather.
        double rainEfficiency = Config.RAIN_EFFICIENCY.get();
        boolean weatherPenalty = rainEfficiency < 1.0 && level.isRaining();
        boolean resonatorRained = weatherPenalty && level.isRainingAt(center);

        // Mob interference: a hostile mob near the resonator ("noisy neighbors") reduces efficiency too.
        double mobEfficiency = Config.MOB_EFFICIENCY.get();
        boolean mobPenalty = mobEfficiency < 1.0 && hasNearbyHostile(level, center);

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
            pulse.schedulePling(tickCounter, center, p); // reaches this crystal after a distance-based delay

            // Resonance is banked for being resonated at all, not for getting a drop out of it — so a
            // crystal's fluid yield tracks uptime rather than luck, and an unlucky streak isn't
            // punished twice. Stops accruing once the crystal saturates.
            crystal.addCharge(1);

            float rollChance = data.tier().value().rollChance();
            if (weatherPenalty && (resonatorRained || level.isRainingAt(p))) {
                rollChance *= (float) rainEfficiency;
            }
            if (mobPenalty) {
                rollChance *= (float) mobEfficiency;
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

            // Silk Touch wins if present (-> ore block); otherwise Fortune stacks (level = overlaps).
            ItemStack tool = plainTool;
            if (isCovered(p, Modulation.SILK_TOUCH)) {
                if (silkTool == null) {
                    silkTool = buildSilkTool(registries);
                }
                tool = silkTool;
            } else {
                int fortune = countCovering(p, Modulation.FORTUNE);
                if (fortune > 0) {
                    int cap = Config.MAX_FORTUNE_LEVEL.get();
                    int fortuneLevel = cap > 0 ? Math.min(fortune, cap) : fortune;
                    tool = fortuneTools.computeIfAbsent(fortuneLevel, lvl -> buildFortuneTool(registries, lvl));
                }
            }
            // Auto-smelt if the crystal's beam to the resonator passes through an Auto-Smelt modulator.
            boolean autoSmelt = beamHitsAutoSmelt(p, center);

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
                if (autoSmelt) {
                    out = smelt(server, level, out); // raw ore -> ingot before unification
                }
                if (ALMOST_UNIFIED) {
                    // Collapse duplicate/disabled ore variants to the pack's preferred item.
                    out = com.beepsterr.resourcegenerators.compat.AlmostUnifiedCompat.unify(out);
                }
                insertOutput(out);
            }
            // Visual flair, staggered to land a beat after this crystal's pling: a burst of dust
            // (red while auto-smelting) + a stream to the resonator.
            int color = autoSmelt ? 0xE23D28 : resource.color();
            pulse.scheduleBurst(tickCounter, center, p, drops.get(0).copy(), color);
        }
        setChanged();
    }

    /** How many modulators of the given kind cover this crystal (overlapping footprints stack). */
    private int countCovering(BlockPos crystal, Modulation type) {
        int count = 0;
        for (ModulatorEntry m : cachedModulators) {
            if (m.modulation() != type) {
                continue;
            }
            BlockPos mp = m.pos();
            if (m.shape().covers(m.facing(), m.horizontalRadius(), m.verticalRadius(),
                    crystal.getX() - mp.getX(), crystal.getY() - mp.getY(), crystal.getZ() - mp.getZ())) {
                count++;
            }
        }
        return count;
    }

    /** Whether at least one modulator of the given kind covers this crystal. */
    private boolean isCovered(BlockPos crystal, Modulation type) {
        return countCovering(crystal, type) > 0;
    }

    /** Whether a live hostile mob is within the disruption radius of the resonator. */
    private static boolean hasNearbyHostile(ServerLevel level, BlockPos center) {
        AABB area = new AABB(center).inflate(Config.MOB_DISRUPTION_RADIUS.get());
        return !level.getEntitiesOfClass(Mob.class, area, m -> m instanceof Enemy && m.isAlive()).isEmpty();
    }

    /** A netherite pickaxe enchanted with Silk Touch, for rolling covered crystals into ore blocks. */
    private static ItemStack buildSilkTool(HolderLookup.Provider registries) {
        ItemStack pick = new ItemStack(Items.NETHERITE_PICKAXE);
        registries.lookupOrThrow(Registries.ENCHANTMENT).get(Enchantments.SILK_TOUCH)
                .ifPresent(holder -> pick.enchant(holder, 1));
        return pick;
    }

    /** A netherite pickaxe enchanted with Fortune at the given level, for extra ore drops. */
    private static ItemStack buildFortuneTool(HolderLookup.Provider registries, int level) {
        ItemStack pick = new ItemStack(Items.NETHERITE_PICKAXE);
        registries.lookupOrThrow(Registries.ENCHANTMENT).get(Enchantments.FORTUNE)
                .ifPresent(holder -> pick.enchant(holder, level));
        return pick;
    }

    /** Whether the crystal->resonator beam passes through any Auto-Smelt modulator (no occlusion). */
    private boolean beamHitsAutoSmelt(BlockPos crystal, BlockPos center) {
        Vec3 from = Vec3.atCenterOf(crystal);
        Vec3 to = Vec3.atCenterOf(center);
        for (ModulatorEntry m : cachedModulators) {
            if (m.modulation() == Modulation.AUTO_SMELT
                    && new AABB(m.pos()).clip(from, to).isPresent()) {
                return true;
            }
        }
        return false;
    }

    /** Smelt one stack via a furnace recipe (count preserved); returned unchanged if it can't be smelted. */
    private static ItemStack smelt(MinecraftServer server, ServerLevel level, ItemStack stack) {
        if (stack.isEmpty()) {
            return stack;
        }
        SingleRecipeInput input = new SingleRecipeInput(stack);
        Optional<RecipeHolder<SmeltingRecipe>> recipe =
                server.getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, level);
        if (recipe.isEmpty()) {
            return stack;
        }
        ItemStack result = recipe.get().value().assemble(input, level.registryAccess());
        if (result.isEmpty()) {
            return stack;
        }
        ItemStack out = result.copy();
        out.setCount(result.getCount() * stack.getCount());
        return out;
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

    // --- Client sync for the world-render animation (fill level, spin, cycle spike) ---

    private void writeClientAnim(CompoundTag tag) {
        tag.putInt("Work", workProgress);
        tag.putInt("Interval", Config.WORK_INTERVAL.get());
        tag.putBoolean("Active", syncedActive);
        tag.putLong("LastCycle", lastCycleGameTime);
    }

    private void readClientAnim(CompoundTag tag) {
        animator.read(tag, level != null ? level.getGameTime() : 0L);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        writeClientAnim(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        readClientAnim(tag);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        CompoundTag tag = packet.getTag();
        if (tag != null) {
            readClientAnim(tag);
        }
    }

    /** Client: 0..1 fill of the fuel tank, interpolated from the last sync so it climbs smoothly. */
    public float fillFraction(float partialTick) {
        return level == null ? 0f : animator.fillFraction(level.getGameTime(), partialTick);
    }

    /** Client: advance and return the ring's spin angle, integrating the (variable) spin speed over time. */
    public float advanceSpin(float partialTick) {
        return level == null ? animator.currentSpin() : animator.advanceSpin(level.getGameTime(), partialTick);
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
        return Component.translatable("block.bmcrm.resonator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ResonatorMenu(containerId, playerInventory, this, data);
    }
}
