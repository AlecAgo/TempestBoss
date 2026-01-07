package me.alec.tempestboss.gui.category;

import me.alec.tempestboss.BossManager;
import me.alec.tempestboss.TempestBossPlugin;
import me.alec.tempestboss.gui.GuiUtil;
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

public class DashboardGui implements Listener {

    private final TempestBossPlugin plugin;
    private final BossManager bossManager;

    private final Component title = Component.text("Boss Control Panel").color(NamedTextColor.LIGHT_PURPLE);

    public DashboardGui(TempestBossPlugin plugin, BossManager bossManager) {
        this.plugin = plugin;
        this.bossManager = bossManager;
    }

    public void open(Player p) {
        Inventory inv = Bukkit.createInventory(p, 27, title);

        inv.setItem(10, GuiUtil.button(Material.MAP, "Arenas", "Create / edit arenas"));
        inv.setItem(12, GuiUtil.button(Material.DRAGON_EGG, "Live Fights", "View running fights"));
        inv.setItem(14, GuiUtil.button(Material.CHEST, "Loot", "Edit loot items"));
        inv.setItem(16, GuiUtil.button(Material.SHIELD, "Protection", "Arena protection defaults"));
        inv.setItem(18, GuiUtil.button(Material.FIREWORK_ROCKET, "Cinematics", "Intro / transitions"));

        inv.setItem(26, GuiUtil.button(Material.BARRIER, "Close", "Close"));

        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.1f);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!e.getView().title().equals(title)) return;
        e.setCancelled(true);

        ItemStack it = e.getCurrentItem();
        if (it == null) return;

        switch (it.getType()) {
            case MAP -> { p.closeInventory(); Bukkit.dispatchCommand(p, "boss arena gui"); }
            case DRAGON_EGG -> { p.closeInventory(); Bukkit.dispatchCommand(p, "boss fights"); }
            case CHEST -> { p.closeInventory(); Bukkit.dispatchCommand(p, "boss loot"); }
            case SHIELD -> { p.closeInventory(); Bukkit.dispatchCommand(p, "boss protection"); }
            case FIREWORK_ROCKET -> { p.closeInventory(); Bukkit.dispatchCommand(p, "boss cinematic"); }
            case BARRIER -> p.closeInventory();
        }
    }
}
