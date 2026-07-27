package com.beepsterr.resourcegenerators.block;

import com.beepsterr.resourcegenerators.crystal.CrystalCharge;
import com.beepsterr.resourcegenerators.crystal.ResonanceInfusionRecipe;
import com.beepsterr.resourcegenerators.inventory.SidedItemHandler;
import com.beepsterr.resourcegenerators.registry.ModBlockEntities;
import com.beepsterr.resourcegenerators.registry.ModFluids;
import com.beepsterr.resourcegenerators.registry.ModItems;
import com.beepsterr.resourcegenerators.registry.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Block entity for the Crystal Crucible — the mod's one fluid machine, working in both directions.
 *
 * <p>It renders crystals down into Liquid Resonance: a crystal holding nothing is refused outright,
 * so melting is always a deliberate trade of a working generator for the fluid it is worth. It then
 * spends that fluid steeping ordinary materials into resonant components (see
 * {@link ResonanceInfusionRecipe}), which are plain crafting ingredients — that's how the cost
 * reaches the rest of the mod without every machine needing a tank of its own.
 *
 * <p>Four slots — crystal to melt (0), material or empty bucket in (1), result out (2), fuel (3) —
 * plus an internal tank, drained by the input slot or by any pipe that speaks fluid handler.
 * Melting is the only timed job; filling a bucket and steeping a material are instant, since both
 * just pour out of the tank.
 */
