package me.alec.tempestboss.gui.arena;

import me.alec.tempestboss.TempestBossPlugin;
import me.alec.tempestboss.arena.Arena;
import me.alec.tempestboss.arena.ArenaManager;
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

public class ArenaProtectionGui implements Listener {

    private final TempestBossPlugin plugin;
    private final ArenaManager arenaManager;
    private Arena editing;

    private final Component title = Component.text("Arena Protection").color(NamedTextColor.AQUA);

    public ArenaProtectionGui(TempestBossPlugin plugin, ArenaManager arenaManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
    }

    public void open(Player p, Arena a) {
        this.editing = a;
        Inventory inv = Bukkit.createInventory(p, 27, title);

        inv.setItem(10, GuiUtil.button(Material.IRON_PICKAXE, "Block Break: " + (a.protectionBreak?"DENY":"ALLOW"), "Toggle"));
        inv.setItem(12, GuiUtil.button(Material.BRICKS, "Block Place: " + (a.protectionPlace?"DENY":"ALLOW"), "Toggle"));
        inv.setItem(14, GuiUtil.button(Material.WATER_BUCKET, "Fluid Spread: " + (a.protectionFluid?"DENY":"ALLOW"), "Toggle"));
        inv.setItem(16, GuiUtil.button(Material.TNT, "Explosions: " + (a.protectionExplosions?"DENY":"ALLOW"), "Toggle"));

        inv.setItem(26, GuiUtil.button(Material.BARRIER, "Back", "Back"));

        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.1f);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!e.getView().title().equals(title)) return;
        e.setCancelled(true);

        if (editing == null) return;
        ItemStack it = e.getCurrentItem();
        if (it == null) return;

        switch (it.getType()) {
            case IRON_PICKAXE -> editing.protectionBreak = !editing.protectionBreak;
            case BRICKS -> editing.protectionPlace = !editing.protectionPlace;
            case WATER_BUCKET -> editing.protectionFluid = !editing.protectionFluid;
            case TNT -> editing.protectionExplosions = !editing.protectionExplosions;
            case BARRIER -> {
                p.closeInventory();
                plugin.getArenaEditorGui().open(p, editing);
                return;
            }
        }

        arenaManager.save();
        open(p, editing);
    }
}
