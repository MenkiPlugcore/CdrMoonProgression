package id.menki.cdrmoonprogression;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class ContributionHistoryStore {
    private final CdrMoonProgressionPlugin plugin;
    private final File file;
    private final List<Entry> entries = new ArrayList<>();
    private int maxEntries;
    private boolean dirty;

    public ContributionHistoryStore(CdrMoonProgressionPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "history.yml");
        reloadSettings();
    }

    public void reloadSettings() {
        maxEntries = Math.max(50, plugin.getConfig().getInt("history.max-entries", 1000));
        trim();
    }

    public void load() {
        entries.clear();
        if (!file.exists()) return;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (var raw : yaml.getMapList("entries")) {
            try {
                UUID playerId = UUID.fromString(String.valueOf(raw.get("player-uuid")));
                String playerName = String.valueOf(raw.getOrDefault("player-name", "Unknown"));
                ProgressStage stage = ProgressStage.parse(String.valueOf(raw.get("stage"))).orElse(null);
                Material material = Material.matchMaterial(String.valueOf(raw.get("material")));
                int items = Integer.parseInt(String.valueOf(raw.getOrDefault("items", 0)));
                long points = Long.parseLong(String.valueOf(raw.getOrDefault("points", 0)));
                long timestamp = Long.parseLong(String.valueOf(raw.getOrDefault("timestamp", 0)));
                if (stage == null || material == null || items <= 0 || points <= 0 || timestamp <= 0) continue;
                entries.add(new Entry(playerId, playerName, stage, material, items, points, timestamp));
            } catch (Exception ex) {
                plugin.getLogger().warning("Mengabaikan entry history invalid: " + ex.getMessage());
            }
        }
        entries.sort((a, b) -> Long.compare(b.timestamp(), a.timestamp()));
        trim();
        dirty = false;
    }

    public void add(UUID playerId, String playerName, ProgressStage stage, Material material, int items, long points) {
        if (playerId == null || stage == null || material == null || items <= 0 || points <= 0) return;
        entries.add(0, new Entry(playerId, playerName == null ? "Unknown" : playerName, stage, material, items, points, Instant.now().toEpochMilli()));
        trim();
        dirty = true;
    }

    private void trim() {
        while (entries.size() > maxEntries) entries.remove(entries.size() - 1);
    }

    public List<Entry> all() {
        return Collections.unmodifiableList(entries);
    }

    public List<Entry> forStage(ProgressStage stage) {
        if (stage == null) return all();
        List<Entry> result = new ArrayList<>();
        for (Entry entry : entries) if (entry.stage() == stage) result.add(entry);
        return result;
    }

    public List<Entry> forPlayer(UUID playerId) {
        List<Entry> result = new ArrayList<>();
        for (Entry entry : entries) if (entry.playerId().equals(playerId)) result.add(entry);
        return result;
    }

    public int size() {
        return entries.size();
    }

    public void clear() {
        entries.clear();
        dirty = true;
    }

    public void saveIfDirty() {
        if (dirty) save();
    }

    public void save() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Gagal membuat folder plugin untuk history.yml");
            return;
        }

        YamlConfiguration yaml = new YamlConfiguration();
        List<java.util.Map<String, Object>> serialized = new ArrayList<>();
        for (Entry entry : entries) {
            java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("player-uuid", entry.playerId().toString());
            row.put("player-name", entry.playerName());
            row.put("stage", entry.stage().key());
            row.put("material", entry.material().name());
            row.put("items", entry.items());
            row.put("points", entry.points());
            row.put("timestamp", entry.timestamp());
            serialized.add(row);
        }
        yaml.set("entries", serialized);

        try {
            yaml.save(file);
            dirty = false;
        } catch (IOException ex) {
            plugin.getLogger().severe("Gagal menyimpan history.yml: " + ex.getMessage());
        }
    }

    public record Entry(UUID playerId, String playerName, ProgressStage stage, Material material,
                        int items, long points, long timestamp) {}
}
