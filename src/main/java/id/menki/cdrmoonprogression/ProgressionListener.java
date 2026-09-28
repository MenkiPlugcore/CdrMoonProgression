package id.menki.cdrmoonprogression;

import org.bukkit.event.Listener;

/**
 * v0.3.0 intentionally removes automatic block-break progression.
 * Progress only increases when players explicitly deposit configured
 * resources through the /progress contribution GUI.
 */
public final class ProgressionListener implements Listener {
    public ProgressionListener(ProgressionService service, PlacedBlockStore placedBlocks) {
        // Kept for binary/source structure compatibility with the existing bootstrap.
    }
}
