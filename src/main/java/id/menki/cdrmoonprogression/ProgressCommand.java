package id.menki.cdrmoonprogression;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ProgressCommand implements TabExecutor {
    private final CdrMoonProgressionPlugin plugin;
    private final ProgressionService service;
    private final ProgressionDataStore data;

    public ProgressCommand(CdrMoonProgressionPlugin plugin, ProgressionService service, ProgressionDataStore data) {
        this.plugin = plugin;
        this.service = service;
        this.data = data;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("top")) {
            ProgressStage stage = args.length > 1
                    ? ProgressStage.parse(args[1]).orElse(null)
                    : service.activeStage().orElse(ProgressStage.NETHER);
            if (stage == null) {
                sender.sendMessage(plugin.color(plugin.prefix() + "&cStage harus overworld atau nether."));
                return true;
            }
            showTop(sender, stage);
            return true;
        }

        ProgressStage stage = service.activeStage().orElse(null);
        sender.sendMessage(plugin.color("&8&m----------------------------------------"));
        sender.sendMessage(plugin.color("&b&lMOON DIMENSION PROGRESSION"));
        if (stage == null) {
            sender.sendMessage(plugin.color("&aSemua dimension progression telah selesai."));
            showStage(sender, ProgressStage.OVERWORLD);
            showStage(sender, ProgressStage.NETHER);
        } else {
            showStage(sender, stage);
            if (sender instanceof Player player) {
                long personal = data.getContribution(player.getUniqueId(), stage);
                sender.sendMessage(plugin.color("&7Kontribusi kamu: &f" + format(personal) + " point"));
            }
            sender.sendMessage(plugin.color("&7Dimension berikutnya: &f" + (stage == ProgressStage.OVERWORLD ? "The Nether" : "The End")));
        }
        sender.sendMessage(plugin.color("&7Gunakan &f/progress top &7untuk leaderboard."));
        sender.sendMessage(plugin.color("&8&m----------------------------------------"));
        return true;
    }

    private void showStage(CommandSender sender, ProgressStage stage) {
        long current = data.getTotal(stage);
        long target = service.target(stage);
        double percent = service.percent(stage);
        sender.sendMessage(plugin.color("&f" + stage.displayName() + ": &b" + format(current) + "&7/&f" + format(target) + " &7(" + String.format(Locale.US, "%.1f", percent) + "%)"));
        sender.sendMessage(plugin.color("&b" + progressBar(percent) + " &7" + String.format(Locale.US, "%.1f%%", percent)));
    }

    private void showTop(CommandSender sender, ProgressStage stage) {
        sender.sendMessage(plugin.color("&8&m----------------------------------------"));
        sender.sendMessage(plugin.color("&b&lTOP CONTRIBUTOR &8- &f" + stage.displayName()));
        List<ProgressionDataStore.Contribution> top = data.top(stage, 10);
        if (top.isEmpty()) {
            sender.sendMessage(plugin.color("&7Belum ada kontribusi."));
        } else {
            int rank = 1;
            for (ProgressionDataStore.Contribution entry : top) {
                OfflinePlayer offline = Bukkit.getOfflinePlayer(entry.playerId());
                String name = offline.getName() == null ? entry.playerId().toString().substring(0, 8) : offline.getName();
                sender.sendMessage(plugin.color("&b#" + rank + " &f" + name + " &8- &7" + format(entry.value()) + " point"));
                rank++;
            }
        }
        sender.sendMessage(plugin.color("&8&m----------------------------------------"));
    }

    private String progressBar(double percent) {
        int filled = (int) Math.round(Math.max(0.0, Math.min(100.0, percent)) / 5.0);
        return "█".repeat(filled) + "&8" + "░".repeat(20 - filled);
    }

    private String format(long value) {
        return String.format(Locale.US, "%,d", value);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) return filter(List.of("top"), args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("top")) return filter(List.of("overworld", "nether"), args[1]);
        return List.of();
    }

    private List<String> filter(List<String> values, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String value : values) if (value.startsWith(lower)) result.add(value);
        return result;
    }
}
