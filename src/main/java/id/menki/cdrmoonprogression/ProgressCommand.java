package id.menki.cdrmoonprogression;

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
    private final ProgressMenu menu;

    public ProgressCommand(CdrMoonProgressionPlugin plugin, ProgressionService service, ProgressionDataStore data, ProgressMenu menu) {
        this.plugin = plugin;
        this.service = service;
        this.data = data;
        this.menu = menu;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            showConsoleStatus(sender);
            return true;
        }

        if (args.length == 0) {
            menu.openMain(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("top")) {
            ProgressStage stage = args.length > 1
                    ? ProgressStage.parse(args[1]).orElse(null)
                    : service.activeStage().orElse(ProgressStage.NETHER);
            if (stage == null) {
                menu.openMain(player);
            } else {
                menu.openLeaderboard(player, stage);
            }
            return true;
        }

        ProgressStage stage = ProgressStage.parse(args[0]).orElse(null);
        if (stage != null) {
            menu.openStage(player, stage);
            return true;
        }

        if (args[0].equalsIgnoreCase("me") || args[0].equalsIgnoreCase("contribution")) {
            menu.openPersonal(player);
            return true;
        }

        menu.openMain(player);
        return true;
    }

    private void showConsoleStatus(CommandSender sender) {
        sender.sendMessage(plugin.color("&8&m----------------------------------------"));
        sender.sendMessage(plugin.color("&b&lMOON DIMENSION PROGRESSION"));
        for (ProgressStage stage : ProgressStage.values()) {
            long current = data.getTotal(stage);
            long target = service.target(stage);
            sender.sendMessage(plugin.color("&f" + stage.displayName() + ": &b" + format(current) + "&7/&f" + format(target)
                    + " &7(" + String.format(Locale.US, "%.1f%%", service.percent(stage)) + ")"));
        }
        sender.sendMessage(plugin.color("&7Nether: " + (data.isNetherUnlocked() ? "&aUNLOCKED" : "&cLOCKED")));
        sender.sendMessage(plugin.color("&7End: " + (data.isEndUnlocked() ? "&aUNLOCKED" : "&cLOCKED")));
        sender.sendMessage(plugin.color("&8&m----------------------------------------"));
    }

    private String format(long value) {
        return String.format(Locale.US, "%,d", value);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) return filter(List.of("overworld", "nether", "top", "me"), args[0]);
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
