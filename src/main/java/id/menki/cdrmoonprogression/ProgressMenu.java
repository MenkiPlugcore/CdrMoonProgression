package id.menki.cdrmoonprogression;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ProgressMenu implements Listener {
    private static final int RESOURCE_PAGE_SIZE = 45;

    private final CdrMoonProgressionPlugin plugin;
    private final ProgressionService service;
    private final ProgressionDataStore data;

    public ProgressMenu(CdrMoonProgressionPlugin plugin, ProgressionService service, ProgressionDataStore data) {
        this.plugin = plugin;
        this.service = service;
        this.data = data;
    }

    public void openMain(Player player) {
        Inventory inventory = create(MenuType.MAIN, null, 0, 45, "&8Moon Progression");
        fill(inventory, Material.GRAY_STAINED_GLASS_PANE);

        inventory.setItem(11, stageItem(ProgressStage.OVERWORLD, player));
        inventory.setItem(13, activeStageItem(player));
        inventory.setItem(15, stageItem(ProgressStage.NETHER, player));

        inventory.setItem(29, item(Material.PLAYER_HEAD, "&b&lKontribusi Kamu", List.of(
                "&7Overworld: &f" + format(data.getContribution(player.getUniqueId(), ProgressStage.OVERWORLD)) + " poin",
                "&7Nether: &f" + format(data.getContribution(player.getUniqueId(), ProgressStage.NETHER)) + " poin",
                "",
                "&eKlik untuk detail kontribusi."
        )));

        ProgressStage active = service.activeStage().orElse(ProgressStage.NETHER);
        inventory.setItem(31, item(Material.GOLD_INGOT, "&6&lLeaderboard", List.of(
                "&7Lihat kontributor terbesar",
                "&7untuk stage aktif.",
                "",
                "&eKlik untuk membuka."
        )));
        inventory.setItem(33, item(Material.BOOK, "&a&lCara Berkontribusi", List.of(
                "&7Lihat block natural yang",
                "&7memberikan progression point.",
                "",
                "&7Stage: &f" + active.displayName(),
                "&eKlik untuk membuka."
        )));

        inventory.setItem(36, item(data.isNetherUnlocked() ? Material.OBSIDIAN : Material.CRYING_OBSIDIAN,
                data.isNetherUnlocked() ? "&aThe Nether: UNLOCKED" : "&cThe Nether: LOCKED",
                List.of("&7Target Overworld: &f" + format(service.target(ProgressStage.OVERWORLD)) + " poin")));
        inventory.setItem(44, item(data.isEndUnlocked() ? Material.END_PORTAL_FRAME : Material.ENDER_EYE,
                data.isEndUnlocked() ? "&aThe End: UNLOCKED" : "&cThe End: LOCKED",
                List.of("&7Target Nether: &f" + format(service.target(ProgressStage.NETHER)) + " poin")));

        inventory.setItem(40, item(Material.BARRIER, "&cTutup", List.of("&7Tutup menu progression.")));
        player.openInventory(inventory);
    }

    public void openStage(Player player, ProgressStage stage) {
        Inventory inventory = create(MenuType.STAGE, stage, 0, 54, "&8Progress • " + stage.displayName());
        fill(inventory, Material.GRAY_STAINED_GLASS_PANE);

        long current = data.getTotal(stage);
        long target = service.target(stage);
        double percent = service.percent(stage);

        inventory.setItem(4, stageItem(stage, player));

        for (int i = 0; i < 9; i++) {
            double threshold = (i + 1) * (100.0 / 9.0);
            boolean filled = percent + 0.0001 >= threshold;
            Material material = filled ? Material.LIME_STAINED_GLASS_PANE : Material.BLACK_STAINED_GLASS_PANE;
            inventory.setItem(18 + i, item(material,
                    filled ? "&aProgress" : "&8Belum tercapai",
                    List.of("&f" + String.format(Locale.US, "%.1f", percent) + "%", "&7" + format(current) + " / " + format(target))));
        }

        inventory.setItem(30, item(Material.PLAYER_HEAD, "&bKontribusi Kamu", List.of(
                "&f" + format(data.getContribution(player.getUniqueId(), stage)) + " poin",
                "",
                "&7Point yang sudah kamu bantu",
                "&7kumpulkan pada stage ini."
        )));
        inventory.setItem(32, item(Material.GOLD_INGOT, "&6Leaderboard", List.of(
                "&7Top contributor: &f" + stage.displayName(),
                "",
                "&eKlik untuk membuka."
        )));
        inventory.setItem(34, item(Material.BOOK, "&aBlock Contribution", List.of(
                "&7Daftar resource dan nilai poin",
                "&7yang dihitung pada stage ini.",
                "",
                "&eKlik untuk membuka."
        )));

        inventory.setItem(45, item(Material.ARROW, "&eKembali", List.of("&7Kembali ke menu utama.")));
        inventory.setItem(53, item(Material.BARRIER, "&cTutup", List.of("&7Tutup menu progression.")));
        player.openInventory(inventory);
    }

    public void openResources(Player player, ProgressStage stage, int requestedPage) {
        List<Map.Entry<Material, Integer>> entries = new ArrayList<>(service.blockValues(stage).entrySet());
        entries.sort(Comparator.comparing(entry -> entry.getKey().name()));

        int maxPage = Math.max(0, (entries.size() - 1) / RESOURCE_PAGE_SIZE);
        int page = Math.max(0, Math.min(maxPage, requestedPage));
        Inventory inventory = create(MenuType.RESOURCES, stage, page, 54, "&8Resource • " + stage.displayName());

        int from = page * RESOURCE_PAGE_SIZE;
        int to = Math.min(entries.size(), from + RESOURCE_PAGE_SIZE);
        for (int i = from; i < to; i++) {
            Map.Entry<Material, Integer> entry = entries.get(i);
            inventory.setItem(i - from, item(entry.getKey(), "&f" + pretty(entry.getKey()), List.of(
                    "&7Nilai: &b&l+" + entry.getValue() + " poin",
                    "",
                    "&8Hanya natural block yang dihitung.",
                    "&8Block hasil place tidak memberi poin."
            )));
        }

        for (int slot = 45; slot < 54; slot++) inventory.setItem(slot, item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of()));
        inventory.setItem(45, item(Material.ARROW, "&eKembali", List.of("&7Kembali ke detail stage.")));
        if (page > 0) inventory.setItem(48, item(Material.SPECTRAL_ARROW, "&eHalaman Sebelumnya", List.of("&7Halaman " + page + "/" + (maxPage + 1))));
        inventory.setItem(49, item(stage == ProgressStage.OVERWORLD ? Material.GRASS_BLOCK : Material.NETHERRACK,
                "&b" + stage.displayName(), List.of("&7Halaman &f" + (page + 1) + "&7/&f" + (maxPage + 1), "&7Total resource: &f" + entries.size())));
        if (page < maxPage) inventory.setItem(50, item(Material.SPECTRAL_ARROW, "&eHalaman Berikutnya", List.of("&7Halaman " + (page + 2) + "/" + (maxPage + 1))));
        inventory.setItem(53, item(Material.BARRIER, "&cTutup", List.of("&7Tutup menu progression.")));
        player.openInventory(inventory);
    }

    public void openLeaderboard(Player player, ProgressStage stage) {
        Inventory inventory = create(MenuType.LEADERBOARD, stage, 0, 54, "&8Top • " + stage.displayName());
        fill(inventory, Material.GRAY_STAINED_GLASS_PANE);

        List<ProgressionDataStore.Contribution> top = data.top(stage, 10);
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 20, 22, 24};
        for (int i = 0; i < top.size() && i < slots.length; i++) {
            ProgressionDataStore.Contribution entry = top.get(i);
            OfflinePlayer offline = Bukkit.getOfflinePlayer(entry.playerId());
            String name = offline.getName() == null ? entry.playerId().toString().substring(0, 8) : offline.getName();
            Material icon = i == 0 ? Material.DIAMOND : (i == 1 ? Material.IRON_INGOT : (i == 2 ? Material.GOLD_INGOT : Material.NAME_TAG));
            inventory.setItem(slots[i], item(icon, rankColor(i) + "#" + (i + 1) + " &f" + name, List.of(
                    "&7Kontribusi: &b" + format(entry.value()) + " poin"
            )));
        }

        if (top.isEmpty()) {
            inventory.setItem(22, item(Material.PAPER, "&7Belum ada kontribusi", List.of("&8Leaderboard masih kosong.")));
        }

        inventory.setItem(40, item(Material.PLAYER_HEAD, "&bKontribusi Kamu", List.of(
                "&f" + format(data.getContribution(player.getUniqueId(), stage)) + " poin"
        )));
        inventory.setItem(45, item(Material.ARROW, "&eKembali", List.of("&7Kembali ke detail stage.")));
        inventory.setItem(53, item(Material.BARRIER, "&cTutup", List.of("&7Tutup menu progression.")));
        player.openInventory(inventory);
    }

    public void openPersonal(Player player) {
        Inventory inventory = create(MenuType.PERSONAL, null, 0, 45, "&8Kontribusi Kamu");
        fill(inventory, Material.GRAY_STAINED_GLASS_PANE);

        inventory.setItem(20, personalStageItem(player, ProgressStage.OVERWORLD));
        inventory.setItem(24, personalStageItem(player, ProgressStage.NETHER));
        inventory.setItem(31, item(Material.NETHER_STAR, "&b&lTotal Contribution", List.of(
                "&f" + format(data.getContribution(player.getUniqueId(), ProgressStage.OVERWORLD)
                + data.getContribution(player.getUniqueId(), ProgressStage.NETHER)) + " poin",
                "",
                "&7Akumulasi seluruh stage."
        )));
        inventory.setItem(36, item(Material.ARROW, "&eKembali", List.of("&7Kembali ke menu utama.")));
        inventory.setItem(44, item(Material.BARRIER, "&cTutup", List.of("&7Tutup menu progression.")));
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) return;

        int slot = event.getRawSlot();
        switch (holder.type) {
            case MAIN -> handleMain(player, slot);
            case STAGE -> handleStage(player, holder.stage, slot);
            case RESOURCES -> handleResources(player, holder.stage, holder.page, slot);
            case LEADERBOARD -> handleLeaderboard(player, holder.stage, slot);
            case PERSONAL -> handlePersonal(player, slot);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof MenuHolder) event.setCancelled(true);
    }

    private void handleMain(Player player, int slot) {
        if (slot == 11) openStage(player, ProgressStage.OVERWORLD);
        else if (slot == 13) openStage(player, service.activeStage().orElse(ProgressStage.NETHER));
        else if (slot == 15) openStage(player, ProgressStage.NETHER);
        else if (slot == 29) openPersonal(player);
        else if (slot == 31) openLeaderboard(player, service.activeStage().orElse(ProgressStage.NETHER));
        else if (slot == 33) openResources(player, service.activeStage().orElse(ProgressStage.NETHER), 0);
        else if (slot == 40) player.closeInventory();
    }

    private void handleStage(Player player, ProgressStage stage, int slot) {
        if (stage == null) return;
        if (slot == 32) openLeaderboard(player, stage);
        else if (slot == 34) openResources(player, stage, 0);
        else if (slot == 45) openMain(player);
        else if (slot == 53) player.closeInventory();
    }

    private void handleResources(Player player, ProgressStage stage, int page, int slot) {
        if (stage == null) return;
        if (slot == 45) openStage(player, stage);
        else if (slot == 48 && page > 0) openResources(player, stage, page - 1);
        else if (slot == 50) openResources(player, stage, page + 1);
        else if (slot == 53) player.closeInventory();
    }

    private void handleLeaderboard(Player player, ProgressStage stage, int slot) {
        if (stage == null) return;
        if (slot == 45) openStage(player, stage);
        else if (slot == 53) player.closeInventory();
    }

    private void handlePersonal(Player player, int slot) {
        if (slot == 20) openStage(player, ProgressStage.OVERWORLD);
        else if (slot == 24) openStage(player, ProgressStage.NETHER);
        else if (slot == 36) openMain(player);
        else if (slot == 44) player.closeInventory();
    }

    private ItemStack stageItem(ProgressStage stage, Player player) {
        long current = data.getTotal(stage);
        long target = service.target(stage);
        double percent = service.percent(stage);
        Material material = stage == ProgressStage.OVERWORLD ? Material.GRASS_BLOCK : Material.NETHERRACK;
        String status = status(stage);
        String destination = stage == ProgressStage.OVERWORLD ? "The Nether" : "The End";

        return item(material, "&b&l" + stage.displayName(), List.of(
                "&7Status: " + status,
                "&7Progress: &f" + format(current) + "&7/&f" + format(target),
                "&7Persentase: &f" + String.format(Locale.US, "%.1f%%", percent),
                "&7Kontribusi kamu: &b" + format(data.getContribution(player.getUniqueId(), stage)),
                "",
                "&7Tujuan unlock: &f" + destination,
                "&eKlik untuk detail."
        ));
    }

    private ItemStack activeStageItem(Player player) {
        return service.activeStage().map(stage -> item(Material.COMPASS, "&e&lExpedition Aktif", List.of(
                "&7Stage: &f" + stage.displayName(),
                "&7Progress: &f" + String.format(Locale.US, "%.1f%%", service.percent(stage)),
                "&7Kontribusi kamu: &b" + format(data.getContribution(player.getUniqueId(), stage)),
                "",
                "&eKlik untuk membuka stage."
        ))).orElseGet(() -> item(Material.NETHER_STAR, "&a&lProgression Selesai", List.of(
                "&7The Nether dan The End",
                "&7telah berhasil dibuka."
        )));
    }

    private ItemStack personalStageItem(Player player, ProgressStage stage) {
        return item(stage == ProgressStage.OVERWORLD ? Material.GRASS_BLOCK : Material.NETHERRACK,
                "&b" + stage.displayName(), List.of(
                        "&7Kontribusi: &f" + format(data.getContribution(player.getUniqueId(), stage)) + " poin",
                        "&7Global: &f" + format(data.getTotal(stage)) + "/" + format(service.target(stage)),
                        "",
                        "&eKlik untuk detail stage."
                ));
    }

    private String status(ProgressStage stage) {
        if (stage == ProgressStage.OVERWORLD) {
            if (data.isNetherUnlocked()) return "&aCOMPLETED";
            return "&eACTIVE";
        }
        if (!data.isNetherUnlocked()) return "&cLOCKED";
        if (data.isEndUnlocked()) return "&aCOMPLETED";
        return "&eACTIVE";
    }

    private Inventory create(MenuType type, ProgressStage stage, int page, int size, String title) {
        MenuHolder holder = new MenuHolder(type, stage, page);
        Inventory inventory = Bukkit.createInventory(holder, size, plugin.color(title));
        holder.inventory = inventory;
        return inventory;
    }

    private void fill(Inventory inventory, Material material) {
        ItemStack filler = item(material, " ", List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, filler);
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.color(name));
            if (!lore.isEmpty()) {
                List<String> colored = new ArrayList<>(lore.size());
                for (String line : lore) colored.add(plugin.color(line));
                meta.setLore(colored);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private String format(long value) {
        return String.format(Locale.US, "%,d", value);
    }

    private String pretty(Material material) {
        String[] parts = material.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (!builder.isEmpty()) builder.append(' ');
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }

    private String rankColor(int index) {
        return switch (index) {
            case 0 -> "&b&l";
            case 1 -> "&f&l";
            case 2 -> "&6&l";
            default -> "&7";
        };
    }

    private enum MenuType {
        MAIN,
        STAGE,
        RESOURCES,
        LEADERBOARD,
        PERSONAL
    }

    private static final class MenuHolder implements InventoryHolder {
        private final MenuType type;
        private final ProgressStage stage;
        private final int page;
        private Inventory inventory;

        private MenuHolder(MenuType type, ProgressStage stage, int page) {
            this.type = type;
            this.stage = stage;
            this.page = page;
        }

        @Override
        public @NotNull Inventory getInventory() {
            if (inventory == null) throw new IllegalStateException("Inventory belum dibuat");
            return inventory;
        }
    }
}
