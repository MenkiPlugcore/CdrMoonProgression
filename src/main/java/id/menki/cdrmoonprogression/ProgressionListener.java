package id.menki.cdrmoonprogression;

import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public final class ProgressionListener implements Listener {
    private final ProgressionService service;
    private final PlacedBlockStore placedBlocks;

    public ProgressionListener(ProgressionService service, PlacedBlockStore placedBlocks) {
        this.service = service;
        this.placedBlocks = placedBlocks;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!service.trackPlacedBlocks()) return;
        Block block = event.getBlockPlaced();
        if (service.isTrackedMaterial(block.getType())) placedBlocks.mark(block);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (service.trackPlacedBlocks() && placedBlocks.remove(block)) {
            return;
        }

        if (service.countOnlySurvival() && event.getPlayer().getGameMode() != GameMode.SURVIVAL) return;

        ProgressStage stage = service.stageForWorld(block.getWorld()).orElse(null);
        if (stage == null || !service.isStageActive(stage)) return;

        int value = service.blockValue(stage, block.getType());
        if (value <= 0) return;
        service.addProgress(stage, value, event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (service.trackPlacedBlocks()) placedBlocks.moveTracked(event.getBlocks(), event.getDirection());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (service.trackPlacedBlocks()) placedBlocks.moveTracked(event.getBlocks(), event.getDirection());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (service.trackPlacedBlocks()) placedBlocks.removeAll(event.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (service.trackPlacedBlocks()) placedBlocks.removeAll(event.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        if (service.trackPlacedBlocks()) placedBlocks.remove(event.getBlock());
    }
}
