package com.beepsterr.resourcegenerators.block;

import com.beepsterr.resourcegenerators.crystal.CrystalCharge;
import com.beepsterr.resourcegenerators.crystal.CrystalData;
import com.beepsterr.resourcegenerators.registry.ModBlockEntities;
import com.beepsterr.resourcegenerators.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Block entity for a placed crystal. Stores the crystal's {@link CrystalData} (tier + resource) so
 * the resonator can read it and the client can tint the block. Synced to the client and preserved
 * across break/place via implicit components + a copy_components loot table.
 */
public class PlacedCrystalBlockEntity extends BlockEntity {

    private static final String KEY = "CrystalData";
    private static final String OWNER_KEY = "Owner";
    private static final String CHARGE_KEY = "Charge";

    @Nullable
    private CrystalData data;
    /** Position of the resonator that has claimed this crystal (one crystal → one resonator). */
    @Nullable
    private BlockPos owner;
    /** Resonance accumulated while placed, in mB; capped by the tier and released by melting. */
    private int charge;

    public PlacedCrystalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PLACED_CRYSTAL.get(), pos, state);
    }

    @Nullable
    public CrystalData getCrystalData() {
        return data;
    }

    public void setCrystalData(@Nullable CrystalData data) {
        this.data = data;
        setChanged();
    }

    public int getCharge() {
        return charge;
    }

    /**
     * Store one generation's worth of resonance. Silently does nothing once the crystal is saturated
     * (or for a tier with no capacity at all), so a long-running crystal stops banking rather than
     * growing without bound.
     */
    public void addCharge(int amount) {
        if (data == null || amount <= 0) {
            return;
        }
        int capacity = CrystalCharge.capacity(data);
        int updated = Math.min(capacity, charge + amount);
        if (updated != charge) {
            charge = updated;
            setChanged();
        }
    }

    @Nullable
    public BlockPos getOwner() {
        return owner;
    }

    public void setOwner(@Nullable BlockPos owner) {
        if (Objects.equals(this.owner, owner)) {
            return;
        }
        this.owner = owner;
        setChanged();
        // setChanged() only marks for saving; push a block-entity update so clients (e.g. the Tuning
        // Fork's crystal->resonator ping) see the new owner without a world reload.
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (data != null) {
            var ops = registries.createSerializationContext(NbtOps.INSTANCE);
            CrystalData.CODEC.encodeStart(ops, data).result().ifPresent(t -> tag.put(KEY, t));
        }
        if (owner != null) {
            tag.putLong(OWNER_KEY, owner.asLong());
        }
        if (charge > 0) {
            tag.putInt(CHARGE_KEY, charge);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.data = null;
        if (tag.contains(KEY)) {
            var ops = registries.createSerializationContext(NbtOps.INSTANCE);
            this.data = CrystalData.CODEC.parse(ops, tag.get(KEY)).result().orElse(null);
        }
        this.owner = tag.contains(OWNER_KEY) ? BlockPos.of(tag.getLong(OWNER_KEY)) : null;
        this.charge = tag.getInt(CHARGE_KEY);
    }

    // --- Item <-> block data transfer (place & break) ---

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        CrystalData applied = input.get(ModDataComponents.CRYSTAL_DATA.get());
        if (applied != null) {
            this.data = applied;
        }
        Integer storedCharge = input.get(ModDataComponents.CRYSTAL_CHARGE.get());
        if (storedCharge != null) {
            // Clamp on the way in: the tier's capacity may have been lowered by a datapack change.
            this.charge = this.data == null ? 0 : Math.min(storedCharge, CrystalCharge.capacity(this.data));
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        if (data != null) {
            builder.set(ModDataComponents.CRYSTAL_DATA.get(), data);
        }
        if (charge > 0) {
            builder.set(ModDataComponents.CRYSTAL_CHARGE.get(), charge);
        }
    }

    // --- Client sync (so BlockColor can tint by resource) ---

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
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
            loadAdditional(tag, registries);
        }
    }
}
