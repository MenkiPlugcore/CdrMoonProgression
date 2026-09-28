package id.menki.cdrmoonprogression;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class HistoryMenu implements Listener {
    private static final int PAGE_SIZE = 45;

    private final CdrMoonProgressionPlugin plugin;
    private final ContributionHistoryStore history;
    private final ProgressMenu progressMenu;
    private final DateTimeFormatter formatter;

    public HistoryMenu(CdrMoonProgressionPlugin plugin, ContributionHistoryStore history, ProgressMenu progressMenu) {
        this.plugin = plugin;
        this.history = history;
        this.progressMenu = progressMenu;

        ZoneId zone;
        try {
            zone = ZoneId.of(plugin.getConfig().getString("history.timezone", "Asia/Jakarta"));
        } catch (Exception ex) {
            zone = ZoneId.of("Asia/Jakarta");
        }
        this.formatter = DateTimeFormatter.ofPattern("dd MMM yyyy • HH:mm", new Locale("id", "ID")).withZone(zone);
    }

    public void open(Player player) {
        open(player, Filter.ALL, 0);
    }

    public void open(Player player, Filter filter, int requestedPage) {
        List<ContributionHistoryStore.Entry> entries = entries(player, filter);
        int maxPage = Math.max(0, (entries.size() - 1) / PAGE_SIZE);
        int page = Math.max(0, Math.min(maxPage, requestedPage));

        Inventory inventory = create(filter, page, 54, "&8Contribution History");
        fill(inventory, Material.GRAY_STAINED_GLASS_PANE);

        int from = page * PAGE_SIZE;
        int to = Math.min(entries.size(), from + PAGE_SIZE);
        for (int i = from; i < to; i++) {
            ContributionHistoryStore.Entry entry = entries.get(i);
            inventory.setItem(i - from, historyItem(entry));
        }

        if (entries.isEmpty()) {
            inventory.setItem(22, item(Material.PAPER, "&7Belum ada history", List.of(
                    "&8Setoran yang berhasil akan muncul di sini."
            )));
        }

        inventory.setItem(45, item(Material.ARROW, "&eKembali", List.of("&7Kembali ke Moon Progression.")));
        inventory.setItem(46, filterItem(Material.BOOK, "&fSemua", Filter.ALL, filter));
        inventory.setItem(47, filterItem(Material.GRASS_BLOCK, "&aOverworld", Filter.OVERWORLD, filter));
        inventory.setItem(48, filterItem(Material.NETHERRACK, "&cNether", Filter.NETHER, filter));
        inventory.setItem(49, filterItem(Material.PLAYER_HEAD, "&bPunyaku", Filter.MINE, filter));
        inventory.setItem(50, item(Material.CLOCK, "&bHistory Info", List.of(
                "&7Filter: &f" + filter.label,
                "&7Entry: &f" + format(entries.size()),
                "&7Halaman: &f" + (page + 1) + "/" + (maxPage + 1),
                "",
                "&8Log terbaru ditampilkan lebih dulu."
        )));
        if (page > 0) inventory.setItem(51, item(Material.SPECTRAL_ARROW, "&eHalaman Sebelumnya", List.of("&7Klik untuk kembali.")));
        if (page < maxPage) inventory.setItem(52, item(Material.SPECTRAL_ARROW, "&eHalaman Berikutnya", List.of("&7Klik untuk lanjut.")));
        inventory.setItem(53, item(Material.BARRIER, "&cTutup", List.of("&7Tutup history.")));

        player.openInventory(inventory);
    }

    private List<ContributionHistoryStore.Entry> entries(Player player, Filter filter) {
        return switch (filter) {
            case ALL -> history.all();
            case OVERWORLD -> history.forStage(ProgressStage.OVERWORLD);
            case NETHER -> history.forStage(ProgressStage.NETHER);
            case MINE -> history.forPlayer(player.getUniqueId());
        };
    }

    private ItemStack historyItem(ContributionHistoryStore.Entry entry) {
        String stageColor = entry.stage() == ProgressStage.OVERWORLD ? "&a" : "&c";
        return item(entry.material(), "&f" + entry.playerName() + " &8• &b" + pretty(entry.material()), List.of(
                "&7Stage: " + stageColor + entry.stage().displayName(),
                "&7Resource: &f" + format(entry.items()) + "x " + pretty(entry.material()),
                "&7Progress: &b+" + format(entry.points()) + " poin",
                "",
                "&8" + formatter.format(Instant.ofEpochMilli(entry.timestamp()))
        ));
    }

    private ItemStack filterItem(Material icon, String name, Filter target, Filter current) {
        boolean selected = target == current;
        return item(icon, (selected ? "&a&l" : "") + name, List.of(
                selected ? "&aSedang dipilih." : "&eKlik untuk filter."
        ));
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof HistoryHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) return;

        int slot = event.getRawSlot();
        if (slot == 45) progressMenu.openMain(player);
        else if (slot == 46) open(player, Filter.ALL, 0);
        else if (slot == 47) open(player, Filter.OVERWORLD, 0);
        else if (slot == 48) open(player, Filter.NETHER, 0);
        else if (slot == 49) open(player, Filter.MINE, 0);
        else if (slot == 51 && holder.page > 0) open(player, holder.filter, holder.page - 1);
        else if (slot == 52) open(player, holder.filter, holder.page + 1);
        else if (slot == 53) player.closeInventory();
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof HistoryHolder) event.setCancelled(true);
    }

    private Inventory create(Filter filter, int page, int size, String title) {
        HistoryHolder holder = new HistoryHolder(filter, page);
        Inventory inventory = Bukkit.createInventory(holder, size, plugin.color(title));
        holder.inventory = inventory;
        return inventory;
    }

    private void fill(Inventory inventory, Material material) {
        ItemStack filler = item(material, " ", List.of());
        for (int i = 0; i < inventory.getSize(); i++) inventory.setItem(i, filler);
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.color(name));
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) coloredLore.add(plugin.color(line));
            meta.setLore(coloredLore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private String pretty(Material material) {
        String[] words = material.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!result.isEmpty()) result.append(' ');
            if (!word.isEmpty()) result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }

    private String format(long value) {
        return String.format(Locale.US, "%,d", value);
    }

    public enum Filter {
        ALL("Semua"), OVERWORLD("Overworld"), NETHER("Nether"), MINE("Punyaku");
        private final String label;
        Filter(String label) { this.label = label; }
    }

    private static final class HistoryHolder implements InventoryHolder {
        private final Filter filter;
        private final int page;
        private Inventory inventory;

        private HistoryHolder(Filter filter, int page) {
            this.filter = filter;
            this.page = page;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }
}
