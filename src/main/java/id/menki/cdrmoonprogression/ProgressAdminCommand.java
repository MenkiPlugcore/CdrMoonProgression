package id.menki.cdrmoonprogression;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class ProgressAdminCommand implements TabExecutor {
    private final CdrMoonProgressionPlugin plugin;
    private final ProgressionService service;
    private final ProgressionDataStore data;
    private final PlacedBlockStore placedBlocks;

    public ProgressAdminCommand(CdrMoonProgressionPlugin plugin, ProgressionService service, ProgressionDataStore data, PlacedBlockStore placedBlocks) {
        this.plugin = plugin;
        this.service = service;
        this.data = data;
        this.placedBlocks = placedBlocks;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("cdrmoonprogression.admin")) {
            sender.sendMessage(plugin.color(plugin.prefix() + "&cKamu tidak punya permission."));
            return true;
        }
        if (args.length == 0) {
            help(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "status" -> status(sender);
            case "reload" -> {
                plugin.reloadConfig();
                service.reloadFromConfig();
                sender.sendMessage(plugin.color(plugin.prefix() + "&aConfig berhasil direload."));
            }
            case "save" -> {
                data.save();
                placedBlocks.saveSync();
                sender.sendMessage(plugin.color(plugin.prefix() + "&aData berhasil disimpan."));
            }
            case "add" -> handleAdd(sender, args);
            case "set" -> handleSet(sender, args);
            case "reset" -> handleReset(sender, args);
            case "unlock" -> handleUnlock(sender, args);
            case "lock" -> handleLock(sender, args);
            default -> help(sender);
        }
        return true;
    }

    private void handleAdd(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.color("&cUsage: /progressadmin add <overworld|nether> <amount> [player]"));
            return;
        }
        ProgressStage stage = ProgressStage.parse(args[1]).orElse(null);
        Long amount = positiveLong(args[2]);
        if (stage == null || amount == null) {
            sender.sendMessage(plugin.color("&cStage/amount invalid."));
            return;
        }

        UUID contributor = null;
        if (args.length >= 4) {
            Player target = Bukkit.getPlayerExact(args[3]);
            if (target == null) {
                sender.sendMessage(plugin.color("&cPlayer harus online agar kontribusi personal dapat dicatat."));
                return;
            }
            contributor = target.getUniqueId();
        }

        long added = service.addProgress(stage, amount, contributor);
        sender.sendMessage(plugin.color(plugin.prefix() + "&aAdded &f" + added + " &apoint ke " + stage.displayName() + "."));
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.color("&cUsage: /progressadmin set <overworld|nether> <amount>"));
            return;
        }
        ProgressStage stage = ProgressStage.parse(args[1]).orElse(null);
        Long amount = nonNegativeLong(args[2]);
        if (stage == null || amount == null) {
            sender.sendMessage(plugin.color("&cStage/amount invalid."));
            return;
        }
        service.setProgress(stage, amount);
        sender.sendMessage(plugin.color(plugin.prefix() + "&aProgress " + stage.displayName() + " di-set ke &f" + data.getTotal(stage) + "&a."));
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.color("&cUsage: /progressadmin reset <overworld|nether|all>"));
            return;
        }
        if (args[1].equalsIgnoreCase("all")) {
            data.resetAll();
            sender.sendMessage(plugin.color(plugin.prefix() + "&eSemua progression direset dan dimension dikunci kembali."));
            return;
        }
        ProgressStage stage = ProgressStage.parse(args[1]).orElse(null);
        if (stage == null) {
            sender.sendMessage(plugin.color("&cStage invalid."));
            return;
        }
        service.reset(stage);
        sender.sendMessage(plugin.color(plugin.prefix() + "&eProgress " + stage.displayName() + " direset."));
    }

    private void handleUnlock(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.color("&cUsage: /progressadmin unlock <nether|end>"));
            return;
        }
        if (args[1].equalsIgnoreCase("nether")) service.unlockNether(true);
        else if (args[1].equalsIgnoreCase("end")) service.unlockEnd(true);
        else {
            sender.sendMessage(plugin.color("&cDimension harus nether atau end."));
            return;
        }
        sender.sendMessage(plugin.color(plugin.prefix() + "&aDimension berhasil di-unlock."));
    }

    private void handleLock(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.color("&cUsage: /progressadmin lock <nether|end>"));
            return;
        }
        if (args[1].equalsIgnoreCase("nether")) service.lockNether();
        else if (args[1].equalsIgnoreCase("end")) service.lockEnd();
        else {
            sender.sendMessage(plugin.color("&cDimension harus nether atau end."));
            return;
        }
        sender.sendMessage(plugin.color(plugin.prefix() + "&eDimension berhasil dikunci."));
    }

    private void status(CommandSender sender) {
        sender.sendMessage(plugin.color("&8&m----------------------------------------"));
        sender.sendMessage(plugin.color("&b&lCdrMoonProgression Admin Status"));
        for (ProgressStage stage : ProgressStage.values()) {
            sender.sendMessage(plugin.color("&f" + stage.displayName() + ": &b" + data.getTotal(stage) + "&7/&f" + service.target(stage)));
        }
        sender.sendMessage(plugin.color("&7Nether unlocked: " + (data.isNetherUnlocked() ? "&aYES" : "&cNO")));
        sender.sendMessage(plugin.color("&7End unlocked: " + (data.isEndUnlocked() ? "&aYES" : "&cNO")));
        sender.sendMessage(plugin.color("&7Tracked placed blocks: &f" + placedBlocks.size()));
        sender.sendMessage(plugin.color("&8&m----------------------------------------"));
    }

    private void help(CommandSender sender) {
        sender.sendMessage(plugin.color("&b/progressadmin status"));
        sender.sendMessage(plugin.color("&b/progressadmin add <stage> <amount> [player]"));
        sender.sendMessage(plugin.color("&b/progressadmin set <stage> <amount>"));
        sender.sendMessage(plugin.color("&b/progressadmin reset <stage|all>"));
        sender.sendMessage(plugin.color("&b/progressadmin unlock <nether|end>"));
        sender.sendMessage(plugin.color("&b/progressadmin lock <nether|end>"));
        sender.sendMessage(plugin.color("&b/progressadmin reload"));
        sender.sendMessage(plugin.color("&b/progressadmin save"));
    }

    private Long positiveLong(String value) {
        Long parsed = nonNegativeLong(value);
        return parsed == null || parsed <= 0 ? null : parsed;
    }

    private Long nonNegativeLong(String value) {
        try {
            long parsed = Long.parseLong(value);
            return parsed < 0 ? null : parsed;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) return filter(List.of("status", "add", "set", "reset", "unlock", "lock", "reload", "save"), args[0]);
        if (args.length == 2 && List.of("add", "set").contains(args[0].toLowerCase(Locale.ROOT))) return filter(List.of("overworld", "nether"), args[1]);
        if (args.length == 2 && args[0].equalsIgnoreCase("reset")) return filter(List.of("overworld", "nether", "all"), args[1]);
        if (args.length == 2 && List.of("unlock", "lock").contains(args[0].toLowerCase(Locale.ROOT))) return filter(List.of("nether", "end"), args[1]);
        if (args.length == 4 && args[0].equalsIgnoreCase("add")) {
            List<String> players = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) players.add(player.getName());
            return filter(players, args[3]);
        }
        return List.of();
    }

    private List<String> filter(List<String> values, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String value : values) if (value.toLowerCase(Locale.ROOT).startsWith(lower)) result.add(value);
        return result;
    }
}
