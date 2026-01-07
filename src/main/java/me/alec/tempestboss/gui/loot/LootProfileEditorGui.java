package me.alec.tempestboss.gui.loot;

import me.alec.tempestboss.TempestBossPlugin;
import me.alec.tempestboss.gui.GuiUtil;
import me.alec.tempestboss.loot.profile.LootProfile;
import me.alec.tempestboss.loot.profile.LootProfileManager;
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

import java.util.ArrayList;
import java.util.List;

public class LootProfileEditorGui implements Listener {

    private final TempestBossPlugin plugin;
    private final LootProfileManager profiles;

    private final Component title = Component.text("Edit Loot Profile").color(NamedTextColor.GOLD);

    private LootProfile editing;

    public LootProfileEditorGui(TempestBossPlugin plugin, LootProfileManager profiles) {
        this.plugin = plugin;
        this.profiles = profiles;
    }

    public void open(Player p, LootProfile lp) {
        this.editing = lp;
        Inventory inv = Bukkit.createInventory(p, 54, title);

        int slot = 0;
        for (ItemStack it : lp.items) {
            if (it == null || it.getType().isAir()) continue;
            if (slot >= 45) break;
            inv.setItem(slot++, it);
        }

        inv.setItem(45, GuiUtil.button(Material.LIME_CONCRETE, "Save", "Save items"));
        inv.setItem(46, GuiUtil.button(Material.RED_CONCRETE, "Clear", "Clear"));
        inv.setItem(47, GuiUtil.button(Material.CLOCK, "Rolls/Player: " + lp.rollsPerPlayer, "Left -1 / Right +1"));
        inv.setItem(49, GuiUtil.button(Material.BARRIER, "Back", "Back"));

        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8f, 1.0f);
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

        if (editing == null) return;

        if (clicked.getType() == Material.LIME_CONCRETE) {
            List<ItemStack> items = new ArrayList<>();
            for (int i=0;i<45;i++) {
                ItemStack it = e.getInventory().getItem(i);
                if (it == null || it.getType().isAir()) continue;
                items.add(it.clone());
            }
            editing.items.clear();
            editing.items.addAll(items);
            profiles.save();
            p.sendMessage("§aSaved profile " + editing.name);
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.9f, 1.1f);
            return;
        }

        if (clicked.getType() == Material.RED_CONCRETE) {
            for (int i=0;i<45;i++) e.getInventory().setItem(i, null);
            p.playSound(p.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 0.7f, 1.3f);
            return;
        }

        if (clicked.getType() == Material.CLOCK) {
            int v = editing.rollsPerPlayer;
            v += e.isLeftClick() ? -1 : 1;
            v = Math.max(0, Math.min(20, v));
            editing.rollsPerPlayer = v;
            profiles.save();
            open(p, editing);
            return;
        }

        if (clicked.getType() == Material.BARRIER) {
            p.closeInventory();
            plugin.getLootProfilesGui().open(p);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!e.getView().title().equals(title)) return;
        editing = null;
    }
}
