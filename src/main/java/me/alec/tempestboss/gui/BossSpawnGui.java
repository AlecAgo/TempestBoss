package me.alec.tempestboss.gui;

import me.alec.tempestboss.BossManager;
import me.alec.tempestboss.TempestBossPlugin;
import me.alec.tempestboss.boss.BossType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BossSpawnGui implements Listener {

    private final TempestBossPlugin plugin;
    private final BossManager bossManager;

    private final Component title = Component.text("Boss Spawn").color(NamedTextColor.AQUA);

    private final Map<UUID, String> pendingDifficulty = new HashMap<>();

    public BossSpawnGui(TempestBossPlugin plugin, BossManager bossManager) {
        this.plugin = plugin;
        this.bossManager = bossManager;
    }

    public void open(Player p) {
        Inventory inv = Bukkit.createInventory(p, 27, title);

        inv.setItem(10, button(Material.NETHER_STAR, "Boss", "Click to toggle boss type"));
        inv.setItem(12, button(Material.ANVIL, "Difficulty", "Click to cycle"));
        inv.setItem(14, button(Material.MUSIC_DISC_13, "Music", "Pick theme"));
        inv.setItem(16, button(Material.CHEST, "Loot", "Edit loot"));

        inv.setItem(22, button(Material.TRIDENT, "Summon", "Summon selected boss here"));
        inv.setItem(26, button(Material.BARRIER, "Back", "Back to BossGUI"));

        pendingDifficulty.putIfAbsent(p.getUniqueId(), "NORMAL");
        update(inv, p);

        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8f, 1.0f);
    }

    private ItemStack button(Material mat, String name, String lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Component.text(name).color(NamedTextColor.YELLOW));
        meta.lore(java.util.List.of(Component.text(lore).color(NamedTextColor.GRAY)));
        it.setItemMeta(meta);
        return it;
    }

    private void update(Inventory inv, Player p) {
        BossType bt = bossManager.getSelectedBossType(p);
        String diff = pendingDifficulty.getOrDefault(p.getUniqueId(), "NORMAL");

        ItemStack bossItem = inv.getItem(10);
        if (bossItem != null) {
            ItemMeta m = bossItem.getItemMeta();
            m.lore(java.util.List.of(
                    Component.text("Selected: ").color(NamedTextColor.GRAY)
                            .append(Component.text(bt.name()).color(NamedTextColor.AQUA)),
                    Component.text("Click to toggle").color(NamedTextColor.DARK_GRAY)
            ));
            bossItem.setItemMeta(m);
            inv.setItem(10, bossItem);
        }

        ItemStack diffItem = inv.getItem(12);
        if (diffItem != null) {
            ItemMeta m = diffItem.getItemMeta();
            m.lore(java.util.List.of(
                    Component.text("Selected: ").color(NamedTextColor.GRAY)
                            .append(Component.text(diff).color(NamedTextColor.AQUA)),
                    Component.text("Click to cycle").color(NamedTextColor.DARK_GRAY)
            ));
            diffItem.setItemMeta(m);
            inv.setItem(12, diffItem);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!e.getView().title().equals(title)) return;
        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        switch (clicked.getType()) {
            case NETHER_STAR -> {
                BossType curr = bossManager.getSelectedBossType(p);
                BossType next = (curr == BossType.TEMPEST) ? BossType.ABYSSAL_LICH : BossType.TEMPEST;
                bossManager.setSelectedBossType(p, next);
                update(e.getInventory(), p);
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
            }
            case ANVIL -> {
                String curr = pendingDifficulty.getOrDefault(p.getUniqueId(), "NORMAL");
                String next = switch (curr) {
                    case "NORMAL" -> "HARD";
                    case "HARD" -> "MYTHIC";
                    default -> "NORMAL";
                };
                pendingDifficulty.put(p.getUniqueId(), next);
                update(e.getInventory(), p);
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
            }
            case MUSIC_DISC_13 -> { p.closeInventory(); Bukkit.dispatchCommand(p, "boss music"); }
            case CHEST -> { p.closeInventory(); Bukkit.dispatchCommand(p, "boss loot"); }
            case TRIDENT -> {
                BossType t = bossManager.getSelectedBossType(p);
                String diff = pendingDifficulty.getOrDefault(p.getUniqueId(), "NORMAL");
                bossManager.spawnBoss(t, p.getLocation(), diff, p);
                p.closeInventory();
            }
            case BARRIER -> { p.closeInventory(); Bukkit.dispatchCommand(p, "bossgui"); }
        }
    }
}
