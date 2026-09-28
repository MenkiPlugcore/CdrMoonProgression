package id.menki.cdrmoonprogression;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ProgressionService {
    private final CdrMoonProgressionPlugin plugin;
    private final ProgressionDataStore data;
    private final EnumMap<ProgressStage, Long> targets = new EnumMap<>(ProgressStage.class);
    private final EnumMap<ProgressStage, Map<Material, Integer>> depositValues = new EnumMap<>(ProgressStage.class);
    private final EnumMap<ProgressStage, List<Milestone>> milestones = new EnumMap<>(ProgressStage.class);

    private List<String> overworldWorlds = List.of();
    private List<String> netherWorlds = List.of();
    private List<String> endWorlds = List.of();
    private boolean dimensionLockEnabled;

    public ProgressionService(CdrMoonProgressionPlugin plugin, ProgressionDataStore data) {
        this.plugin = plugin;
        this.data = data;
        reloadFromConfig();
    }

    public void reloadFromConfig() {
        targets.clear();
        depositValues.clear();
        milestones.clear();

        for (ProgressStage stage : ProgressStage.values()) {
            long defaultTarget = stage == ProgressStage.OVERWORLD ? 50_000L : 70_000L;
            long target = Math.max(1L, plugin.getConfig().getLong("progression." + stage.key() + ".target", defaultTarget));
            targets.put(stage, target);

            Map<Material, Integer> values = new EnumMap<>(Material.class);
            ConfigurationSection section = plugin.getConfig().getConfigurationSection("progression." + stage.key() + ".deposit-items");
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    Material material = Material.matchMaterial(key.toUpperCase(Locale.ROOT));
                    int value = section.getInt(key, 0);
                    if (material == null) {
                        plugin.getLogger().warning("Material deposit tidak dikenal di config: " + key);
                    } else if (value > 0) {
                        values.put(material, value);
                    }
                }
            }
            depositValues.put(stage, values);
            milestones.put(stage, loadMilestones(stage));
        }

        overworldWorlds = normalizeWorldList(plugin.getConfig().getStringList("worlds.overworld"));
        netherWorlds = normalizeWorldList(plugin.getConfig().getStringList("worlds.nether"));
        endWorlds = normalizeWorldList(plugin.getConfig().getStringList("worlds.end"));
        dimensionLockEnabled = plugin.getConfig().getBoolean("dimension-lock.enabled", true);

        initializeMilestoneState();
    }

    private List<Milestone> loadMilestones(ProgressStage stage) {
        List<Milestone> result = new ArrayList<>();
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("milestones." + stage.key());
        if (root == null) return List.of();

        for (String rawPercent : root.getKeys(false)) {
            int percent;
            try {
                percent = Integer.parseInt(rawPercent);
            } catch (NumberFormatException ex) {
                plugin.getLogger().warning("Milestone invalid untuk " + stage.key() + ": " + rawPercent);
                continue;
            }
            if (percent <= 0 || percent > 100) {
                plugin.getLogger().warning("Milestone harus 1-100: " + stage.key() + "." + rawPercent);
                continue;
            }

            String path = "milestones." + stage.key() + "." + rawPercent;
            String name = plugin.getConfig().getString(path + ".name", "&bMilestone " + percent + "%");
            Material icon = Material.matchMaterial(plugin.getConfig().getString(path + ".icon", "CHEST"));
            if (icon == null || icon.isAir()) icon = Material.CHEST;
            List<String> lore = List.copyOf(plugin.getConfig().getStringList(path + ".lore"));
            List<String> commands = List.copyOf(plugin.getConfig().getStringList(path + ".commands"));
            String broadcast = plugin.getConfig().getString(path + ".broadcast", "");
            String title = plugin.getConfig().getString(path + ".title", "");
            String subtitle = plugin.getConfig().getString(path + ".subtitle", "");
            result.add(new Milestone(percent, name, icon, lore, commands, broadcast, title, subtitle));
        }

        result.sort(Comparator.comparingInt(Milestone::percent));
        return List.copyOf(result);
    }

    private void initializeMilestoneState() {
        if (data.milestonesInitialized()) return;

        boolean rewardExisting = plugin.getConfig().getBoolean("milestones.reward-existing-progress-on-first-load", false);
        for (ProgressStage stage : ProgressStage.values()) {
            if (rewardExisting) {
                processMilestones(stage);
            } else {
                double currentPercent = percent(stage);
                for (Milestone milestone : milestones(stage)) {
                    if (currentPercent + 0.0001D >= milestone.percent()) {
                        data.markMilestoneClaimed(stage, milestone.percent());
                    }
                }
            }
        }
        data.setMilestonesInitialized(true);
        data.save();
    }

    private List<String> normalizeWorldList(List<String> input) {
        List<String> result = new ArrayList<>();
        for (String world : input) result.add(world.toLowerCase(Locale.ROOT));
        return List.copyOf(result);
    }

    public Optional<ProgressStage> stageForWorld(World world) {
        if (matchesWorld(world, overworldWorlds, World.Environment.NORMAL)) return Optional.of(ProgressStage.OVERWORLD);
        if (matchesWorld(world, netherWorlds, World.Environment.NETHER)) return Optional.of(ProgressStage.NETHER);
        return Optional.empty();
    }

    public boolean isNetherWorld(World world) {
        return matchesWorld(world, netherWorlds, World.Environment.NETHER);
    }

    public boolean isEndWorld(World world) {
        return matchesWorld(world, endWorlds, World.Environment.THE_END);
    }

    private boolean matchesWorld(World world, List<String> configuredWorlds, World.Environment fallback) {
        if (!configuredWorlds.isEmpty()) {
            return configuredWorlds.contains(world.getName().toLowerCase(Locale.ROOT));
        }
        return world.getEnvironment() == fallback;
    }

    public Map<Material, Integer> depositValues(ProgressStage stage) {
        return Map.copyOf(depositValues.getOrDefault(stage, Map.of()));
    }

    public int depositValue(ProgressStage stage, Material material) {
        return depositValues.getOrDefault(stage, Map.of()).getOrDefault(material, 0);
    }

    public int countDepositable(Player player, Material material) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (isDepositableStack(stack, material)) total += stack.getAmount();
        }
        return total;
    }

    public DepositResult deposit(Player player, ProgressStage stage, Material material, int requestedAmount) {
        if (player == null || stage == null || material == null) return DepositResult.failed("invalid");
        if (!isStageActive(stage)) return DepositResult.failed("inactive-stage");

        int pointValue = depositValue(stage, material);
        if (pointValue <= 0) return DepositResult.failed("invalid-resource");

        int available = countDepositable(player, material);
        if (available <= 0) return DepositResult.failed("no-items");

        int wanted = requestedAmount <= 0 || requestedAmount == Integer.MAX_VALUE
                ? available
                : Math.min(requestedAmount, available);

        long remainingPoints = Math.max(0L, target(stage) - data.getTotal(stage));
        if (remainingPoints <= 0L) return DepositResult.failed("completed");

        long usefulItemsLong = (remainingPoints + pointValue - 1L) / pointValue;
        int usefulItems = (int) Math.min(Integer.MAX_VALUE, usefulItemsLong);
        int toConsume = Math.min(wanted, usefulItems);
        if (toConsume <= 0) return DepositResult.failed("completed");

        int removed = removeDepositable(player.getInventory(), material, toConsume);
        if (removed <= 0) return DepositResult.failed("no-items");

        long requestedPoints = (long) removed * pointValue;
        long actualPoints = addProgress(stage, requestedPoints, player.getUniqueId());
        return new DepositResult(true, removed, actualPoints, pointValue, countDepositable(player, material), null);
    }

    private int removeDepositable(PlayerInventory inventory, Material material, int amount) {
        ItemStack[] contents = inventory.getStorageContents();
        int remaining = amount;
        int removed = 0;

        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (!isDepositableStack(stack, material)) continue;

            int take = Math.min(remaining, stack.getAmount());
            stack.setAmount(stack.getAmount() - take);
            if (stack.getAmount() <= 0) contents[i] = null;
            removed += take;
            remaining -= take;
        }

        inventory.setStorageContents(contents);
        return removed;
    }

    private boolean isDepositableStack(ItemStack stack, Material material) {
        if (stack == null || stack.getType() != material || stack.getAmount() <= 0) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return true;
        if (meta.hasDisplayName() || meta.hasLore() || meta.hasCustomModelData()) return false;
        return meta.getPersistentDataContainer().isEmpty();
    }

    public long target(ProgressStage stage) {
        return targets.getOrDefault(stage, 1L);
    }

    public double percent(ProgressStage stage) {
        return Math.min(100.0D, (data.getTotal(stage) * 100.0D) / target(stage));
    }

    public Optional<ProgressStage> activeStage() {
        if (!data.isNetherUnlocked()) return Optional.of(ProgressStage.OVERWORLD);
        if (!data.isEndUnlocked()) return Optional.of(ProgressStage.NETHER);
        return Optional.empty();
    }

    public boolean isStageActive(ProgressStage stage) {
        return activeStage().map(active -> active == stage).orElse(false);
    }

    public List<Milestone> milestones(ProgressStage stage) {
        return milestones.getOrDefault(stage, List.of());
    }

    public Optional<Milestone> nextMilestone(ProgressStage stage) {
        for (Milestone milestone : milestones(stage)) {
            if (!data.isMilestoneClaimed(stage, milestone.percent())) return Optional.of(milestone);
        }
        return Optional.empty();
    }

    public long addProgress(ProgressStage stage, long requested, UUID contributor) {
        if (requested <= 0) return 0L;
        long current = data.getTotal(stage);
        long target = target(stage);
        long next = Math.min(target, current + requested);
        long actual = Math.max(0L, next - current);
        if (actual <= 0) return 0L;

        data.setTotal(stage, next);
        if (contributor != null) data.addContribution(contributor, stage, actual);
        processMilestones(stage);
        checkUnlock(stage);
        return actual;
    }

    public void setProgress(ProgressStage stage, long value) {
        data.setTotal(stage, Math.min(target(stage), Math.max(0L, value)));
        processMilestones(stage);
        checkUnlock(stage);
    }

    private void processMilestones(ProgressStage stage) {
        double currentPercent = percent(stage);
        for (Milestone milestone : milestones(stage)) {
            if (currentPercent + 0.0001D < milestone.percent()) continue;
            if (data.isMilestoneClaimed(stage, milestone.percent())) continue;

            data.markMilestoneClaimed(stage, milestone.percent());
            data.save();
            executeMilestone(stage, milestone);
        }
    }

    private void executeMilestone(ProgressStage stage, Milestone milestone) {
        String broadcast = placeholders(milestone.broadcast(), stage, milestone);
        if (!broadcast.isBlank()) Bukkit.broadcastMessage(plugin.color(broadcast));

        String title = plugin.color(placeholders(milestone.title(), stage, milestone));
        String subtitle = plugin.color(placeholders(milestone.subtitle(), stage, milestone));
        if (!title.isBlank() || !subtitle.isBlank()) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.sendTitle(title, subtitle, 10, 60, 15);
            }
        }

        for (String rawCommand : milestone.commands()) {
            String command = placeholders(rawCommand, stage, milestone).replaceFirst("^/", "");
            if (command.isBlank()) continue;
            boolean accepted = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            if (!accepted) {
                plugin.getLogger().warning("Milestone command tidak dikenali: " + command);
            }
        }
    }

    private String placeholders(String input, ProgressStage stage, Milestone milestone) {
        if (input == null) return "";
        return input
                .replace("{stage}", stage.displayName())
                .replace("{stage_key}", stage.key())
                .replace("{percent}", Integer.toString(milestone.percent()))
                .replace("{current}", Long.toString(data.getTotal(stage)))
                .replace("{target}", Long.toString(target(stage)));
    }

    private void checkUnlock(ProgressStage stage) {
        if (data.getTotal(stage) < target(stage)) return;

        if (stage == ProgressStage.OVERWORLD && !data.isNetherUnlocked()) {
            data.setNetherUnlocked(true);
            announceUnlock("THE NETHER", stage);
        } else if (stage == ProgressStage.NETHER && !data.isEndUnlocked()) {
            data.setEndUnlocked(true);
            announceUnlock("THE END", stage);
        }
    }

    public void unlockNether(boolean announce) {
        if (data.isNetherUnlocked()) return;
        data.setNetherUnlocked(true);
        if (announce) announceUnlock("THE NETHER", ProgressStage.OVERWORLD);
    }

    public void unlockEnd(boolean announce) {
        if (!data.isNetherUnlocked()) data.setNetherUnlocked(true);
        if (data.isEndUnlocked()) return;
        data.setEndUnlocked(true);
        if (announce) announceUnlock("THE END", ProgressStage.NETHER);
    }

    public void lockNether() {
        data.setNetherUnlocked(false);
    }

    public void lockEnd() {
        data.setEndUnlocked(false);
    }

    public void reset(ProgressStage stage) {
        data.resetStage(stage);
        if (stage == ProgressStage.OVERWORLD) data.setNetherUnlocked(false);
        if (stage == ProgressStage.NETHER) data.setEndUnlocked(false);
    }

    private void announceUnlock(String dimension, ProgressStage completedStage) {
        String broadcast = plugin.message("messages.unlock-broadcast", "&b&lDIMENSION UNLOCKED! &f{dimension}")
                .replace("{dimension}", dimension);
        Bukkit.broadcastMessage(plugin.color(broadcast));

        String title = plugin.color(plugin.message("messages.unlock-title", "&b&lDIMENSION UNLOCKED"));
        String subtitle = plugin.color(plugin.message("messages.unlock-subtitle", "&f{dimension} &7telah terbuka!").replace("{dimension}", dimension));
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(title, subtitle, 10, 80, 20);
        }

        for (String command : plugin.getConfig().getStringList("progression." + completedStage.key() + ".unlock-commands")) {
            if (!command.isBlank()) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replaceFirst("^/", ""));
        }
        data.save();
    }

    public boolean dimensionLockEnabled() {
        return dimensionLockEnabled;
    }

    public record Milestone(int percent, String name, Material icon, List<String> lore, List<String> commands,
                            String broadcast, String title, String subtitle) {}

    public record DepositResult(boolean success, int itemsConsumed, long pointsAdded, int pointValue,
                                int remainingItems, String reason) {
        public static DepositResult failed(String reason) {
            return new DepositResult(false, 0, 0L, 0, 0, reason);
        }
    }
}
