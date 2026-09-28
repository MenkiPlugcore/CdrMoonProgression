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

public final class PersonalGoalMenu implements Listener {
    private final CdrMoonProgressionPlugin plugin;
    private final ProgressionDataStore data;
    private final PersonalGoalService goals;
    private final ProgressMenu progressMenu;

    public PersonalGoalMenu(CdrMoonProgressionPlugin plugin, ProgressionDataStore data,
                            PersonalGoalService goals, ProgressMenu progressMenu) {
        this.plugin = plugin;
        this.data = data;
        this.goals = goals;
        this.progressMenu = progressMenu;
    }

    public void open(Player player, ProgressStage stage) {
        Inventory inventory = create(stage, 54, "&8Personal Goals • " + stage.displayName());
        fill(inventory, Material.GRAY_STAINED_GLASS_PANE);

        long contribution = data.getContribution(player.getUniqueId(), stage);
        long next = goals.nextGoalPoints(player.getUniqueId(), stage);

        inventory.setItem(4, item(Material.PLAYER_HEAD, "&b&lPersonal Contribution", List.of(
                "&7Stage: &f" + stage.displayName(),
                "&7Kontribusi kamu: &b" + format(contribution) + " poin",
                next > 0L ? "&7Goal berikutnya: &f" + format(next) + " poin" : "&7Goal berikutnya: &aSemua goal tercapai",
                "",
                "&eReward diberikan manual oleh admin."
        )));

        List<PersonalGoalService.Goal> configured = goals.goals(stage);
        int[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34
        };

        for (int i = 0; i < configured.size() && i < slots.length; i++) {
            PersonalGoalService.Goal goal = configured.get(i);
            inventory.setItem(slots[i], goalItem(player, stage, goal));
        }

        if (!goals.enabled()) {
            inventory.setItem(22, item(Material.BARRIER, "&cPersonal Goals Disabled", List.of(
                    "&7Fitur sedang dinonaktifkan dari config."
            )));
        } else if (configured.isEmpty()) {
            inventory.setItem(22, item(Material.PAPER, "&7Belum ada goal", List.of(
                    "&8Admin belum mengatur personal goal stage ini."
            )));
        }

        inventory.setItem(45, item(Material.ARROW, "&eKembali", List.of("&7Kembali ke kontribusi personal.")));
        inventory.setItem(47, stageButton(ProgressStage.OVERWORLD, stage));
        inventory.setItem(49, item(Material.CHEST, "&6Reward Manual", List.of(
                "&7Saat status menjadi &ePENDING REWARD&7,",
                "&7admin memberikan hadiah secara manual.",
                "",
                "&7Setelah diberikan, admin akan",
                "&7menandainya sebagai &aREWARDED&7."
        )));
        inventory.setItem(51, stageButton(ProgressStage.NETHER, stage));
        inventory.setItem(53, item(Material.BARRIER, "&cTutup", List.of("&7Tutup menu personal goals.")));

        player.openInventory(inventory);
    }

    private ItemStack goalItem(Player player, ProgressStage stage, PersonalGoalService.Goal goal) {
        PersonalGoalService.Status status = goals.status(player.getUniqueId(), stage, goal.points());
        long contribution = data.getContribution(player.getUniqueId(), stage);
        long remaining = Math.max(0L, goal.points() - contribution);

        List<String> lore = new ArrayList<>();
        lore.add("&7Target: &f" + format(goal.points()) + " poin");
        lore.add("&7Progress kamu: &f" + format(contribution) + " poin");
        switch (status) {
            case LOCKED -> {
                lore.add("&7Status: &c&lLOCKED");
                lore.add("&7Sisa: &f" + format(remaining) + " poin");
            }
            case PENDING_REWARD -> {
                lore.add("&7Status: &e&lPENDING REWARD");
                lore.add("&eGoal tercapai. Tunggu reward dari admin.");
            }
            case REWARDED -> {
                lore.add("&7Status: &a&lREWARDED");
                lore.add("&aReward sudah dikonfirmasi admin.");
            }
        }

        if (!goal.lore().isEmpty()) {
            lore.add("");
            lore.addAll(goal.lore());
        }
        lore.add("");
        lore.add("&8Tidak ada auto-reward dari plugin.");
        return item(goal.icon(), goal.name(), lore);
    }

    private ItemStack stageButton(ProgressStage target, ProgressStage current) {
        Material icon = target == ProgressStage.OVERWORLD ? Material.GRASS_BLOCK : Material.NETHERRACK;
        return item(icon, target == current ? "&a&l" + target.displayName() : "&b" + target.displayName(), List.of(
                target == current ? "&7Sedang ditampilkan." : "&eKlik untuk lihat personal goal stage ini."
        ));
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof GoalHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) return;

        int slot = event.getRawSlot();
        if (slot == 45) progressMenu.openPersonal(player);
        else if (slot == 47) open(player, ProgressStage.OVERWORLD);
        else if (slot == 51) open(player, ProgressStage.NETHER);
        else if (slot == 53) player.closeInventory();
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof GoalHolder) event.setCancelled(true);
    }

    private Inventory create(ProgressStage stage, int size, String title) {
        GoalHolder holder = new GoalHolder(stage);
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

    private static final class GoalHolder implements InventoryHolder {
        private final ProgressStage stage;
        private Inventory inventory;

        private GoalHolder(ProgressStage stage) {
            this.stage = stage;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }
}
