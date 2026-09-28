package id.menki.cdrmoonprogression;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public final class DimensionLockListener implements Listener {
    private final CdrMoonProgressionPlugin plugin;
    private final ProgressionService service;
    private final ProgressionDataStore data;

    public DimensionLockListener(CdrMoonProgressionPlugin plugin, ProgressionService service, ProgressionDataStore data) {
        this.plugin = plugin;
        this.service = service;
        this.data = data;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPortal(PlayerPortalEvent event) {
        check(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event instanceof PlayerPortalEvent) return;
        check(event);
    }

    private void check(PlayerTeleportEvent event) {
        if (!service.dimensionLockEnabled() || event.getTo() == null) return;
        Player player = event.getPlayer();
        if (player.hasPermission("cdrmoonprogression.bypass.dimension")) return;

        World targetWorld = event.getTo().getWorld();
        if (targetWorld == null || targetWorld.equals(event.getFrom().getWorld())) return;

        if (service.isNetherWorld(targetWorld) && !data.isNetherUnlocked()) {
            event.setCancelled(true);
            sendLocked(player, ProgressStage.OVERWORLD, "messages.locked-nether");
        } else if (service.isEndWorld(targetWorld) && !data.isEndUnlocked()) {
            event.setCancelled(true);
            sendLocked(player, ProgressStage.NETHER, "messages.locked-end");
        }
    }

    private void sendLocked(Player player, ProgressStage stage, String path) {
        String message = plugin.message(path, "&cDimension masih terkunci!")
                .replace("{current}", String.valueOf(data.getTotal(stage)))
                .replace("{target}", String.valueOf(service.target(stage)))
                .replace("{percent}", String.format(java.util.Locale.US, "%.1f", service.percent(stage)));
        player.sendMessage(plugin.color(plugin.prefix() + message));
    }
}
