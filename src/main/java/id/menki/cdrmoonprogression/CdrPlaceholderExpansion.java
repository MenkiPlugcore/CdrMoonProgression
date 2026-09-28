package id.menki.cdrmoonprogression;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class CdrPlaceholderExpansion extends PlaceholderExpansion {
    private final CdrMoonProgressionPlugin plugin;
    private final ProgressionService service;
    private final ProgressionDataStore data;

    public CdrPlaceholderExpansion(CdrMoonProgressionPlugin plugin, ProgressionService service, ProgressionDataStore data) {
        this.plugin = plugin;
        this.service = service;
        this.data = data;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "cdrmoonprogression";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Cadera";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        String key = params.toLowerCase(Locale.ROOT);
        return switch (key) {
            case "stage" -> service.activeStage().map(ProgressStage::key).orElse("complete");
            case "overworld_progress" -> String.valueOf(data.getTotal(ProgressStage.OVERWORLD));
            case "overworld_target" -> String.valueOf(service.target(ProgressStage.OVERWORLD));
            case "overworld_percent" -> oneDecimal(service.percent(ProgressStage.OVERWORLD));
            case "overworld_requirements_complete" -> String.valueOf(service.completedRequirementCount(ProgressStage.OVERWORLD));
            case "overworld_requirements_total" -> String.valueOf(service.totalRequirementCount(ProgressStage.OVERWORLD));
            case "overworld_requirements_satisfied" -> String.valueOf(service.requirementsSatisfied(ProgressStage.OVERWORLD));
            case "nether_progress" -> String.valueOf(data.getTotal(ProgressStage.NETHER));
            case "nether_target" -> String.valueOf(service.target(ProgressStage.NETHER));
            case "nether_percent" -> oneDecimal(service.percent(ProgressStage.NETHER));
            case "nether_requirements_complete" -> String.valueOf(service.completedRequirementCount(ProgressStage.NETHER));
            case "nether_requirements_total" -> String.valueOf(service.totalRequirementCount(ProgressStage.NETHER));
            case "nether_requirements_satisfied" -> String.valueOf(service.requirementsSatisfied(ProgressStage.NETHER));
            case "nether_unlocked" -> String.valueOf(data.isNetherUnlocked());
            case "end_unlocked" -> String.valueOf(data.isEndUnlocked());
            case "current_progress" -> service.activeStage().map(stage -> String.valueOf(data.getTotal(stage))).orElse("0");
            case "current_target" -> service.activeStage().map(stage -> String.valueOf(service.target(stage))).orElse("0");
            case "current_percent" -> service.activeStage().map(stage -> oneDecimal(service.percent(stage))).orElse("100.0");
            case "current_requirements_complete" -> service.activeStage().map(stage -> String.valueOf(service.completedRequirementCount(stage))).orElse("0");
            case "current_requirements_total" -> service.activeStage().map(stage -> String.valueOf(service.totalRequirementCount(stage))).orElse("0");
            case "current_requirements_satisfied" -> service.activeStage().map(stage -> String.valueOf(service.requirementsSatisfied(stage))).orElse("true");
            case "personal_overworld" -> player == null ? "0" : String.valueOf(data.getContribution(player.getUniqueId(), ProgressStage.OVERWORLD));
            case "personal_nether" -> player == null ? "0" : String.valueOf(data.getContribution(player.getUniqueId(), ProgressStage.NETHER));
            case "personal_current" -> {
                if (player == null) yield "0";
                yield service.activeStage().map(stage -> String.valueOf(data.getContribution(player.getUniqueId(), stage))).orElse("0");
            }
            default -> null;
        };
    }

    private String oneDecimal(double value) {
        return String.format(Locale.US, "%.1f", value);
    }
}
