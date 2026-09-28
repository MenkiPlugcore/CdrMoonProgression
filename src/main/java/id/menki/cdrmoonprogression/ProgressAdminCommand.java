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
import java.util.UUID;

public final class ProgressAdminCommand implements TabExecutor {
    private final CdrMoonProgressionPlugin plugin;
    private final ProgressionService service;
    private final ProgressionDataStore data;
    private final PlacedBlockStore placedBlocks;
    private final ContributionHistoryStore history;
    private final PersonalGoalService personalGoals;

    public ProgressAdminCommand(CdrMoonProgressionPlugin plugin, ProgressionService service, ProgressionDataStore data,
                                PlacedBlockStore placedBlocks, ContributionHistoryStore history,
                                PersonalGoalService personalGoals) {
        this.plugin = plugin;
        this.service = service;
        this.data = data;
        this.placedBlocks = placedBlocks;
        this.history = history;
        this.personalGoals = personalGoals;
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
                personalGoals.reloadFromConfig();
                sender.sendMessage(plugin.color(plugin.prefix() + "&aConfig berhasil direload."));
            }
            case "save" -> {
                data.save();
                history.save();
                placedBlocks.saveSync();
                sender.sendMessage(plugin.color(plugin.prefix() + "&aData berhasil disimpan."));
            }
            case "add" -> handleAdd(sender, args);
            case "set" -> handleSet(sender, args);
            case "reset" -> handleReset(sender, args);
            case "unlock" -> handleUnlock(sender, args);
            case "lock" -> handleLock(sender, args);
            case "goals", "goal" -> handleGoals(sender, args);
            default -> help(sender);
        }
        return true;
    }

    private void handleGoals(CommandSender sender, String[] args) {
        if (args.length < 2) {
            goalsHelp(sender);
            return;
        }

        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "pending" -> showPendingGoals(sender, args);
            case "reward" -> setGoalRewardState(sender, args, true);
            case "unreward", "reopen" -> setGoalRewardState(sender, args, false);
            default -> goalsHelp(sender);
        }
    }

    private void showPendingGoals(CommandSender sender, String[] args) {
        ProgressStage stageFilter = null;
        if (args.length >= 3 && !args[2].equalsIgnoreCase("all")) {
            stageFilter = ProgressStage.parse(args[2]).orElse(null);
            if (stageFilter == null) {
                sender.sendMessage(plugin.color("&cStage harus overworld, nether, atau all."));
                return;
            }
        }

        List<PersonalGoalService.PendingGoal> pending = personalGoals.pendingAll(stageFilter);
        sender.sendMessage(plugin.color("&8&m----------------------------------------"));
        sender.sendMessage(plugin.color("&e&lPENDING PERSONAL GOAL REWARDS"));
        if (pending.isEmpty()) {
            sender.sendMessage(plugin.color("&7Tidak ada reward personal yang pending."));
        } else {
            int shown = Math.min(20, pending.size());
            for (int i = 0; i < shown; i++) {
                PersonalGoalService.PendingGoal entry = pending.get(i);
                sender.sendMessage(plugin.color("&e" + playerName(entry.playerId())
                        + " &8• &f" + entry.stage().displayName()
                        + " &8• &b" + format(entry.goal().points()) + " poin"));
            }
            if (pending.size() > shown) {
                sender.sendMessage(plugin.color("&8... dan " + (pending.size() - shown) + " pending lainnya."));
            }
            sender.sendMessage(plugin.color("&7Total pending: &f" + pending.size()));
        }
        sender.sendMessage(plugin.color("&8&m----------------------------------------"));
    }

    private void setGoalRewardState(CommandSender sender, String[] args, boolean rewarded) {
        if (args.length < 5) {
            sender.sendMessage(plugin.color("&cUsage: /progressadmin goals " + (rewarded ? "reward" : "unreward")
                    + " <player> <overworld|nether> <goal>"));
            return;
        }

        UUID playerId = findContributor(args[2]);
        ProgressStage stage = ProgressStage.parse(args[3]).orElse(null);
        Long goal = positiveLong(args[4]);
        if (playerId == null) {
            sender.sendMessage(plugin.color("&cContributor tidak ditemukan di data progression."));
            return;
        }
        if (stage == null || goal == null || !personalGoals.isConfiguredGoal(stage, goal)) {
            sender.sendMessage(plugin.color("&cStage atau personal goal tidak valid/configured."));
            return;
        }

        if (rewarded) {
            long contribution = data.getContribution(playerId, stage);
            if (contribution < goal) {
                sender.sendMessage(plugin.color("&cGoal belum tercapai. Kontribusi player: &f" + format(contribution)
                        + "&c/&f" + format(goal) + "&c."));
                return;
            }
            personalGoals.markRewarded(playerId, stage, goal);
            data.save();
            sender.sendMessage(plugin.color(plugin.prefix() + "&aReward personal ditandai REWARDED: &f"
                    + playerName(playerId) + " &8• &f" + stage.displayName() + " &8• &b" + format(goal) + " poin&a."));
        } else {
            personalGoals.unmarkRewarded(playerId, stage, goal);
            data.save();
            sender.sendMessage(plugin.color(plugin.prefix() + "&eReward personal dibuka kembali: &f"
                    + playerName(playerId) + " &8• &f" + stage.displayName() + " &8• &b" + format(goal) + " poin&e."));
        }
    }

    private UUID findContributor(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null && data.contributorIds().contains(online.getUniqueId())) return online.getUniqueId();

        for (UUID uuid : data.contributorIds()) {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
            if (offline.getName() != null && offline.getName().equalsIgnoreCase(name)) return uuid;
        }
        return null;
    }

    private String playerName(UUID uuid) {
        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        return offline.getName() == null ? uuid.toString().substring(0, 8) : offline.getName();
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
            sender.sendMessage(plugin.color(plugin.prefix() + "&eSemua progression, contribution, milestone, dan personal goal state direset."));
            return;
        }
        ProgressStage stage = ProgressStage.parse(args[1]).orElse(null);
        if (stage == null) {
            sender.sendMessage(plugin.color("&cStage invalid."));
            return;
        }
        service.reset(stage);
        sender.sendMessage(plugin.color(plugin.prefix() + "&eProgress dan personal goal state " + stage.displayName() + " direset."));
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
        sender.sendMessage(plugin.color("&7Pending personal rewards: &f" + personalGoals.pendingAll(null).size()));
        sender.sendMessage(plugin.color("&7History entries: &f" + history.size()));
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
        sender.sendMessage(plugin.color("&b/progressadmin goals pending [stage|all]"));
        sender.sendMessage(plugin.color("&b/progressadmin goals reward <player> <stage> <goal>"));
        sender.sendMessage(plugin.color("&b/progressadmin goals unreward <player> <stage> <goal>"));
        sender.sendMessage(plugin.color("&b/progressadmin reload"));
        sender.sendMessage(plugin.color("&b/progressadmin save"));
    }

    private void goalsHelp(CommandSender sender) {
        sender.sendMessage(plugin.color("&ePersonal Goal Reward Admin"));
        sender.sendMessage(plugin.color("&b/progressadmin goals pending [overworld|nether|all]"));
        sender.sendMessage(plugin.color("&b/progressadmin goals reward <player> <stage> <goal>"));
        sender.sendMessage(plugin.color("&b/progressadmin goals unreward <player> <stage> <goal>"));
        sender.sendMessage(plugin.color("&7Reward item/money diberikan manual oleh admin sebelum menjalankan &freward&7."));
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

    private String format(long value) {
        return String.format(Locale.US, "%,d", value);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) return filter(List.of("status", "add", "set", "reset", "unlock", "lock", "goals", "reload", "save"), args[0]);
        if (args.length == 2 && List.of("add", "set").contains(args[0].toLowerCase(Locale.ROOT))) return filter(List.of("overworld", "nether"), args[1]);
        if (args.length == 2 && args[0].equalsIgnoreCase("reset")) return filter(List.of("overworld", "nether", "all"), args[1]);
        if (args.length == 2 && List.of("unlock", "lock").contains(args[0].toLowerCase(Locale.ROOT))) return filter(List.of("nether", "end"), args[1]);
        if (args.length == 2 && (args[0].equalsIgnoreCase("goals") || args[0].equalsIgnoreCase("goal"))) {
            return filter(List.of("pending", "reward", "unreward"), args[1]);
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("goals") || args[0].equalsIgnoreCase("goal"))
                && args[1].equalsIgnoreCase("pending")) {
            return filter(List.of("all", "overworld", "nether"), args[2]);
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("goals") || args[0].equalsIgnoreCase("goal"))
                && List.of("reward", "unreward", "reopen").contains(args[1].toLowerCase(Locale.ROOT))) {
            List<String> players = new ArrayList<>();
            for (UUID uuid : data.contributorIds()) {
                String name = playerName(uuid);
                if (!name.matches("[0-9a-fA-F]{8}")) players.add(name);
            }
            return filter(players, args[2]);
        }
        if (args.length == 4 && (args[0].equalsIgnoreCase("goals") || args[0].equalsIgnoreCase("goal"))
                && List.of("reward", "unreward", "reopen").contains(args[1].toLowerCase(Locale.ROOT))) {
            return filter(List.of("overworld", "nether"), args[3]);
        }
        if (args.length == 5 && (args[0].equalsIgnoreCase("goals") || args[0].equalsIgnoreCase("goal"))
                && List.of("reward", "unreward", "reopen").contains(args[1].toLowerCase(Locale.ROOT))) {
            ProgressStage stage = ProgressStage.parse(args[3]).orElse(null);
            if (stage == null) return List.of();
            List<String> values = new ArrayList<>();
            for (PersonalGoalService.Goal goal : personalGoals.goals(stage)) values.add(Long.toString(goal.points()));
            return filter(values, args[4]);
        }
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
