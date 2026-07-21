package com.beepsterr.resourcegenerators.block;

import com.beepsterr.resourcegenerators.crystal.CrystalData;
import com.beepsterr.resourcegenerators.crystal.CrystalInfusion;
import com.beepsterr.resourcegenerators.crystal.CrystalInfusionMap;
import com.beepsterr.resourcegenerators.crystal.CrystalResource;
import com.beepsterr.resourcegenerators.inventory.SidedItemHandler;
import com.beepsterr.resourcegenerators.registry.ModBlockEntities;
import com.beepsterr.resourcegenerators.registry.ModDataComponents;
import com.beepsterr.resourcegenerators.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Block entity for the Crystal Infuser. Two slots — material (0) and crystal (1) — and no output:
 * it feeds the material into the blank crystal in place, one unit every {@link #TICKS_PER_UNIT}
 * ticks, until the crystal's infusion reaches {@link #INFUSION_COST}, at which point its resource
 * is set and the fill state removed. The fill state lives on the crystal item (durability bar).
 */
public class CrystalInfuserBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOT_MATERIAL = 0;
    public static final int SLOT_CRYSTAL = 1;
    public static final int SLOT_FUEL = 2;

    /** Units of material needed to fully infuse a crystal. */
    public static final int INFUSION_COST = 32;
    /** Ticks between consuming one unit of material. */
    private static final int TICKS_PER_UNIT = 10;

    /** Set when the inventory changes; drained in serverTick to sync the two shown items to clients. */
    private boolean needsClientSync = false;

    /** Power supply: solid fuel (furnace-style) or a filled RF/FE buffer. Required to make progress. */
    private final MachineFuel fuel = new MachineFuel();

    private final ItemStackHandler inventory = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            needsClientSync = true;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_MATERIAL -> isInfusableMaterial(stack);  // only materials that map to a resource
                case SLOT_CRYSTAL -> isBlankCrystal(stack);        // blank / still-infusing crystals only
                case SLOT_FUEL -> MachineFuel.isFuel(stack);
                default -> false;
            };
        }

        @Override
        public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
            // Older saves had fewer slots; don't let the stored Size shrink the handler (fuel slot added).
            if (nbt.getInt("Size") < getSlots()) {
                nbt = nbt.copy();
                nbt.putInt("Size", getSlots());
            }
            super.deserializeNBT(provider, nbt);
        }
    };

    private int feedTimer = 0;

    /** Syncs the furnace-style flame gauge to the open GUI. */
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> fuel.getLitTime();
                case 1 -> fuel.getLitDuration();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> fuel.setLit(value, fuel.getLitDuration());
                case 1 -> fuel.setLit(fuel.getLitTime(), value);
                default -> { }
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public CrystalInfuserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRYSTAL_INFUSER.get(), pos, state);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    // Cached per-face views: inputs + fuel in from the top, fuel from the sides, crystals out the bottom.
    private SidedItemHandler topInsert;
    private SidedItemHandler sideInsert;
    private SidedItemHandler bottomOutput;

    /** The automation view for a given face; null side (internal access) sees the full inventory. */
    @Nullable
    public IItemHandler getInventoryForSide(@Nullable Direction side) {
        if (side == null) {
            return inventory;
        }
        // Masks are {material, crystal, fuel}.
        return switch (side) {
            // Top accepts material, blank crystals, and fuel; isItemValid routes each to its slot.
            case UP -> topInsert != null ? topInsert
                    : (topInsert = new SidedItemHandler(inventory,
                    new boolean[]{true, true, true}, new boolean[]{false, false, false}, null));
            // Bottom only yields the crystal once it is fully infused — never a half-done one.
            case DOWN -> bottomOutput != null ? bottomOutput
                    : (bottomOutput = new SidedItemHandler(inventory,
                    new boolean[]{false, false, false}, new boolean[]{false, true, false},
                    (slot, current) -> isInfusedCrystal(current)));
            // Sides accept fuel only.
            default -> sideInsert != null ? sideInsert
                    : (sideInsert = new SidedItemHandler(inventory,
                    new boolean[]{false, false, true}, new boolean[]{false, false, false}, null));
        };
    }

    public ContainerData getContainerData() {
        return data;
    }

    /** The RF/FE buffer, exposed to adjacent cables as an alternative to solid fuel. */
    public MachineFuel getEnergyStorage() {
        return fuel;
    }

    static boolean isCrystal(ItemStack stack) {
        return stack.is(ModItems.CRYSTAL.get());
    }

    static boolean isBlankCrystal(ItemStack stack) {
        if (!isCrystal(stack)) {
            return false;
        }
        CrystalData data = stack.get(ModDataComponents.CRYSTAL_DATA.get());
        return data != null && data.resource().isEmpty();
    }

    /** A crystal that has finished infusing (carries a resource). */
    static boolean isInfusedCrystal(ItemStack stack) {
        if (!isCrystal(stack)) {
            return false;
        }
        CrystalData data = stack.get(ModDataComponents.CRYSTAL_DATA.get());
        return data != null && data.resource().isPresent();
    }

    /** A non-crystal material that actually maps to a crystal resource (so it can be fed in). */
    private boolean isInfusableMaterial(ItemStack stack) {
        if (stack.isEmpty() || isCrystal(stack) || level == null) {
            return false;
        }
        return CrystalInfusionMap.resolve(level.registryAccess(), stack).isPresent();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CrystalInfuserBlockEntity be) {
        // Push the shown items (crystal + material) to nearby clients whenever the inventory changed.
        if (be.needsClientSync) {
            be.needsClientSync = false;
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
        ItemStack crystal = be.inventory.getStackInSlot(SLOT_CRYSTAL);
        ItemStack material = be.inventory.getStackInSlot(SLOT_MATERIAL);

        Optional<CrystalResource> target = be.currentTarget(level, crystal, material);
        boolean work = target.isPresent();
        // Burn fuel furnace-style (lit fuel drains every tick); a tick infuses only when lit + work.
        boolean powered = be.fuel.tick(work, be.inventory, SLOT_FUEL);

        if (!work) {
            boolean changed = be.fuel.isLit(); // a fuel still burning down while idle must be persisted
            if (be.feedTimer != 0) {
                be.feedTimer = 0;
                changed = true;
            }
            if (changed) {
                be.setChanged();
            }
            return;
        }
        if (powered) {
            be.feedTimer++;
            if (be.feedTimer >= TICKS_PER_UNIT) {
                be.feedTimer = 0;
                be.feedOneUnit(crystal, material, target.get());
            }
        }
        // else: primed but unpowered — hold feedTimer, awaiting fuel/energy.
        be.setChanged();
    }

    /** The resource this pairing would produce, or empty if it can't currently infuse. */
    private Optional<CrystalResource> currentTarget(Level level, ItemStack crystal, ItemStack material) {
        if (!isBlankCrystal(crystal) || material.isEmpty()) {
            return Optional.empty();
        }
        Optional<CrystalResource> target = CrystalInfusionMap.resolve(level.registryAccess(), material);
        if (target.isEmpty()) {
            return Optional.empty();
        }
        // If already infusing, the material must match what it locked onto.
        CrystalInfusion inProgress = crystal.get(ModDataComponents.CRYSTAL_INFUSION.get());
        if (inProgress != null && !inProgress.target().equals(target.get())) {
            return Optional.empty();
        }
        return target;
    }

    private void feedOneUnit(ItemStack crystalRef, ItemStack material, CrystalResource target) {
        // A drip of the material falling down through the grate onto the crystal.
        if (level instanceof ServerLevel server) {
            spawnFeedParticles(server, material);
        }
        ItemStack crystal = crystalRef.copy();
        CrystalInfusion infusion = crystal.get(ModDataComponents.CRYSTAL_INFUSION.get());
        if (infusion == null) {
            infusion = new CrystalInfusion(target, 0, INFUSION_COST);
        }
        infusion = infusion.plus(1);

        if (infusion.isComplete()) {
            CrystalData data = crystal.get(ModDataComponents.CRYSTAL_DATA.get());
            crystal.set(ModDataComponents.CRYSTAL_DATA.get(), data.withResource(target));
            crystal.remove(ModDataComponents.CRYSTAL_INFUSION.get());
        } else {
            crystal.set(ModDataComponents.CRYSTAL_INFUSION.get(), infusion);
        }

        inventory.setStackInSlot(SLOT_CRYSTAL, crystal);
        inventory.extractItem(SLOT_MATERIAL, 1, false);
    }

    /** A short shower of the material's item particles dripping down through the grate. */
    private void spawnFeedParticles(ServerLevel level, ItemStack material) {
        if (material.isEmpty()) {
            return;
        }
        ItemParticleOption particle = new ItemParticleOption(ParticleTypes.ITEM, material.copyWithCount(1));
        RandomSource random = level.getRandom();
        double x = worldPosition.getX() + 0.5;
        double y = worldPosition.getY() + 0.82; // just under the grate
        double z = worldPosition.getZ() + 0.5;
        for (int i = 0; i < 5; i++) {
            double ox = (random.nextDouble() - 0.5) * 0.4;
            double oz = (random.nextDouble() - 0.5) * 0.4;
            // count 0 -> the vector is the velocity: straight down through the grate.
            level.sendParticles(particle, x + ox, y, z + oz, 0, 0.0, -1.0, 0.0, 0.12);
        }
    }

    // --- Client sync: the crystal + material shown inside the machine (drawn by CrystalInfuserRenderer) ---

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("Inventory", inventory.serializeNBT(registries));
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
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
            handleUpdateTag(tag, registries);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.putInt("FeedTimer", feedTimer);
        fuel.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        feedTimer = tag.getInt("FeedTimer");
        fuel.load(tag);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.bmcrm.crystal_infuser");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CrystalInfuserMenu(containerId, playerInventory, this, data);
    }
}
