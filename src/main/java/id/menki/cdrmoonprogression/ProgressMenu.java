package id.menki.cdrmoonprogression;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
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
    private static final int REQUIREMENT_PAGE_SIZE = 45;

    private final CdrMoonProgressionPlugin plugin;
    private final ProgressionService service;
    private final ProgressionDataStore data;

    public ProgressMenu(CdrMoonProgressionPlugin plugin, ProgressionService service, ProgressionDataStore data) {
        this.plugin = plugin;
        this.service = service;
        this.data = data;
    }

    public void openMain(Player player) {
        Inventory inventory = create(MenuType.MAIN, null, 0, null, 45, "&8Moon Progression");
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

        ProgressStage active = service.activeStage().orElse(null);
        inventory.setItem(31, item(Material.GOLD_INGOT, "&6&lLeaderboard", List.of(
                active == null ? "&7Progression sudah selesai." : "&7Top contributor: &f" + active.displayName(),
                "",
                active == null ? "&8Tidak ada stage aktif." : "&eKlik untuk membuka."
        )));

        inventory.setItem(33, item(active == null ? Material.ENDER_CHEST : Material.HOPPER,
                active == null ? "&a&lKontribusi Selesai" : "&a&lSetor Kontribusi",
                active == null ? List.of(
                        "&7Semua dimension progression",
                        "&7sudah diselesaikan."
                ) : List.of(
                        "&7Pilih resource dari inventory",
                        "&7lalu setor ke expedition.",
                        "",
                        "&7Stage: &f" + active.displayName(),
                        "&eKlik untuk mulai setor."
                )));

        inventory.setItem(36, dimensionStatusItem(ProgressStage.OVERWORLD, data.isNetherUnlocked(), "The Nether"));
        inventory.setItem(44, dimensionStatusItem(ProgressStage.NETHER, data.isEndUnlocked(), "The End"));

        inventory.setItem(40, item(Material.BARRIER, "&cTutup", List.of("&7Tutup menu progression.")));
        player.openInventory(inventory);
    }

    public void openStage(Player player, ProgressStage stage) {
        Inventory inventory = create(MenuType.STAGE, stage, 0, null, 54, "&8Progress • " + stage.displayName());
        fill(inventory, Material.GRAY_STAINED_GLASS_PANE);

        long current = data.getTotal(stage);
        long target = service.target(stage);
        double percent = service.percent(stage);

        inventory.setItem(4, stageItem(stage, player));

        for (int i = 0; i < 9; i++) {
            double threshold = (i + 1) * (100.0 / 9.0);
            boolean reached = percent + 0.0001 >= threshold;
            Material material = reached ? Material.LIME_STAINED_GLASS_PANE : Material.BLACK_STAINED_GLASS_PANE;
            inventory.setItem(18 + i, item(material,
                    reached ? "&aProgress Point" : "&8Belum tercapai",
                    List.of("&f" + String.format(Locale.US, "%.1f", percent) + "%", "&7" + format(current) + " / " + format(target))));
        }

        inventory.setItem(28, requirementSummaryItem(stage));
        inventory.setItem(30, item(Material.PLAYER_HEAD, "&bKontribusi Kamu", List.of(
                "&f" + format(data.getContribution(player.getUniqueId(), stage)) + " poin",
                "",
                "&7Total poin setoran kamu",
                "&7pada stage ini."
        )));
        inventory.setItem(32, item(Material.GOLD_INGOT, "&6Leaderboard", List.of(
                "&7Top contributor: &f" + stage.displayName(),
                "",
                "&eKlik untuk membuka."
        )));

        boolean active = service.isStageActive(stage);
        inventory.setItem(34, item(active ? Material.HOPPER : Material.BARRIER,
                active ? "&a&lSetor Kontribusi" : "&cSetoran Tidak Aktif",
                active ? List.of(
                        "&7Pilih resource dan jumlah",
                        "&7yang ingin disumbangkan.",
                        "",
                        "&eKlik untuk membuka."
                ) : List.of(
                        status(stage),
                        "&7Hanya stage aktif yang",
                        "&7menerima setoran."
                )));

        inventory.setItem(45, item(Material.ARROW, "&eKembali", List.of("&7Kembali ke menu utama.")));
        inventory.setItem(53, item(Material.BARRIER, "&cTutup", List.of("&7Tutup menu progression.")));
        player.openInventory(inventory);
    }

    public void openRequirements(Player player, ProgressStage stage, int requestedPage) {
        List<ProgressionService.ResourceRequirement> requirements = service.requirements(stage);
        int maxPage = Math.max(0, (requirements.size() - 1) / REQUIREMENT_PAGE_SIZE);
        int page = Math.max(0, Math.min(maxPage, requestedPage));

        Inventory inventory = create(MenuType.REQUIREMENTS, stage, page, null, 54, "&8Requirements • " + stage.displayName());
        fill(inventory, Material.GRAY_STAINED_GLASS_PANE);

        int from = page * REQUIREMENT_PAGE_SIZE;
        int to = Math.min(requirements.size(), from + REQUIREMENT_PAGE_SIZE);
        for (int i = from; i < to; i++) {
            ProgressionService.ResourceRequirement requirement = requirements.get(i);
            inventory.setItem(i - from, requirementItem(stage, requirement));
        }

        if (requirements.isEmpty()) {
            inventory.setItem(22, item(Material.PAPER, "&7Tidak ada resource requirement", List.of(
                    service.requirementsEnabled() ? "&8Stage hanya membutuhkan target poin." : "&8Resource Requirements sedang disabled."
            )));
        }

        inventory.setItem(45, item(Material.ARROW, "&eKembali", List.of("&7Kembali ke detail stage.")));
        if (page > 0) inventory.setItem(48, item(Material.SPECTRAL_ARROW, "&eHalaman Sebelumnya", List.of()));
        inventory.setItem(49, item(service.requirementsSatisfied(stage) ? Material.LIME_DYE : Material.CHEST,
                service.requirementsSatisfied(stage) ? "&a&lSEMUA REQUIREMENT SELESAI" : "&e&lRESOURCE REQUIREMENTS",
                List.of(
                        "&7Selesai: &f" + service.completedRequirementCount(stage) + "&7/&f" + service.totalRequirementCount(stage),
                        "&7Target poin: " + (data.getTotal(stage) >= service.target(stage) ? "&aSELESAI" : "&cBELUM"),
                        "",
                        service.stageComplete(stage) ? "&aStage siap membuka dimension." : "&7Poin + seluruh requirement wajib selesai."
                )));
        if (page < maxPage) inventory.setItem(50, item(Material.SPECTRAL_ARROW, "&eHalaman Berikutnya", List.of()));
        inventory.setItem(53, item(Material.BARRIER, "&cTutup", List.of("&7Tutup menu progression.")));
        player.openInventory(inventory);
    }

    public void openDepositResources(Player player, ProgressStage stage, int requestedPage) {
        if (!service.isStageActive(stage)) {
            player.sendActionBar(Component.text("Stage ini tidak menerima setoran.", NamedTextColor.RED));
            openStage(player, stage);
            return;
        }

        List<Map.Entry<Material, Integer>> entries = sortedDepositEntries(stage);
        int maxPage = Math.max(0, (entries.size() - 1) / RESOURCE_PAGE_SIZE);
        int page = Math.max(0, Math.min(maxPage, requestedPage));

        Inventory inventory = create(MenuType.DEPOSIT_RESOURCES, stage, page, null, 54, "&8Setor • " + stage.displayName());
        fill(inventory, Material.GRAY_STAINED_GLASS_PANE);

        int from = page * RESOURCE_PAGE_SIZE;
        int to = Math.min(entries.size(), from + RESOURCE_PAGE_SIZE);
        for (int i = from; i < to; i++) {
            Map.Entry<Material, Integer> entry = entries.get(i);
            Material material = entry.getKey();
            int owned = service.countDepositable(player, material);
            long useful = service.usefulDepositItems(stage, material, owned);
            List<String> lore = new ArrayList<>();
            lore.add("&7Nilai: &b&l+" + entry.getValue() + " poin &7/ item");
            lore.add("&7Kamu punya: &f" + format(owned));

            List<ProgressionService.ResourceRequirement> matching = service.matchingRequirements(stage, material);
            if (!matching.isEmpty()) {
                lore.add("");
                lore.add("&eResource Requirement:");
                for (ProgressionService.ResourceRequirement requirement : matching) {
                    long progress = service.requirementProgress(stage, requirement);
                    lore.add("&7• " + requirement.name() + ": &f" + format(progress) + "&7/&f" + format(requirement.target()));
                }
            }

            lore.add("");
            if (owned <= 0) lore.add("&8Resource tidak tersedia di inventory.");
            else if (useful <= 0L) lore.add("&aResource ini sudah tidak dibutuhkan.");
            else lore.add("&eKlik untuk pilih jumlah setoran.");
            lore.add("&8Item custom/named tidak akan diambil.");

            inventory.setItem(i - from, item(material, "&f" + pretty(material), lore));
        }

        inventory.setItem(45, item(Material.ARROW, "&eKembali", List.of("&7Kembali ke detail stage.")));
        if (page > 0) {
            inventory.setItem(48, item(Material.SPECTRAL_ARROW, "&eHalaman Sebelumnya", List.of("&7Halaman " + page + "/" + (maxPage + 1))));
        }
        inventory.setItem(49, item(stage == ProgressStage.OVERWORLD ? Material.GRASS_BLOCK : Material.NETHERRACK,
                "&b" + stage.displayName(), List.of(
                        "&7Halaman &f" + (page + 1) + "&7/&f" + (maxPage + 1),
                        "&7Resource: &f" + entries.size(),
                        "&7Points: &f" + format(data.getTotal(stage)) + "/" + format(service.target(stage)),
                        "&7Requirements: &f" + service.completedRequirementCount(stage) + "/" + service.totalRequirementCount(stage)
                )));
        if (page < maxPage) {
            inventory.setItem(50, item(Material.SPECTRAL_ARROW, "&eHalaman Berikutnya", List.of("&7Halaman " + (page + 2) + "/" + (maxPage + 1))));
        }
        inventory.setItem(53, item(Material.BARRIER, "&cTutup", List.of("&7Tutup menu progression.")));
        player.openInventory(inventory);
    }

    public void openDepositAmount(Player player, ProgressStage stage, Material material, int returnPage) {
        if (!service.isStageActive(stage)) {
            player.sendActionBar(Component.text("Stage sudah tidak aktif.", NamedTextColor.RED));
            openMain(player);
            return;
        }

        int value = service.depositValue(stage, material);
        if (value <= 0) {
            openDepositResources(player, stage, returnPage);
            return;
        }

        int owned = service.countDepositable(player, material);
        long remainingPoints = Math.max(0L, service.target(stage) - data.getTotal(stage));
        long useful = service.usefulDepositItems(stage, material, owned);

        Inventory inventory = create(MenuType.DEPOSIT_AMOUNT, stage, returnPage, material, 27, "&8Jumlah Setoran");
        fill(inventory, Material.GRAY_STAINED_GLASS_PANE);

        List<String> info = new ArrayList<>();
        info.add("&7Nilai: &f" + value + " poin / item");
        info.add("&7Kamu punya: &f" + format(owned));
        info.add("&7Maks. berguna: &f" + format(useful));
        info.add("&7Sisa target point: &f" + format(remainingPoints));
        for (ProgressionService.ResourceRequirement requirement : service.matchingRequirements(stage, material)) {
            long current = service.requirementProgress(stage, requirement);
            info.add("&7" + requirement.name() + ": &f" + format(current) + "&7/&f" + format(requirement.target()));
        }
        inventory.setItem(4, item(material, "&b&l" + pretty(material), info));

        inventory.setItem(10, depositButton(Material.IRON_NUGGET, "&aSetor 1", 1, owned, useful, value));
        inventory.setItem(12, depositButton(Material.IRON_INGOT, "&aSetor 16", 16, owned, useful, value));
        inventory.setItem(14, depositButton(Material.IRON_BLOCK, "&aSetor 64", 64, owned, useful, value));
        inventory.setItem(16, depositAllButton(owned, useful, value));

        inventory.setItem(22, item(Material.ARROW, "&eKembali", List.of("&7Kembali ke daftar resource.")));
        inventory.setItem(26, item(Material.BARRIER, "&cTutup", List.of("&7Tutup menu progression.")));
        player.openInventory(inventory);
    }

    public void openLeaderboard(Player player, ProgressStage stage) {
        Inventory inventory = create(MenuType.LEADERBOARD, stage, 0, null, 54, "&8Top • " + stage.displayName());
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
        Inventory inventory = create(MenuType.PERSONAL, null, 0, null, 45, "&8Kontribusi Kamu");
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
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) return;

        int slot = event.getRawSlot();
        switch (holder.type) {
            case MAIN -> handleMain(player, slot);
            case STAGE -> handleStage(player, holder.stage, slot);
            case REQUIREMENTS -> handleRequirements(player, holder.stage, holder.page, slot);
            case DEPOSIT_RESOURCES -> handleDepositResources(player, holder.stage, holder.page, slot);
            case DEPOSIT_AMOUNT -> handleDepositAmount(player, holder.stage, holder.material, holder.page, slot);
            case LEADERBOARD -> handleLeaderboard(player, holder.stage, slot);
            case PERSONAL -> handlePersonal(player, slot);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MenuHolder) event.setCancelled(true);
    }

    private void handleMain(Player player, int slot) {
        if (slot == 11) openStage(player, ProgressStage.OVERWORLD);
        else if (slot == 13) service.activeStage().ifPresentOrElse(stage -> openStage(player, stage), () -> openMain(player));
        else if (slot == 15) openStage(player, ProgressStage.NETHER);
        else if (slot == 29) openPersonal(player);
        else if (slot == 31) service.activeStage().ifPresent(stage -> openLeaderboard(player, stage));
        else if (slot == 33) service.activeStage().ifPresent(stage -> openDepositResources(player, stage, 0));
        else if (slot == 40) player.closeInventory();
    }

    private void handleStage(Player player, ProgressStage stage, int slot) {
        if (stage == null) return;
        if (slot == 28) openRequirements(player, stage, 0);
        else if (slot == 32) openLeaderboard(player, stage);
        else if (slot == 34 && service.isStageActive(stage)) openDepositResources(player, stage, 0);
        else if (slot == 45) openMain(player);
        else if (slot == 53) player.closeInventory();
    }

    private void handleRequirements(Player player, ProgressStage stage, int page, int slot) {
        if (stage == null) return;
        if (slot == 45) openStage(player, stage);
        else if (slot == 48 && page > 0) openRequirements(player, stage, page - 1);
        else if (slot == 50) openRequirements(player, stage, page + 1);
        else if (slot == 53) player.closeInventory();
    }

    private void handleDepositResources(Player player, ProgressStage stage, int page, int slot) {
        if (stage == null) return;

        if (slot >= 0 && slot < RESOURCE_PAGE_SIZE) {
            List<Map.Entry<Material, Integer>> entries = sortedDepositEntries(stage);
            int index = page * RESOURCE_PAGE_SIZE + slot;
            if (index >= 0 && index < entries.size()) {
                Material material = entries.get(index).getKey();
                int owned = service.countDepositable(player, material);
                if (owned <= 0) {
                    player.sendActionBar(Component.text("Kamu tidak punya resource vanilla itu di inventory.", NamedTextColor.RED));
                    return;
                }
                if (service.usefulDepositItems(stage, material, owned) <= 0L) {
                    player.sendActionBar(Component.text("Resource ini sudah tidak dibutuhkan untuk stage ini.", NamedTextColor.GREEN));
                    return;
                }
                openDepositAmount(player, stage, material, page);
            }
            return;
        }

        if (slot == 45) openStage(player, stage);
        else if (slot == 48 && page > 0) openDepositResources(player, stage, page - 1);
        else if (slot == 50) openDepositResources(player, stage, page + 1);
        else if (slot == 53) player.closeInventory();
    }

    private void handleDepositAmount(Player player, ProgressStage stage, Material material, int returnPage, int slot) {
        if (stage == null || material == null) return;

        if (slot == 22) {
            openDepositResources(player, stage, returnPage);
            return;
        }
        if (slot == 26) {
            player.closeInventory();
            return;
        }

        int amount = switch (slot) {
            case 10 -> 1;
            case 12 -> 16;
            case 14 -> 64;
            case 16 -> Integer.MAX_VALUE;
            default -> 0;
        };
        if (amount == 0) return;

        ProgressionService.DepositResult result = service.deposit(player, stage, material, amount);
        if (!result.success()) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.7f);
            player.sendActionBar(Component.text(depositFailureMessage(result.reason()), NamedTextColor.RED));
            if (service.isStageActive(stage)) openDepositAmount(player, stage, material, returnPage);
            else openMain(player);
            return;
        }

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.25f);
        player.sendActionBar(Component.text(
                "Setor " + result.itemsConsumed() + "x " + pretty(material) + " • +" + format(result.pointsAdded()) + " kontribusi",
                NamedTextColor.GREEN));

        if (service.isStageActive(stage)) openDepositAmount(player, stage, material, returnPage);
        else openMain(player);
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

    private List<Map.Entry<Material, Integer>> sortedDepositEntries(ProgressStage stage) {
        List<Map.Entry<Material, Integer>> entries = new ArrayList<>(service.depositValues(stage).entrySet());
        entries.sort(Comparator.comparing(entry -> entry.getKey().name()));
        return entries;
    }

    private ItemStack depositButton(Material icon, String name, int amount, int owned, long useful, int value) {
        long actual = Math.min(Math.min((long) amount, owned), useful);
        return item(icon, name, List.of(
                "&7Item tersedia: &f" + format(owned),
                "&7Akan dipakai maks.: &f" + format(actual),
                "&7Nilai kontribusi: &b+" + format(actual * value),
                "",
                actual > 0 ? "&eKlik untuk setor." : "&cResource tidak dibutuhkan."
        ));
    }

    private ItemStack depositAllButton(int owned, long useful, int value) {
        long actual = Math.min(owned, useful);
        return item(Material.HOPPER, "&a&lSetor Semua yang Dibutuhkan", List.of(
                "&7Item tersedia: &f" + format(owned),
                "&7Akan dipakai maks.: &f" + format(actual),
                "&7Nilai kontribusi: &b+" + format(actual * value),
                "",
                actual > 0 ? "&eKlik untuk setor resource yang masih berguna." : "&cResource tidak dibutuhkan."
        ));
    }

    private String depositFailureMessage(String reason) {
        if (reason == null) return "Setoran gagal.";
        return switch (reason) {
            case "inactive-stage", "completed" -> "Stage ini sudah tidak menerima setoran.";
            case "no-items" -> "Resource tidak tersedia di inventory.";
            case "invalid-resource" -> "Resource ini tidak dapat disetor.";
            case "not-needed" -> "Resource ini sudah tidak dibutuhkan untuk stage ini.";
            default -> "Setoran gagal diproses.";
        };
    }

    private ItemStack dimensionStatusItem(ProgressStage stage, boolean unlocked, String dimension) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Points: &f" + format(data.getTotal(stage)) + "&7/&f" + format(service.target(stage)));
        lore.add("&7Requirements: &f" + service.completedRequirementCount(stage) + "&7/&f" + service.totalRequirementCount(stage));
        lore.add("");
        lore.add(unlocked ? "&aSemua syarat telah selesai." : "&7Points dan resource requirement wajib selesai.");
        Material icon = stage == ProgressStage.OVERWORLD
                ? (unlocked ? Material.OBSIDIAN : Material.CRYING_OBSIDIAN)
                : (unlocked ? Material.END_PORTAL_FRAME : Material.ENDER_EYE);
        return item(icon, unlocked ? "&a" + dimension + ": UNLOCKED" : "&c" + dimension + ": LOCKED", lore);
    }

    private ItemStack requirementSummaryItem(ProgressStage stage) {
        int complete = service.completedRequirementCount(stage);
        int total = service.totalRequirementCount(stage);
        boolean done = service.requirementsSatisfied(stage);
        return item(done ? Material.LIME_DYE : Material.CHEST,
                done ? "&a&lResource Requirements Selesai" : "&e&lResource Requirements",
                List.of(
                        "&7Selesai: &f" + complete + "&7/&f" + total,
                        "&7Status: " + (done ? "&aCOMPLETE" : "&cINCOMPLETE"),
                        "",
                        "&eKlik untuk lihat checklist resource."
                ));
    }

    private ItemStack requirementItem(ProgressStage stage, ProgressionService.ResourceRequirement requirement) {
        long current = service.requirementProgress(stage, requirement);
        boolean complete = current >= requirement.target();
        List<String> lore = new ArrayList<>();
        lore.add("&7Progress: &f" + format(current) + "&7/&f" + format(requirement.target()));
        lore.add("&7Persentase: &f" + String.format(Locale.US, "%.1f%%", service.requirementPercent(stage, requirement)));
        lore.add("&7Status: " + (complete ? "&aCOMPLETE" : "&cINCOMPLETE"));
        lore.add("");
        lore.add("&7Material yang diterima:");
        for (Material material : requirement.materials()) {
            lore.add("&8• &f" + pretty(material));
        }
        return item(requirement.icon(), requirement.name(), lore);
    }

    private ItemStack stageItem(ProgressStage stage, Player player) {
        long current = data.getTotal(stage);
        long target = service.target(stage);
        double percent = service.percent(stage);
        Material material = stage == ProgressStage.OVERWORLD ? Material.GRASS_BLOCK : Material.NETHERRACK;
        String destination = stage == ProgressStage.OVERWORLD ? "The Nether" : "The End";

        return item(material, "&b&l" + stage.displayName(), List.of(
                "&7Status: " + status(stage),
                "&7Points: &f" + format(current) + "&7/&f" + format(target),
                "&7Persentase point: &f" + String.format(Locale.US, "%.1f%%", percent),
                "&7Requirements: &f" + service.completedRequirementCount(stage) + "&7/&f" + service.totalRequirementCount(stage),
                "&7Kontribusi kamu: &b" + format(data.getContribution(player.getUniqueId(), stage)),
                "",
                "&7Tujuan unlock: &f" + destination,
                "&eKlik untuk detail."
        ));
    }

    private ItemStack activeStageItem(Player player) {
        return service.activeStage().map(stage -> item(Material.COMPASS, "&e&lExpedition Aktif", List.of(
                "&7Stage: &f" + stage.displayName(),
                "&7Points: &f" + String.format(Locale.US, "%.1f%%", service.percent(stage)),
                "&7Requirements: &f" + service.completedRequirementCount(stage) + "&7/&f" + service.totalRequirementCount(stage),
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
                        "&7Global points: &f" + format(data.getTotal(stage)) + "/" + format(service.target(stage)),
                        "&7Requirements: &f" + service.completedRequirementCount(stage) + "/" + service.totalRequirementCount(stage),
                        "",
                        "&eKlik untuk detail stage."
                ));
    }

    private String status(ProgressStage stage) {
        if (service.isStageActive(stage)) return "&aACTIVE";
        if (stage == ProgressStage.OVERWORLD) return data.isNetherUnlocked() ? "&bCOMPLETED" : "&aACTIVE";
        if (!data.isNetherUnlocked()) return "&cLOCKED";
        return data.isEndUnlocked() ? "&bCOMPLETED" : "&aACTIVE";
    }

    private String rankColor(int index) {
        return switch (index) {
            case 0 -> "&b&l";
            case 1 -> "&f&l";
            case 2 -> "&6&l";
            default -> "&7";
        };
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

    private Inventory create(MenuType type, ProgressStage stage, int page, Material material, int size, String title) {
        MenuHolder holder = new MenuHolder(type, stage, page, material);
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

    private enum MenuType {
        MAIN,
        STAGE,
        REQUIREMENTS,
        DEPOSIT_RESOURCES,
        DEPOSIT_AMOUNT,
        LEADERBOARD,
        PERSONAL
    }

    private static final class MenuHolder implements InventoryHolder {
        private final MenuType type;
        private final ProgressStage stage;
        private final int page;
        private final Material material;
        private Inventory inventory;

        private MenuHolder(MenuType type, ProgressStage stage, int page, Material material) {
            this.type = type;
            this.stage = stage;
            this.page = page;
            this.material = material;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }
}
