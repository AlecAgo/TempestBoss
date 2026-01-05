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
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class LootEditorGui implements Listener {

    private final TempestBossPlugin plugin;
    private final LootManager lootManager;

    private final Component title = Component.text("Edit Boss Loot").color(NamedTextColor.GOLD);

    private final Map<UUID, BossType> editing = new HashMap<>();

    public LootEditorGui(TempestBossPlugin plugin, LootManager lootManager) {
        this.plugin = plugin;
        this.lootManager = lootManager;
    }

    public void open(Player p, BossType type) {
        editing.put(p.getUniqueId(), type);

        Inventory inv = Bukkit.createInventory(p, 54, title);

        List<ItemStack> saved = lootManager.getLootRaw(type);
        int slot = 0;
        for (ItemStack it : saved) {
            if (it == null || it.getType().isAir()) continue;
            if (slot >= 45) break;
            inv.setItem(slot++, it);
        }

        inv.setItem(45, button(Material.LIME_CONCRETE, "Save", "Save loot"));
        inv.setItem(46, button(Material.RED_CONCRETE, "Clear", "Clear loot"));
        inv.setItem(49, button(Material.BARRIER, "Back", "Return"));

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

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!e.getView().title().equals(title)) return;

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null) return;

        int slot = e.getRawSlot();

        if (slot < 45) {
            e.setCancelled(false);
            return;
        }

        e.setCancelled(true);

        if (clicked.getType() == Material.LIME_CONCRETE) {
            BossType type = editing.getOrDefault(p.getUniqueId(), BossType.TEMPEST);
            List<ItemStack> items = new ArrayList<>();
            for (int i = 0; i < 45; i++) {
                ItemStack it = e.getInventory().getItem(i);
                if (it == null || it.getType().isAir()) continue;
                items.add(it.clone());
            }
            lootManager.setLoot(type, items);
            lootManager.save();
            p.sendMessage("§aLoot saved for " + type.name());
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.9f, 1.1f);
            return;
        }

        if (clicked.getType() == Material.RED_CONCRETE) {
            for (int i = 0; i < 45; i++) e.getInventory().setItem(i, null);
            p.playSound(p.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 0.7f, 1.3f);
            return;
        }

        if (clicked.getType() == Material.BARRIER) {
            p.closeInventory();
            Bukkit.dispatchCommand(p, "bossspawn");
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        if (!e.getView().title().equals(title)) return;
        editing.remove(p.getUniqueId());
    }
}
