package me.alec.tempestboss.gui;

import me.alec.tempestboss.BossManager;
import me.alec.tempestboss.TempestBossPlugin;
import me.alec.tempestboss.loot.LootManager;
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

import java.util.List;

public class BossHubGui implements Listener {

    private final TempestBossPlugin plugin;
    private final BossManager bossManager;
    private final LootManager lootManager;

    private final Component title = Component.text("Boss Control Panel").color(NamedTextColor.LIGHT_PURPLE);

    public BossHubGui(TempestBossPlugin plugin, BossManager bossManager, LootManager lootManager) {
        this.plugin = plugin;
        this.bossManager = bossManager;
        this.lootManager = lootManager;
    }

    public void open(Player p) {
        Inventory inv = Bukkit.createInventory(p, 27, title);

        inv.setItem(10, button(Material.TRIDENT, "Spawn Menu", "Choose boss / difficulty and summon"));
        inv.setItem(12, button(Material.MUSIC_DISC_13, "Music", "Pick boss theme disc"));
        inv.setItem(14, button(Material.CHEST, "Loot", "Edit loot items"));

        inv.setItem(16, button(Material.NOTE_BLOCK, "Toggle Personal Music", "Turn boss music ON/OFF for you"));
        inv.setItem(18, button(Material.REPEATER, "Reload", "Reload config + loot.yml"));
        inv.setItem(20, button(Material.REDSTONE_BLOCK, "Kill Boss", "Remove active boss"));

        inv.setItem(26, button(Material.BARRIER, "Close", "Close"));

        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.1f);
    }

    private ItemStack button(Material mat, String name, String lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Component.text(name).color(NamedTextColor.YELLOW));
        meta.lore(List.of(Component.text(lore).color(NamedTextColor.GRAY)));
        it.setItemMeta(meta);
        return it;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!e.getView().title().equals(title)) return;
        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        switch (clicked.getType()) {
            case TRIDENT -> { p.closeInventory(); Bukkit.dispatchCommand(p, "bossspawn"); }
            case MUSIC_DISC_13 -> { p.closeInventory(); Bukkit.dispatchCommand(p, "boss music"); }
            case CHEST -> { p.closeInventory(); Bukkit.dispatchCommand(p, "boss loot"); }
            case NOTE_BLOCK -> {
                bossManager.togglePersonalMusic(p);
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
            }
            case REPEATER -> { p.closeInventory(); Bukkit.dispatchCommand(p, "boss reload"); }
            case REDSTONE_BLOCK -> { p.closeInventory(); Bukkit.dispatchCommand(p, "boss kill"); }
            case BARRIER -> p.closeInventory();
        }
    }
}
