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
    private final Map<UUID, EnumMap<ProgressStage, Set<Long>>> rewardedPersonalGoals = new HashMap<>();
    private final EnumMap<ProgressStage, Map<String, Long>> requirementProgress = new EnumMap<>(ProgressStage.class);

    private boolean netherUnlocked;
    private boolean endUnlocked;
    private boolean milestonesInitialized;
    private boolean requirementsInitialized;
    private boolean dirty;

    public ProgressionDataStore(CdrMoonProgressionPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
        for (ProgressStage stage : ProgressStage.values()) {
            totals.put(stage, 0L);
            claimedMilestones.put(stage, new HashSet<>());
            requirementProgress.put(stage, new HashMap<>());
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
        requirementsInitialized = yaml.getBoolean("requirements.initialized", false);

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

        rewardedPersonalGoals.clear();
        ConfigurationSection rewardRoot = yaml.getConfigurationSection("personal-goals.rewarded");
        if (rewardRoot != null) {
            for (String rawUuid : rewardRoot.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(rawUuid);
                    EnumMap<ProgressStage, Set<Long>> byStage = new EnumMap<>(ProgressStage.class);
                    for (ProgressStage stage : ProgressStage.values()) {
                        Set<Long> goals = new HashSet<>();
                        for (long goal : rewardRoot.getLongList(rawUuid + "." + stage.key())) {
                            if (goal > 0L) goals.add(goal);
                        }
                        if (!goals.isEmpty()) byStage.put(stage, goals);
                    }
                    if (!byStage.isEmpty()) rewardedPersonalGoals.put(uuid, byStage);
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("Mengabaikan UUID personal goal invalid di data.yml: " + rawUuid);
                }
            }
        }

        for (ProgressStage stage : ProgressStage.values()) {
            Map<String, Long> values = requirementProgress.computeIfAbsent(stage, ignored -> new HashMap<>());
            values.clear();
            ConfigurationSection requirementRoot = yaml.getConfigurationSection("requirements.progress." + stage.key());
            if (requirementRoot == null) continue;
            for (String id : requirementRoot.getKeys(false)) {
                long amount = Math.max(0L, requirementRoot.getLong(id, 0L));
                if (amount > 0L) values.put(id.toLowerCase(), amount);
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
        yaml.set("requirements.initialized", requirementsInitialized);

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

        for (Map.Entry<UUID, EnumMap<ProgressStage, Set<Long>>> entry : rewardedPersonalGoals.entrySet()) {
            for (ProgressStage stage : ProgressStage.values()) {
                Set<Long> values = entry.getValue().get(stage);
                if (values == null || values.isEmpty()) continue;
                List<Long> sorted = new ArrayList<>(values);
                sorted.sort(Long::compareTo);
                yaml.set("personal-goals.rewarded." + entry.getKey() + "." + stage.key(), sorted);
            }
        }

        for (ProgressStage stage : ProgressStage.values()) {
            for (Map.Entry<String, Long> entry : requirementProgress.getOrDefault(stage, Map.of()).entrySet()) {
                if (entry.getValue() > 0L) {
                    yaml.set("requirements.progress." + stage.key() + "." + entry.getKey(), entry.getValue());
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

    public Set<UUID> contributorIds() {
        return Set.copyOf(contributions.keySet());
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

    public boolean requirementsInitialized() {
        return requirementsInitialized;
    }

    public void setRequirementsInitialized(boolean value) {
        requirementsInitialized = value;
        dirty = true;
    }

    public long getRequirementProgress(ProgressStage stage, String requirementId) {
        if (stage == null || requirementId == null) return 0L;
        return requirementProgress.getOrDefault(stage, Map.of()).getOrDefault(requirementId.toLowerCase(), 0L);
    }

    public void setRequirementProgress(ProgressStage stage, String requirementId, long value) {
        if (stage == null || requirementId == null || requirementId.isBlank()) return;
        Map<String, Long> values = requirementProgress.computeIfAbsent(stage, ignored -> new HashMap<>());
        String key = requirementId.toLowerCase();
        long normalized = Math.max(0L, value);
        if (normalized == 0L) values.remove(key);
        else values.put(key, normalized);
        dirty = true;
    }

    public long addRequirementProgress(ProgressStage stage, String requirementId, long amount) {
        long result = Math.max(0L, getRequirementProgress(stage, requirementId) + amount);
        setRequirementProgress(stage, requirementId, result);
        return result;
    }

    public Map<String, Long> requirementProgress(ProgressStage stage) {
        return Map.copyOf(requirementProgress.getOrDefault(stage, Map.of()));
    }

    public void clearRequirementProgress(ProgressStage stage) {
        requirementProgress.computeIfAbsent(stage, ignored -> new HashMap<>()).clear();
        dirty = true;
    }

    public boolean isPersonalGoalRewarded(UUID uuid, ProgressStage stage, long goal) {
        EnumMap<ProgressStage, Set<Long>> byStage = rewardedPersonalGoals.get(uuid);
        if (byStage == null) return false;
        return byStage.getOrDefault(stage, Set.of()).contains(goal);
    }

    public void markPersonalGoalRewarded(UUID uuid, ProgressStage stage, long goal) {
        if (uuid == null || stage == null || goal <= 0L) return;
        EnumMap<ProgressStage, Set<Long>> byStage = rewardedPersonalGoals.computeIfAbsent(uuid, ignored -> new EnumMap<>(ProgressStage.class));
        byStage.computeIfAbsent(stage, ignored -> new HashSet<>()).add(goal);
        dirty = true;
    }

    public void unmarkPersonalGoalRewarded(UUID uuid, ProgressStage stage, long goal) {
        EnumMap<ProgressStage, Set<Long>> byStage = rewardedPersonalGoals.get(uuid);
        if (byStage == null) return;
        Set<Long> values = byStage.get(stage);
        if (values == null) return;
        if (values.remove(goal)) dirty = true;
        if (values.isEmpty()) byStage.remove(stage);
        if (byStage.isEmpty()) rewardedPersonalGoals.remove(uuid);
    }

    public void resetStage(ProgressStage stage) {
        totals.put(stage, 0L);
        for (EnumMap<ProgressStage, Long> values : contributions.values()) {
            values.remove(stage);
        }
        contributions.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        claimedMilestones.computeIfAbsent(stage, ignored -> new HashSet<>()).clear();
        for (EnumMap<ProgressStage, Set<Long>> values : rewardedPersonalGoals.values()) {
            values.remove(stage);
        }
        rewardedPersonalGoals.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        clearRequirementProgress(stage);
        dirty = true;
    }

    public void resetAll() {
        for (ProgressStage stage : ProgressStage.values()) {
            totals.put(stage, 0L);
            requirementProgress.computeIfAbsent(stage, ignored -> new HashMap<>()).clear();
        }
        contributions.clear();
        for (Set<Integer> values : claimedMilestones.values()) values.clear();
        rewardedPersonalGoals.clear();
        netherUnlocked = false;
        endUnlocked = false;
        milestonesInitialized = true;
        requirementsInitialized = true;
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
