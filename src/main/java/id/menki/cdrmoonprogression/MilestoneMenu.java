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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MilestoneMenu implements Listener {
    private final CdrMoonProgressionPlugin plugin;
    private final ProgressionService service;
    private final ProgressionDataStore data;
    private final ProgressMenu progressMenu;

    public MilestoneMenu(CdrMoonProgressionPlugin plugin, ProgressionService service,
                         ProgressionDataStore data, ProgressMenu progressMenu) {
        this.plugin = plugin;
        this.service = service;
        this.data = data;
        this.progressMenu = progressMenu;
    }

    public void open(Player player, ProgressStage stage) {
        Inventory inventory = create(stage, 54, "&8Milestone • " + stage.displayName());
        fill(inventory, Material.GRAY_STAINED_GLASS_PANE);

        inventory.setItem(4, item(stage == ProgressStage.OVERWORLD ? Material.GRASS_BLOCK : Material.NETHERRACK,
                "&b&l" + stage.displayName() + " Milestones", List.of(
                        "&7Progress: &f" + format(data.getTotal(stage)) + "&7/&f" + format(service.target(stage)),
                        "&7Persentase: &f" + String.format(Locale.US, "%.1f%%", service.percent(stage)),
                        "",
                        "&7Reward milestone dipicu otomatis",
                        "&7saat progress global mencapainya."
                )));

        List<ProgressionService.Milestone> milestones = service.milestones(stage);
        int[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34
        };

        for (int i = 0; i < milestones.size() && i < slots.length; i++) {
            ProgressionService.Milestone milestone = milestones.get(i);
            inventory.setItem(slots[i], milestoneItem(stage, milestone));
        }

        if (milestones.isEmpty()) {
            inventory.setItem(22, item(Material.PAPER, "&7Belum ada milestone", List.of(
                    "&8Tambahkan milestone di config.yml."
            )));
        } else if (milestones.size() > slots.length) {
            inventory.setItem(40, item(Material.BOOK, "&eMilestone tambahan", List.of(
                    "&7GUI menampilkan " + slots.length + " milestone pertama.",
                    "&7Total configured: &f" + milestones.size()
            )));
        }

        inventory.setItem(45, item(Material.ARROW, "&eKembali", List.of("&7Kembali ke menu progression.")));
        inventory.setItem(47, stageButton(ProgressStage.OVERWORLD, stage));
        inventory.setItem(49, item(Material.NETHER_STAR, "&bReward Otomatis", List.of(
                "&7Tidak perlu claim manual.",
                "&7Command reward hanya dijalankan",
                "&7satu kali per milestone."
        )));
        inventory.setItem(51, stageButton(ProgressStage.NETHER, stage));
        inventory.setItem(53, item(Material.BARRIER, "&cTutup", List.of("&7Tutup menu milestone.")));

        player.openInventory(inventory);
    }

    private ItemStack milestoneItem(ProgressStage stage, ProgressionService.Milestone milestone) {
        boolean claimed = data.isMilestoneClaimed(stage, milestone.percent());
        boolean reached = service.percent(stage) + 0.0001D >= milestone.percent();
        long requiredPoints = Math.max(1L, (long) Math.ceil(service.target(stage) * (milestone.percent() / 100.0D)));
        long remaining = Math.max(0L, requiredPoints - data.getTotal(stage));

        List<String> lore = new ArrayList<>();
        lore.add("&7Target: &f" + milestone.percent() + "%");
        lore.add("&7Butuh progress: &f" + format(requiredPoints) + " poin");
        if (claimed) {
            lore.add("&7Status: &a&lREWARDED");
        } else if (reached) {
            lore.add("&7Status: &eREACHED");
        } else {
            lore.add("&7Status: &cPENDING");
            lore.add("&7Sisa: &f" + format(remaining) + " poin");
        }

        if (!milestone.lore().isEmpty()) {
            lore.add("");
            lore.addAll(milestone.lore());
        }
        lore.add("");
        lore.add("&8Reward otomatis • sekali saja");

        return item(milestone.icon(), milestone.name(), lore);
    }

    private ItemStack stageButton(ProgressStage target, ProgressStage current) {
        Material icon = target == ProgressStage.OVERWORLD ? Material.GRASS_BLOCK : Material.NETHERRACK;
        String prefix = target == current ? "&a&l" : "&b";
        return item(icon, prefix + target.displayName(), List.of(
                target == current ? "&7Sedang ditampilkan." : "&eKlik untuk lihat milestone stage ini."
        ));
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof MilestoneHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) return;

        int slot = event.getRawSlot();
        if (slot == 45) progressMenu.openMain(player);
        else if (slot == 47) open(player, ProgressStage.OVERWORLD);
        else if (slot == 51) open(player, ProgressStage.NETHER);
        else if (slot == 53) player.closeInventory();
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MilestoneHolder) event.setCancelled(true);
    }

    private Inventory create(ProgressStage stage, int size, String title) {
        MilestoneHolder holder = new MilestoneHolder(stage);
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

    private String format(long value) {
        return String.format(Locale.US, "%,d", value);
    }

    private static final class MilestoneHolder implements InventoryHolder {
        private final ProgressStage stage;
        private Inventory inventory;

        private MilestoneHolder(ProgressStage stage) {
            this.stage = stage;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }
}
