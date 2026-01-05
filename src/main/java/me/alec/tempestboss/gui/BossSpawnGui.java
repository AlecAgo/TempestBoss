package me.alec.tempestboss.gui;

import me.alec.tempestboss.BossManager;
import me.alec.tempestboss.TempestBossPlugin;
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

    private final BossManager bossManager;
    private final Component title = Component.text("Spawn Boss").color(NamedTextColor.AQUA);

    private final Map<UUID, String> pendingDifficulty = new HashMap<>();

    public BossSpawnGui(TempestBossPlugin plugin, BossManager bossManager) {
        this.bossManager = bossManager;
    }

    public void open(Player p) {
        Inventory inv = Bukkit.createInventory(p, 27, title);

        inv.setItem(11, button(Material.TRIDENT, "Spawn Tempest Herald", "Spawns the storm boss here."));
        inv.setItem(13, button(Material.NETHER_STAR, "Difficulty", "Click to cycle: NORMAL / HARD / MYTHIC"));
        inv.setItem(15, button(Material.MUSIC_DISC_13, "Toggle Music", "Enable/disable boss music for you."));
        inv.setItem(26, button(Material.BARRIER, "Close", "Close this menu."));

        pendingDifficulty.putIfAbsent(p.getUniqueId(), "NORMAL");
        updateDifficultyItem(inv, p.getUniqueId());

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

    private void updateDifficultyItem(Inventory inv, UUID uuid) {
        String diff = pendingDifficulty.getOrDefault(uuid, "NORMAL");
        ItemStack it = inv.getItem(13);
        if (it == null) return;
        ItemMeta meta = it.getItemMeta();
        meta.lore(java.util.List.of(
                Component.text("Current: ").color(NamedTextColor.GRAY)
                        .append(Component.text(diff).color(NamedTextColor.AQUA)),
                Component.text("Click to cycle.").color(NamedTextColor.DARK_GRAY)
        ));
        it.setItemMeta(meta);
        inv.setItem(13, it);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!e.getView().title().equals(title)) return;

        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        switch (clicked.getType()) {
            case TRIDENT -> {
                String diff = pendingDifficulty.getOrDefault(p.getUniqueId(), "NORMAL");
                bossManager.setDifficulty(diff);
                bossManager.spawnBoss(p.getLocation(), p);
                p.closeInventory();
            }
            case NETHER_STAR -> {
                String curr = pendingDifficulty.getOrDefault(p.getUniqueId(), "NORMAL");
                String next = switch (curr) {
                    case "NORMAL" -> "HARD";
                    case "HARD" -> "MYTHIC";
                    default -> "NORMAL";
                };
                pendingDifficulty.put(p.getUniqueId(), next);
                updateDifficultyItem(e.getInventory(), p.getUniqueId());
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
            }
            case MUSIC_DISC_13 -> {
                bossManager.togglePersonalMusic(p);
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
            }
            case BARRIER -> p.closeInventory();
        }
    }
}
