package id.menki.cdrmoonprogression;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class PersonalGoalService {
    private final CdrMoonProgressionPlugin plugin;
    private final ProgressionDataStore data;
    private final EnumMap<ProgressStage, List<Goal>> goals = new EnumMap<>(ProgressStage.class);
    private boolean enabled;

    public PersonalGoalService(CdrMoonProgressionPlugin plugin, ProgressionDataStore data) {
        this.plugin = plugin;
        this.data = data;
        reloadFromConfig();
    }

    public void reloadFromConfig() {
        enabled = plugin.getConfig().getBoolean("personal-goals.enabled", true);
        goals.clear();
        for (ProgressStage stage : ProgressStage.values()) {
            goals.put(stage, loadGoals(stage));
        }
    }

    private List<Goal> loadGoals(ProgressStage stage) {
        List<Goal> result = new ArrayList<>();
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("personal-goals." + stage.key());
        if (root == null) return List.of();

        for (String rawGoal : root.getKeys(false)) {
            long points;
            try {
                points = Long.parseLong(rawGoal);
            } catch (NumberFormatException ex) {
                plugin.getLogger().warning("Personal goal invalid untuk " + stage.key() + ": " + rawGoal);
                continue;
            }
            if (points <= 0L) continue;

            String path = "personal-goals." + stage.key() + "." + rawGoal;
            String name = plugin.getConfig().getString(path + ".name", "&bContribution Goal &f" + points);
            Material icon = Material.matchMaterial(plugin.getConfig().getString(path + ".icon", "CHEST"));
            if (icon == null || icon.isAir()) icon = Material.CHEST;
            List<String> lore = List.copyOf(plugin.getConfig().getStringList(path + ".lore"));
            result.add(new Goal(points, name, icon, lore));
        }

        result.sort(Comparator.comparingLong(Goal::points));
        return List.copyOf(result);
    }

    public boolean enabled() {
        return enabled;
    }

    public List<Goal> goals(ProgressStage stage) {
        return goals.getOrDefault(stage, List.of());
    }

    public boolean isConfiguredGoal(ProgressStage stage, long points) {
        for (Goal goal : goals(stage)) {
            if (goal.points() == points) return true;
        }
        return false;
    }

    public Status status(UUID playerId, ProgressStage stage, long goalPoints) {
        if (data.isPersonalGoalRewarded(playerId, stage, goalPoints)) return Status.REWARDED;
        long contribution = data.getContribution(playerId, stage);
        return contribution >= goalPoints ? Status.PENDING_REWARD : Status.LOCKED;
    }

    public List<PendingGoal> pending(UUID playerId, ProgressStage stage) {
        List<PendingGoal> result = new ArrayList<>();
        for (Goal goal : goals(stage)) {
            if (status(playerId, stage, goal.points()) == Status.PENDING_REWARD) {
                result.add(new PendingGoal(playerId, stage, goal));
            }
        }
        return result;
    }

    public List<PendingGoal> pendingAll(ProgressStage stageFilter) {
        List<PendingGoal> result = new ArrayList<>();
        for (UUID playerId : data.contributorIds()) {
            for (ProgressStage stage : ProgressStage.values()) {
                if (stageFilter != null && stage != stageFilter) continue;
                result.addAll(pending(playerId, stage));
            }
        }
        result.sort(Comparator
                .comparing((PendingGoal pending) -> pending.stage().ordinal())
                .thenComparingLong(pending -> pending.goal().points())
                .thenComparing(pending -> pending.playerId().toString()));
        return result;
    }

    public boolean markRewarded(UUID playerId, ProgressStage stage, long goalPoints) {
        if (!enabled || !isConfiguredGoal(stage, goalPoints)) return false;
        if (data.getContribution(playerId, stage) < goalPoints) return false;
        data.markPersonalGoalRewarded(playerId, stage, goalPoints);
        return true;
    }

    public boolean unmarkRewarded(UUID playerId, ProgressStage stage, long goalPoints) {
        if (!isConfiguredGoal(stage, goalPoints)) return false;
        data.unmarkPersonalGoalRewarded(playerId, stage, goalPoints);
        return true;
    }

    public long nextGoalPoints(UUID playerId, ProgressStage stage) {
        long contribution = data.getContribution(playerId, stage);
        for (Goal goal : goals(stage)) {
            if (contribution < goal.points()) return goal.points();
        }
        return -1L;
    }

    public String prettyStatus(Status status) {
        return switch (status) {
            case LOCKED -> "LOCKED";
            case PENDING_REWARD -> "PENDING REWARD";
            case REWARDED -> "REWARDED";
        };
    }

    public enum Status {
        LOCKED,
        PENDING_REWARD,
        REWARDED
    }

    public record Goal(long points, String name, Material icon, List<String> lore) {}

    public record PendingGoal(UUID playerId, ProgressStage stage, Goal goal) {}
}
