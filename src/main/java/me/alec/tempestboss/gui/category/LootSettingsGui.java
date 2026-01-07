package me.alec.tempestboss.gui.category;

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

public class LootSettingsGui implements Listener {

    private final TempestBossPlugin plugin;
    private final Component title = Component.text("Loot Settings").color(NamedTextColor.GOLD);

    public LootSettingsGui(TempestBossPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player p) {
        Inventory inv = Bukkit.createInventory(p, 27, title);

        String mode = plugin.getConfig().getString("loot.default-delivery", "CHEST");
        long lock = plugin.getConfig().getLong("loot.chest.lock-seconds", 30);

        inv.setItem(10, GuiUtil.button(Material.CHEST, "Delivery: " + mode, "Cycle"));
        inv.setItem(12, GuiUtil.button(Material.CLOCK, "Chest Lock: " + lock + "s", "Left -5 / Right +5"));
        inv.setItem(14, GuiUtil.button(Material.WRITABLE_BOOK, "Edit Loot Items", "Open loot editor"));
        inv.setItem(16, GuiUtil.button(Material.BUNDLE, "Loot Profiles", "Edit loot profiles"));

        inv.setItem(26, GuiUtil.button(Material.BARRIER, "Back", "Back"));

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
            case BARRIER -> { p.closeInventory(); Bukkit.dispatchCommand(p, "bossgui"); return; }
            case CHEST -> {
                String mode = plugin.getConfig().getString("loot.default-delivery", "CHEST");
                mode = switch (mode) {
                    case "CHEST" -> "DROP";
                    case "DROP" -> "PER_PLAYER";
                    case "PER_PLAYER" -> "NONE";
                    default -> "CHEST";
                };
                plugin.getConfig().set("loot.default-delivery", mode);
                plugin.saveConfig();
                open(p);
            }
            case CLOCK -> {
                long lock = plugin.getConfig().getLong("loot.chest.lock-seconds", 30);
                lock += e.isLeftClick() ? -5 : 5;
                lock = Math.max(0, Math.min(600, lock));
                plugin.getConfig().set("loot.chest.lock-seconds", lock);
                plugin.saveConfig();
                open(p);
            }
            case WRITABLE_BOOK -> { p.closeInventory(); plugin.getLootEditorGui().open(p, plugin.getBossManager().getSelectedBossType(p)); }
            case BUNDLE -> { p.closeInventory(); plugin.getLootProfilesGui().open(p); }
        }
    }
}
