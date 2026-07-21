package com.beepsterr.resourcegenerators.block;

import com.beepsterr.resourcegenerators.crystal.CrystalTier;
import com.beepsterr.resourcegenerators.inventory.SidedItemHandler;
import com.beepsterr.resourcegenerators.item.CrystalItem;
import com.beepsterr.resourcegenerators.registry.ModBlockEntities;
import com.beepsterr.resourcegenerators.registry.ModRegistries;
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
import net.minecraft.util.Mth;
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

/**
 * Block entity for the Crystal Former. Three slots — base (0), catalyst (1), output (2) — and
 * a progress timer. Each tick it looks for a tier whose base+catalyst ingredients match the
 * input slots, and once progress completes, consumes one of each and outputs a blank crystal
 * of that tier.
 */
public class CrystalFormerBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOT_BASE = 0;
    public static final int SLOT_CATALYST = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int SLOT_FUEL = 3;

    /** Power supply: solid fuel (furnace-style) or a filled RF/FE buffer. Required to make progress. */
    private final MachineFuel fuel = new MachineFuel();

    /** Forming a crystal takes a long time — ~120 seconds. */
    private static final int DEFAULT_MAX_PROGRESS = 2400;
    /** How often (ticks) to sync fill progress to clients while forming; the client interpolates between. */
    private static final int SYNC_INTERVAL = 20;

    /** Set when the inventory changes; drained in serverTick to sync the shown output crystal. */
    private boolean needsClientSync = false;
    /** Server-side: whether it's actively forming, and the colour of the tier being formed (-1 = none). */
    private boolean forming = false;
    private int formingColor = -1;

    // Client-side view for the renderer (fill column + shown crystal).
    private int clientProgress = 0;
    private int clientMaxProgress = DEFAULT_MAX_PROGRESS;
    private boolean clientForming = false;
    private int clientColor = -1;
    private long clientSyncGameTime = 0L;

    private final ItemStackHandler inventory = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            needsClientSync = true;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_OUTPUT -> false;                     // code-filled only
                case SLOT_FUEL -> MachineFuel.isFuel(stack);
                case SLOT_BASE -> isBaseItem(stack);           // must be some tier's base
                case SLOT_CATALYST -> isCatalystItem(stack);   // must be some tier's catalyst
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

    private int progress = 0;
    private int maxProgress = DEFAULT_MAX_PROGRESS;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> fuel.getLitTime();
                case 3 -> fuel.getLitDuration();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 1 -> maxProgress = value;
                case 2 -> fuel.setLit(value, fuel.getLitDuration());
                case 3 -> fuel.setLit(fuel.getLitTime(), value);
                default -> { }
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public CrystalFormerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRYSTAL_FORMER.get(), pos, state);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    // Cached per-face views: base in from the top, catalyst in from the sides, output out the bottom.
    private SidedItemHandler topInsert;    // base only
    private SidedItemHandler sideInsert;   // catalyst only
    private SidedItemHandler bottomOutput; // output only

    /** The automation view for a given face; null side (internal access) sees the full inventory. */
    @Nullable
    public IItemHandler getInventoryForSide(@Nullable Direction side) {
        if (side == null) {
            return inventory;
        }
        // Masks are {base, catalyst, output, fuel}; fuel may be fed in from the top or the sides.
        return switch (side) {
            case UP -> topInsert != null ? topInsert
                    : (topInsert = new SidedItemHandler(inventory,
                    new boolean[]{true, false, false, true}, new boolean[]{false, false, false, false}, null));
            case DOWN -> bottomOutput != null ? bottomOutput
                    : (bottomOutput = new SidedItemHandler(inventory,
                    new boolean[]{false, false, false, false}, new boolean[]{false, false, true, false}, null));
            default -> sideInsert != null ? sideInsert
                    : (sideInsert = new SidedItemHandler(inventory,
                    new boolean[]{false, true, false, true}, new boolean[]{false, false, false, false}, null));
        };
    }

    public ContainerData getContainerData() {
        return data;
    }

    /** The RF/FE buffer, exposed to adjacent cables as an alternative to solid fuel. */
    public MachineFuel getEnergyStorage() {
        return fuel;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CrystalFormerBlockEntity be) {
        Holder<CrystalTier> match = be.findMatchingTier(level);
        ItemStack result = match == null ? ItemStack.EMPTY : CrystalItem.createBlank(match);
        boolean hasRecipe = match != null && be.canOutput(result);
        // A recipe alone isn't enough — the machine only advances on ticks it can draw power.
        boolean powered = be.fuel.tryPower(hasRecipe, be.inventory, SLOT_FUEL);
        boolean forming = hasRecipe && powered;
        int color = forming ? match.value().color() : -1;

        if (!hasRecipe) {
            if (be.progress != 0) {
                be.progress = 0;
                be.setChanged();
            }
        } else if (powered) {
            be.progress++;
            if (be.progress >= be.maxProgress) {
                be.inventory.extractItem(SLOT_BASE, 1, false);
                be.inventory.extractItem(SLOT_CATALYST, 1, false);
                be.pushOutput(result);
                be.progress = 0;
            }
            be.setChanged();
        } else {
            // Recipe primed but unpowered — hold progress where it is, awaiting fuel/energy.
            be.setChanged();
        }

        // Sync the fill (progress + tier colour) and the shown output crystal to nearby clients:
        // on any state change, when the inventory changed, and a slow heartbeat while forming.
        boolean stateChanged = forming != be.forming || color != be.formingColor;
        be.forming = forming;
        be.formingColor = color;
        if (be.needsClientSync || stateChanged || (forming && level.getGameTime() % SYNC_INTERVAL == 0)) {
            be.needsClientSync = false;
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    /** Whether the stack is accepted in the base slot (it's the base ingredient of some tier). */
    public boolean isBaseItem(ItemStack stack) {
        return matchesAnyTier(stack, true);
    }

    /** Whether the stack is accepted in the catalyst slot (it's the catalyst of some tier). */
    public boolean isCatalystItem(ItemStack stack) {
        return matchesAnyTier(stack, false);
    }

    private boolean matchesAnyTier(ItemStack stack, boolean asBase) {
        if (stack.isEmpty() || level == null) {
            return false;
        }
        var registry = level.registryAccess().registryOrThrow(ModRegistries.CRYSTAL_TIER_KEY);
        for (Holder.Reference<CrystalTier> tier : registry.holders().toList()) {
            CrystalTier value = tier.value();
            if ((asBase ? value.base() : value.catalyst()).test(stack)) {
                return true;
            }
        }
        return false;
    }

    /** First tier whose base + catalyst ingredients both match the input slots, else null. */
    @Nullable
    private Holder<CrystalTier> findMatchingTier(Level level) {
        ItemStack base = inventory.getStackInSlot(SLOT_BASE);
        ItemStack catalyst = inventory.getStackInSlot(SLOT_CATALYST);
        if (base.isEmpty() || catalyst.isEmpty()) {
            return null;
        }
        var registry = level.registryAccess().registryOrThrow(ModRegistries.CRYSTAL_TIER_KEY);
        for (Holder.Reference<CrystalTier> tier : registry.holders().toList()) {
            CrystalTier value = tier.value();
            if (value.base().test(base) && value.catalyst().test(catalyst)) {
                return tier;
            }
        }
        return null;
    }

    private boolean canOutput(ItemStack result) {
        ItemStack out = inventory.getStackInSlot(SLOT_OUTPUT);
        if (out.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(out, result)
                && out.getCount() + result.getCount() <= out.getMaxStackSize();
    }

    private void pushOutput(ItemStack result) {
        ItemStack out = inventory.getStackInSlot(SLOT_OUTPUT);
        if (out.isEmpty()) {
            inventory.setStackInSlot(SLOT_OUTPUT, result.copy());
        } else {
            out.grow(result.getCount());
        }
    }

    // --- Client sync: the fill column (progress + tier colour) and the shown output crystal ---

    public float fillFraction(float partialTick) {
        if (clientMaxProgress <= 0 || level == null) {
            return 0f;
        }
        float p = clientProgress;
        if (clientForming) {
            float elapsed = ((float) level.getGameTime() + partialTick) - clientSyncGameTime;
            p = Math.min(clientMaxProgress, clientProgress + Math.max(0f, elapsed));
        }
        return Mth.clamp(p / clientMaxProgress, 0f, 1f);
    }

    /** Packed 0xRRGGBB of the tier currently being formed, or -1 when not forming. */
    public int getFormingColor() {
        return clientColor;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.putInt("Progress", progress);
        tag.putInt("MaxProgress", maxProgress);
        tag.putBoolean("Forming", forming);
        tag.putInt("FormingColor", formingColor);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
        clientProgress = tag.getInt("Progress");
        clientMaxProgress = tag.getInt("MaxProgress");
        clientForming = tag.getBoolean("Forming");
        clientColor = tag.getInt("FormingColor");
        clientSyncGameTime = level != null ? level.getGameTime() : 0L;
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
        tag.putInt("Progress", progress);
        fuel.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        progress = tag.getInt("Progress");
        maxProgress = DEFAULT_MAX_PROGRESS; // constant per machine, not persisted state
        fuel.load(tag);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.bmcrm.crystal_former");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CrystalFormerMenu(containerId, playerInventory, this, data);
    }
}