public class CrystalCrucibleBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOT_CRYSTAL = 0;
    public static final int SLOT_INPUT = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int SLOT_FUEL = 3;

    /** Melting a crystal takes ten seconds regardless of how much it holds. */
    private static final int MELT_TICKS = 200;
    /** Tank size — one shulker crystal's worth, the largest single melt there can be. */
    public static final int TANK_CAPACITY = 4000;

    /** Power supply: solid fuel (furnace-style) or a filled RF/FE buffer. Required to make progress. */
    private final MachineFuel fuel = new MachineFuel();

    /** Set when the tank changes; drained in serverTick to sync the fill level to nearby clients. */
    private boolean needsClientSync = false;

    private final FluidTank tank = new FluidTank(TANK_CAPACITY,
            stack -> stack.is(ModFluids.LIQUID_RESONANCE.get())) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            needsClientSync = true;
        }
    };

    private final ItemStackHandler inventory = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_CRYSTAL -> isMeltable(stack);
                case SLOT_INPUT -> isResonanceContainer(stack) || hasInfusionRecipe(stack);
                case SLOT_OUTPUT -> false; // code-filled only
                case SLOT_FUEL -> MachineFuel.isFuel(stack);
                default -> false;
            };
        }
    };

    private int progress = 0;

    /** Syncs the flame gauge and the tank level to the open GUI. */
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> MELT_TICKS;
                case 2 -> fuel.getLitTime();
                case 3 -> fuel.getLitDuration();
                case 4 -> tank.getFluidAmount();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 2 -> fuel.setLit(value, fuel.getLitDuration());
                case 3 -> fuel.setLit(fuel.getLitTime(), value);
                case 4 -> tank.setFluid(new FluidStack(ModFluids.LIQUID_RESONANCE.get(), value));
                default -> { }
            }
        }

        @Override
        public int getCount() {
            return 5;
        }
    };

    public CrystalCrucibleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRYSTAL_CRUCIBLE.get(), pos, state);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public ContainerData getContainerData() {
        return data;
    }

    /** The RF/FE buffer, exposed to adjacent cables as an alternative to solid fuel. */
    public MachineFuel getEnergyStorage() {
        return fuel;
    }

    /** Pipes can both fill and drain a crucible — it works the fluid in both directions. */
    public IFluidHandler getFluidHandler() {
        return tank;
    }

    /** A crystal worth melting: it must actually be holding something. */
    public static boolean isMeltable(ItemStack stack) {
        return stack.is(ModItems.CRYSTAL.get()) && CrystalCharge.get(stack) > 0;
    }

    /** A bucket the crucible can serve — empty to fill from the tank, or full to empty into it. */
    private static boolean isResonanceContainer(ItemStack stack) {
        return stack.is(Items.BUCKET) || stack.is(ModItems.LIQUID_RESONANCE_BUCKET.get());
    }

    /** Whether some infusion recipe takes this item (so the input slot should accept it). */
    private boolean hasInfusionRecipe(ItemStack stack) {
        return findInfusion(stack).isPresent();
    }

    /** The infusion recipe for this input, if there is one. */
    private Optional<ResonanceInfusionRecipe> findInfusion(ItemStack stack) {
        if (stack.isEmpty() || level == null) {
            return Optional.empty();
        }
        return level.getRecipeManager()
                .getRecipeFor(ModRecipes.RESONANCE_INFUSION.get(), new SingleRecipeInput(stack), level)
                .map(RecipeHolder::value);
    }

    // Cached per-face views: crystals + containers + fuel in from the top/sides, filled out the bottom.
    private SidedItemHandler topInsert;
    private SidedItemHandler sideInsert;
    private SidedItemHandler bottomOutput;

    /** The automation view for a given face; null side (internal access) sees the full inventory. */
    @Nullable
    public IItemHandler getInventoryForSide(@Nullable Direction side) {
        if (side == null) {
            return inventory;
        }
        // Masks are {crystal, container in, container out, fuel}.
        return switch (side) {
            case UP -> topInsert != null ? topInsert
                    : (topInsert = new SidedItemHandler(inventory,
                    new boolean[]{true, true, false, true}, new boolean[]{false, false, false, false}, null));
            case DOWN -> bottomOutput != null ? bottomOutput
                    : (bottomOutput = new SidedItemHandler(inventory,
                    new boolean[]{false, false, false, false}, new boolean[]{false, false, true, false}, null));
            default -> sideInsert != null ? sideInsert
                    : (sideInsert = new SidedItemHandler(inventory,
                    new boolean[]{false, true, false, true}, new boolean[]{false, false, false, false}, null));
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CrystalCrucibleBlockEntity be) {
        // Anything the input slot can do is just decanting the tank — instant, and it needs no fuel.
        // Melting is the work, so it alone owns the progress bar and the flame.
        be.processInput();

        // The front face only glows while fuel is actually burning.
        boolean lit = be.fuel.isLit();
        if (state.getValue(CrystalCrucibleBlock.LIT) != lit) {
            state = state.setValue(CrystalCrucibleBlock.LIT, lit);
            level.setBlock(pos, state, Block.UPDATE_ALL);
        }
        // Push the fill level, which is all the renderer draws.
        if (be.needsClientSync) {
            be.needsClientSync = false;
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }

        ItemStack crystal = be.inventory.getStackInSlot(SLOT_CRYSTAL);
        boolean work = be.canMelt(crystal);
        // Burn fuel furnace-style (lit fuel drains every tick); a tick melts only when lit + work.
        boolean powered = be.fuel.tick(work, be.inventory, SLOT_FUEL);

        if (!work) {
            boolean changed = be.fuel.isLit(); // a fuel still burning down while idle must be persisted
            if (be.progress != 0) {
                be.progress = 0;
                changed = true;
            }
            if (changed) {
                be.setChanged();
            }
            return;
        }
        if (powered) {
            be.progress++;
            if (be.progress >= MELT_TICKS) {
                be.progress = 0;
                be.melt(crystal);
            }
        }
        // else: primed but unpowered — hold progress, awaiting fuel/energy.
        be.setChanged();
    }

    /**
     * Serve whatever is waiting in the input slot: an empty bucket gets filled, a full one gets
     * emptied into the tank, an infusable material gets steeped. All three are instant — they only
     * pour in or out of the tank, so there's nothing to burn fuel for and nothing to time.
     */
    private void processInput() {
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
        if (input.isEmpty()) {
            return;
        }
        if (input.is(Items.BUCKET)) {
            fillBucket();
            return;
        }
        if (input.is(ModItems.LIQUID_RESONANCE_BUCKET.get())) {
            emptyBucket();
            return;
        }
        findInfusion(input).ifPresent(this::tryInfuse);
    }

    /** Steep one input in the tank, if the tank can pay for it and the result has somewhere to go. */
    private void tryInfuse(ResonanceInfusionRecipe recipe) {
        ItemStack result = recipe.getResultItem(level.registryAccess());
        if (tank.getFluidAmount() < recipe.resonance() || !hasRoomFor(result)) {
            return;
        }
        tank.drain(recipe.resonance(), IFluidHandler.FluidAction.EXECUTE);
        inventory.extractItem(SLOT_INPUT, 1, false);
        pushOutput(result.copy());
    }

    /** Whether the output slot could take this stack (empty, or the same item with room). */
    private boolean hasRoomFor(ItemStack result) {
        ItemStack out = inventory.getStackInSlot(SLOT_OUTPUT);
        return out.isEmpty()
                || (ItemStack.isSameItemSameComponents(out, result)
                    && out.getCount() + result.getCount() <= out.getMaxStackSize());
    }

    /** Put a finished stack into the output slot, merging with what's already there. */
    private void pushOutput(ItemStack result) {
        ItemStack out = inventory.getStackInSlot(SLOT_OUTPUT);
        if (out.isEmpty()) {
            inventory.setStackInSlot(SLOT_OUTPUT, result);
        } else {
            out.grow(result.getCount());
            inventory.setStackInSlot(SLOT_OUTPUT, out);
        }
    }

    /**
     * Whether this crystal can be melted right now: it must hold resonance, and the tank must have
     * room for all of it — a melt is all-or-nothing, so a nearly-full tank waits rather than
     * swallowing a crystal and spilling most of its charge.
     */
    private boolean canMelt(ItemStack crystal) {
        if (!isMeltable(crystal)) {
            return false;
        }
        int charge = CrystalCharge.get(crystal);
        return tank.getSpace() >= charge;
    }

    /** Consume the crystal and bank everything it held. */
    private void melt(ItemStack crystal) {
        int charge = CrystalCharge.get(crystal);
        tank.fill(new FluidStack(ModFluids.LIQUID_RESONANCE.get(), charge), IFluidHandler.FluidAction.EXECUTE);
        inventory.extractItem(SLOT_CRYSTAL, 1, false);
    }

    /**
     * Fill one waiting bucket from the tank, if there's a full bucket's worth and somewhere to put
     * the result.
     */
    private void fillBucket() {
        int cost = ModFluids.BUCKET;
        ItemStack filled = new ItemStack(ModItems.LIQUID_RESONANCE_BUCKET.get());
        if (tank.getFluidAmount() < cost) {
            return;
        }
        if (!hasRoomFor(filled)) {
            return;
        }
        tank.drain(cost, IFluidHandler.FluidAction.EXECUTE);
        inventory.extractItem(SLOT_INPUT, 1, false);
        pushOutput(filled);
    }

    /**
     * Empty a full resonance bucket into the tank, handing back the empty. Only runs when the whole
     * bucket fits, so a nearly-full tank waits rather than swallowing part of one.
     */
    private void emptyBucket() {
        ItemStack empty = new ItemStack(Items.BUCKET);
        if (tank.getSpace() < ModFluids.BUCKET || !hasRoomFor(empty)) {
            return;
        }
        tank.fill(new FluidStack(ModFluids.LIQUID_RESONANCE.get(), ModFluids.BUCKET),
                IFluidHandler.FluidAction.EXECUTE);
        inventory.extractItem(SLOT_INPUT, 1, false);
        pushOutput(empty);
    }

    // --- Client sync: how full the bowl is (drawn by CrystalCrucibleRenderer) ---

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        tank.readFromNBT(registries, tag.getCompound("Tank"));
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

    /** Client: 0..1 how full the bowl is, for the fill column the renderer draws. */
    public float fillFraction() {
        return TANK_CAPACITY <= 0 ? 0f : (float) tank.getFluidAmount() / TANK_CAPACITY;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("Progress", progress);
        fuel.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        tank.readFromNBT(registries, tag.getCompound("Tank"));
        progress = tag.getInt("Progress");
        fuel.load(tag);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.bmcrm.crystal_crucible");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CrystalCrucibleMenu(containerId, playerInventory, this, data);
    }
}
