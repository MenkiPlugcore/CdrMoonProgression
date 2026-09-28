package id.menki.cdrmoonprogression;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ProgressionDataStore {
    private final CdrMoonProgressionPlugin plugin;
    private final File file;
    private final EnumMap<ProgressStage, Long> totals = new EnumMap<>(ProgressStage.class);
    private final Map<UUID, EnumMap<ProgressStage, Long>> contributions = new HashMap<>();
    private final EnumMap<ProgressStage, Set<Integer>> claimedMilestones = new EnumMap<>(ProgressStage.class);

    private boolean netherUnlocked;
    private boolean endUnlocked;
    private boolean milestonesInitialized;
    private boolean dirty;

    public ProgressionDataStore(CdrMoonProgressionPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
        for (ProgressStage stage : ProgressStage.values()) {
            totals.put(stage, 0L);
            claimedMilestones.put(stage, new HashSet<>());
        }
    }

    public void load() {
        if (!file.exists()) return;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (ProgressStage stage : ProgressStage.values()) {
            totals.put(stage, Math.max(0L, yaml.getLong("progress." + stage.key(), 0L)));
        }
        netherUnlocked = yaml.getBoolean("unlocked.nether", false);
        endUnlocked = yaml.getBoolean("unlocked.end", false);
        milestonesInitialized = yaml.getBoolean("milestones.initialized", false);

        contributions.clear();
        ConfigurationSection root = yaml.getConfigurationSection("contributions");
        if (root != null) {
            for (String rawUuid : root.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(rawUuid);
                    EnumMap<ProgressStage, Long> values = new EnumMap<>(ProgressStage.class);
                    for (ProgressStage stage : ProgressStage.values()) {
                        long amount = Math.max(0L, root.getLong(rawUuid + "." + stage.key(), 0L));
                        if (amount > 0) values.put(stage, amount);
                    }
                    if (!values.isEmpty()) contributions.put(uuid, values);
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("Mengabaikan UUID invalid di data.yml: " + rawUuid);
                }
            }
        }

        for (ProgressStage stage : ProgressStage.values()) {
            Set<Integer> values = claimedMilestones.computeIfAbsent(stage, ignored -> new HashSet<>());
            values.clear();
            for (int percent : yaml.getIntegerList("milestones.claimed." + stage.key())) {
                if (percent > 0 && percent <= 100) values.add(percent);
            }
        }
        dirty = false;
    }

    public void saveIfDirty() {
        if (dirty) save();
    }

    public void save() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Gagal membuat folder plugin untuk data.yml");
            return;
        }

        YamlConfiguration yaml = new YamlConfiguration();
        for (ProgressStage stage : ProgressStage.values()) {
            yaml.set("progress." + stage.key(), getTotal(stage));
        }
        yaml.set("unlocked.nether", netherUnlocked);
        yaml.set("unlocked.end", endUnlocked);
        yaml.set("milestones.initialized", milestonesInitialized);

        for (ProgressStage stage : ProgressStage.values()) {
            List<Integer> claimed = new ArrayList<>(claimedMilestones.getOrDefault(stage, Set.of()));
            claimed.sort(Integer::compareTo);
            yaml.set("milestones.claimed." + stage.key(), claimed);
        }

        for (Map.Entry<UUID, EnumMap<ProgressStage, Long>> entry : contributions.entrySet()) {
            for (ProgressStage stage : ProgressStage.values()) {
                long amount = entry.getValue().getOrDefault(stage, 0L);
                if (amount > 0) {
                    yaml.set("contributions." + entry.getKey() + "." + stage.key(), amount);
                }
            }
        }

        try {
            yaml.save(file);
            dirty = false;
        } catch (IOException ex) {
            plugin.getLogger().severe("Gagal menyimpan data.yml: " + ex.getMessage());
        }
    }

    public long getTotal(ProgressStage stage) {
        return totals.getOrDefault(stage, 0L);
    }

    public void setTotal(ProgressStage stage, long value) {
        totals.put(stage, Math.max(0L, value));
        dirty = true;
    }

    public long addTotal(ProgressStage stage, long amount) {
        long result = Math.max(0L, getTotal(stage) + amount);
        totals.put(stage, result);
        dirty = true;
        return result;
    }

    public long getContribution(UUID uuid, ProgressStage stage) {
        EnumMap<ProgressStage, Long> values = contributions.get(uuid);
        return values == null ? 0L : values.getOrDefault(stage, 0L);
    }

    public void addContribution(UUID uuid, ProgressStage stage, long amount) {
        if (uuid == null || amount <= 0) return;
        EnumMap<ProgressStage, Long> values = contributions.computeIfAbsent(uuid, ignored -> new EnumMap<>(ProgressStage.class));
        values.put(stage, values.getOrDefault(stage, 0L) + amount);
        dirty = true;
    }

    public boolean isMilestoneClaimed(ProgressStage stage, int percent) {
        return claimedMilestones.getOrDefault(stage, Set.of()).contains(percent);
    }

    public void markMilestoneClaimed(ProgressStage stage, int percent) {
        if (percent <= 0 || percent > 100) return;
        claimedMilestones.computeIfAbsent(stage, ignored -> new HashSet<>()).add(percent);
        dirty = true;
    }

    public Set<Integer> claimedMilestones(ProgressStage stage) {
        return Set.copyOf(claimedMilestones.getOrDefault(stage, Set.of()));
    }

    public boolean milestonesInitialized() {
        return milestonesInitialized;
    }

    public void setMilestonesInitialized(boolean value) {
        milestonesInitialized = value;
        dirty = true;
    }

    public void resetStage(ProgressStage stage) {
        totals.put(stage, 0L);
        for (EnumMap<ProgressStage, Long> values : contributions.values()) {
            values.remove(stage);
        }
        contributions.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        claimedMilestones.computeIfAbsent(stage, ignored -> new HashSet<>()).clear();
        dirty = true;
    }

    public void resetAll() {
        for (ProgressStage stage : ProgressStage.values()) totals.put(stage, 0L);
        contributions.clear();
        for (Set<Integer> values : claimedMilestones.values()) values.clear();
        netherUnlocked = false;
        endUnlocked = false;
        milestonesInitialized = true;
        dirty = true;
    }

    public boolean isNetherUnlocked() {
        return netherUnlocked;
    }

    public void setNetherUnlocked(boolean value) {
        netherUnlocked = value;
        if (!value) endUnlocked = false;
        dirty = true;
    }

    public boolean isEndUnlocked() {
        return endUnlocked;
    }

    public void setEndUnlocked(boolean value) {
        endUnlocked = value;
        dirty = true;
    }

    public List<Contribution> top(ProgressStage stage, int limit) {
        List<Contribution> result = new ArrayList<>();
        for (Map.Entry<UUID, EnumMap<ProgressStage, Long>> entry : contributions.entrySet()) {
            long value = entry.getValue().getOrDefault(stage, 0L);
            if (value > 0) result.add(new Contribution(entry.getKey(), value));
        }
        result.sort(Comparator.comparingLong(Contribution::value).reversed());
        return result.size() <= limit ? result : new ArrayList<>(result.subList(0, limit));
    }

    public record Contribution(UUID playerId, long value) {}
}
