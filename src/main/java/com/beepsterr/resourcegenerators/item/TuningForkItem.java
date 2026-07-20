package com.beepsterr.resourcegenerators.item;

import com.beepsterr.resourcegenerators.block.AreaPreview;
import com.beepsterr.resourcegenerators.block.PlacedCrystalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The Tuning Fork: a diagnostic "wrench" (tagged {@code c:tools/wrench} for cross-mod interop) that
 * visualises the mod's invisible spatial rules. Right-clicking a block whose BE has an
 * {@link AreaPreview} toggles a wireframe of its area (instead of opening its GUI); right-clicking a
 * placed crystal briefly flashes the resonator that owns it. All overlay state is client-side.
 */
public class TuningForkItem extends Item {

    public TuningForkItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockEntity be = level.getBlockEntity(pos);
        Player player = context.getPlayer();

        if (be instanceof AreaPreview) {
            // Toggle the overlay instead of opening the block's GUI.
            if (level.isClientSide) {
                boolean shown = com.beepsterr.resourcegenerators.client.TuningForkOverlay.toggle(pos);
                feedback(player, shown ? "shown" : "hidden");
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (be instanceof PlacedCrystalBlockEntity crystal) {
            if (level.isClientSide) {
                BlockPos owner = crystal.getOwner();
                if (owner != null) {
                    com.beepsterr.resourcegenerators.client.TuningForkOverlay.pingResonator(level, pos, owner);
                    feedback(player, "linked");
                } else {
                    feedback(player, "unlinked");
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // Anything else (e.g. another mod's wrenchable machine) is left to normal handling.
        return InteractionResult.PASS;
    }

    private static void feedback(Player player, String key) {
        if (player != null) {
            player.displayClientMessage(
                    Component.translatable("item.bmcrm.tuning_fork." + key), true);
        }
    }
}
