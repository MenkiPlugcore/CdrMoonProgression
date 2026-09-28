package id.menki.cdrmoonprogression;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class ProgressionService {
    private final CdrMoonProgressionPlugin plugin;
    private final ProgressionDataStore data;
    private final EnumMap<ProgressStage, Long> targets = new EnumMap<>(ProgressStage.class);
    private final EnumMap<ProgressStage, Map<Material, Integer>> blockValues = new EnumMap<>(ProgressStage.class);
    private final Set<Material> trackedMaterials = new HashSet<>();

    private List<String> overworldWorlds = List.of();
    private List<String> netherWorlds = List.of();
    private List<String> endWorlds = List.of();
    private boolean countOnlySurvival;
    private boolean trackPlacedBlocks;
    private boolean dimensionLockEnabled;

    public ProgressionService(CdrMoonProgressionPlugin plugin, ProgressionDataStore data) {
        this.plugin = plugin;
        this.data = data;
        reloadFromConfig();
    }

    public void reloadFromConfig() {
        targets.clear();
        blockValues.clear();
        trackedMaterials.clear();

        for (ProgressStage stage : ProgressStage.values()) {
            long target = Math.max(1L, plugin.getConfig().getLong("progression." + stage.key() + ".target", stage == ProgressStage.OVERWORLD ? 50_000L : 70_000L));
            targets.put(stage, target);

            Map<Material, Integer> values = new EnumMap<>(Material.class);
            ConfigurationSection section = plugin.getConfig().getConfigurationSection("progression." + stage.key() + ".blocks");
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    Material material = Material.matchMaterial(key.toUpperCase(Locale.ROOT));
                    int value = section.getInt(key, 0);
                    if (material == null) {
                        plugin.getLogger().warning("Material tidak dikenal di config: " + key);
                    } else if (value > 0) {
                        values.put(material, value);
                        trackedMaterials.add(material);
                    }
                }
            }
            blockValues.put(stage, values);
        }

        overworldWorlds = normalizeWorldList(plugin.getConfig().getStringList("worlds.overworld"));
        netherWorlds = normalizeWorldList(plugin.getConfig().getStringList("worlds.nether"));
        endWorlds = normalizeWorldList(plugin.getConfig().getStringList("worlds.end"));
        countOnlySurvival = plugin.getConfig().getBoolean("anti-exploit.count-only-survival", true);
        trackPlacedBlocks = plugin.getConfig().getBoolean("anti-exploit.track-player-placed-blocks", true);
        dimensionLockEnabled = plugin.getConfig().getBoolean("dimension-lock.enabled", true);
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

    public boolean isTrackedMaterial(Material material) {
        return trackedMaterials.contains(material);
    }

    public int blockValue(ProgressStage stage, Material material) {
        return blockValues.getOrDefault(stage, Map.of()).getOrDefault(material, 0);
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

    public long addProgress(ProgressStage stage, long requested, UUID contributor) {
        if (requested <= 0) return 0L;
        long current = data.getTotal(stage);
        long target = target(stage);
        long next = Math.min(target, current + requested);
        long actual = Math.max(0L, next - current);
        if (actual <= 0) return 0L;

        data.setTotal(stage, next);
        if (contributor != null) data.addContribution(contributor, stage, actual);
        checkUnlock(stage);
        return actual;
    }

    public void setProgress(ProgressStage stage, long value) {
        data.setTotal(stage, Math.min(target(stage), Math.max(0L, value)));
        checkUnlock(stage);
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

    public boolean countOnlySurvival() {
        return countOnlySurvival;
    }

    public boolean trackPlacedBlocks() {
        return trackPlacedBlocks;
    }

    public boolean dimensionLockEnabled() {
        return dimensionLockEnabled;
    }
}
