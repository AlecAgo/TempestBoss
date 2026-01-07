
package me.alec.tempestboss.gui;

import me.alec.tempestboss.TempestBossPlugin;
import me.alec.tempestboss.boss.BossType;
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

import java.util.List;

public class LootEditorGui implements Listener {

    private final TempestBossPlugin plugin;
    private final LootManager lootManager;

    private final Component title = Component.text("Loot Editor").color(NamedTextColor.GOLD);

    public LootEditorGui(TempestBossPlugin plugin, LootManager lootManager) {
        this.plugin = plugin;
        this.lootManager = lootManager;
    }

    public void open(Player p, BossType type) {
        Inventory inv = Bukkit.createInventory(p, 54, title);

        // header
        inv.setItem(4, GuiUtil.button(Material.WRITABLE_BOOK, "Viewing loot: " + type.name(), "This screen is currently read-only"));

        // show loot items (first 45 slots)
        List<ItemStack> items = lootManager.getLootRaw(type);
        for (int i = 0; i < Math.min(45, items.size()); i++) {
            ItemStack it = items.get(i);
            if (it == null || it.getType().isAir()) continue;
            inv.setItem(i, it.clone());
        }

        inv.setItem(53, GuiUtil.button(Material.BARRIER, "Back", "Return to Loot Settings"));

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

        if (it.getType() == Material.BARRIER) {
            p.closeInventory();
            plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getBossManager().reloadAll());
            // go back to Loot Settings GUI
            plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getServer().dispatchCommand(p, "boss loot"));
        }
    }
}
