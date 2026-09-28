package id.menki.cdrmoonprogression;

import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class CdrMoonProgressionPlugin extends JavaPlugin {
    private ProgressionDataStore dataStore;
    private PlacedBlockStore placedBlockStore;
    private ContributionHistoryStore historyStore;
    private ProgressionService progressionService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();

        dataStore = new ProgressionDataStore(this);
        dataStore.load();

        placedBlockStore = new PlacedBlockStore(this);
        placedBlockStore.load();

        historyStore = new ContributionHistoryStore(this);
        historyStore.load();

        progressionService = new ProgressionService(this, dataStore, historyStore);

        getServer().getPluginManager().registerEvents(new ProgressionListener(progressionService, placedBlockStore), this);
        getServer().getPluginManager().registerEvents(new DimensionLockListener(this, progressionService, dataStore), this);

        ProgressMenu progressMenu = new ProgressMenu(this, progressionService, dataStore);
        getServer().getPluginManager().registerEvents(progressMenu, this);

        MilestoneMenu milestoneMenu = new MilestoneMenu(this, progressionService, dataStore, progressMenu);
        getServer().getPluginManager().registerEvents(milestoneMenu, this);

        HistoryMenu historyMenu = new HistoryMenu(this, historyStore, progressMenu);
        getServer().getPluginManager().registerEvents(historyMenu, this);

        ProgressCommand progressCommand = new ProgressCommand(this, progressionService, dataStore, progressMenu, milestoneMenu, historyMenu);
        PluginCommand progress = getCommand("progress");
        if (progress != null) {
            progress.setExecutor(progressCommand);
            progress.setTabCompleter(progressCommand);
        }

        ProgressAdminCommand adminCommand = new ProgressAdminCommand(this, progressionService, dataStore, placedBlockStore);
        PluginCommand progressAdmin = getCommand("progressadmin");
        if (progressAdmin != null) {
            progressAdmin.setExecutor(adminCommand);
            progressAdmin.setTabCompleter(adminCommand);
        }

        getServer().getScheduler().runTaskTimer(this, () -> {
            dataStore.saveIfDirty();
            historyStore.saveIfDirty();
            placedBlockStore.requestAsyncSave();
        }, 1200L, 1200L);

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new CdrPlaceholderExpansion(this, progressionService, dataStore).register();
            getLogger().info("PlaceholderAPI hook enabled.");
        }

        getLogger().info("CdrMoonProgression v" + getPluginMeta().getVersion() + " enabled.");
        getLogger().info("Progress mode: GUI DEPOSIT ONLY (block break does not add points).");
        getLogger().info("Milestone rewards: enabled (persistent one-time triggers).");
        getLogger().info("Contribution history: " + historyStore.size() + " entries loaded.");
        getLogger().info("Overworld: " + dataStore.getTotal(ProgressStage.OVERWORLD) + "/" + progressionService.target(ProgressStage.OVERWORLD)
                + " | Nether: " + dataStore.getTotal(ProgressStage.NETHER) + "/" + progressionService.target(ProgressStage.NETHER));
    }

    @Override
    public void onDisable() {
        if (dataStore != null) dataStore.save();
        if (historyStore != null) historyStore.save();
        if (placedBlockStore != null) placedBlockStore.saveSync();
    }

    public String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }

    public String message(String path, String fallback) {
        return getConfig().getString(path, fallback);
    }

    public String prefix() {
        return message("messages.prefix", "&8[&bMoon Progress&8]&r ");
    }
}
