package com.beepsterr.resourcegenerators.event;

import com.beepsterr.resourcegenerators.BeepsResourceGenerators;
import com.beepsterr.resourcegenerators.Config;
import com.beepsterr.resourcegenerators.crystal.CrystalData;
import com.beepsterr.resourcegenerators.crystal.CrystalInfusion;
import com.beepsterr.resourcegenerators.crystal.EntityResource;
import com.beepsterr.resourcegenerators.registry.ModDataComponents;
import com.beepsterr.resourcegenerators.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Optional;

/**
 * Mob crystals are attuned in the field rather than in the Infuser, by either of two routes:
 *
 * <ul>
 *   <li><b>Killing mobs</b> with a blank crystal in the off hand. The first kill locks the crystal
 *       to that mob and each further kill fills it, reusing the same {@link CrystalInfusion}
 *       accumulator (and durability bar) the Infuser uses for materials.</li>
 *   <li><b>Right-clicking a spawner</b> with a blank crystal, which attunes it in one go and leaves
 *       the spawner empty. Costly, but it turns a dungeon find into a finished crystal.</li>
 * </ul>
 *
 * Both paths require a <em>single</em> blank crystal — components apply to a whole stack, so a
 * stack of eight would otherwise attune all eight for one mob's worth of effort.
 */
@EventBusSubscriber(modid = BeepsResourceGenerators.MOD_ID)
public final class CrystalAttunement {

    private CrystalAttunement() {}

    /** A lone blank crystal, or empty if this stack can't be attuned. */
    private static Optional<CrystalData> blankCrystal(ItemStack stack) {
        if (stack.getCount() != 1 || !stack.is(ModItems.CRYSTAL.get())) {
            return Optional.empty();
        }
        CrystalData data = stack.get(ModDataComponents.CRYSTAL_DATA.get());
        return data == null || data.resource().isPresent() ? Optional.empty() : Optional.of(data);
    }

    private static Holder<EntityType<?>> holderOf(EntityType<?> type) {
        return BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type);
    }

    // --- Path 1: kill mobs holding a blank crystal in the off hand ---

    @SubscribeEvent
    public static void onMobKilled(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        EntityType<?> type = event.getEntity().getType();
        if (!EntityResource.isAttunable(type)) {
            return; // players, MISC entities and blacklisted bosses never attune
        }
        ItemStack crystal = player.getOffhandItem();
        Optional<CrystalData> blank = blankCrystal(crystal);
        if (blank.isEmpty()) {
            return;
        }

        EntityResource target = new EntityResource(holderOf(type));
        CrystalInfusion infusion = crystal.get(ModDataComponents.CRYSTAL_INFUSION.get());
        if (infusion == null) {
            infusion = new CrystalInfusion(target, 0, Config.MOB_CRYSTAL_KILLS.get());
        } else if (!infusion.target().equals(target)) {
            return; // locked to a different mob; this kill doesn't count
        }
        infusion = infusion.plus(1);

        if (infusion.isComplete()) {
            complete(player, crystal, blank.get(), target);
        } else {
            crystal.set(ModDataComponents.CRYSTAL_INFUSION.get(), infusion);
        }
    }

    // --- Path 2: right-click a spawner, draining it ---

    @SubscribeEvent
    public static void onSpawnerClicked(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack crystal = event.getItemStack();
        Optional<CrystalData> blank = blankCrystal(crystal);
        if (blank.isEmpty()) {
            return;
        }
        BlockPos pos = event.getPos();
        if (!(level.getBlockEntity(pos) instanceof SpawnerBlockEntity spawner)) {
            return;
        }
        Optional<EntityType<?>> type = spawnerEntity(spawner, level);
        if (type.isEmpty() || !EntityResource.isAttunable(type.get())) {
            return; // already drained, unreadable, or a blacklisted mob
        }

        complete(player, crystal, blank.get(), new EntityResource(holderOf(type.get())));
        drain(level, pos, spawner);

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    /** The mob a spawner is set to, read from its synced NBT (there is no public getter). */
    private static Optional<EntityType<?>> spawnerEntity(SpawnerBlockEntity spawner, ServerLevel level) {
        CompoundTag tag = spawner.getUpdateTag(level.registryAccess());
        if (!tag.contains("SpawnData")) {
            return Optional.empty();
        }
        String id = tag.getCompound("SpawnData").getCompound("entity").getString("id");
        return id.isEmpty() ? Optional.empty() : EntityType.byString(id);
    }

    /**
     * Empty the spawner by giving it a fresh block entity. {@code BaseSpawner.load} only ever
     * <em>sets</em> spawn data and never clears it, so there is no in-place way to blank one; a new
     * block entity leaves the spawner in exactly the state a creative-placed one is in — present,
     * inert, and configurable again with a spawn egg.
     */
    private static void drain(ServerLevel level, BlockPos pos, SpawnerBlockEntity spawner) {
        BlockState state = level.getBlockState(pos);
        level.removeBlockEntity(pos);
        level.setBlockEntity(new SpawnerBlockEntity(pos, state));
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0f, 1.4f);
    }

    /** Finish a crystal: stamp the resource on, drop any part-done infusion, and make a noise. */
    private static void complete(ServerPlayer player, ItemStack crystal, CrystalData data,
                                 EntityResource target) {
        crystal.set(ModDataComponents.CRYSTAL_DATA.get(), data.withResource(target));
        crystal.remove(ModDataComponents.CRYSTAL_INFUSION.get());
        player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 1.0f, 1.2f);
    }
}
