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

public class ProtectionDefaultsGui implements Listener {

    private final TempestBossPlugin plugin;
    private final Component title = Component.text("Protection Defaults").color(NamedTextColor.AQUA);

    public ProtectionDefaultsGui(TempestBossPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player p) {
        Inventory inv = Bukkit.createInventory(p, 27, title);

        boolean enabled = plugin.getConfig().getBoolean("protection.enabled", true);
        boolean br = plugin.getConfig().getBoolean("protection.block-break", false);
        boolean bp = plugin.getConfig().getBoolean("protection.block-place", false);
        boolean fl = plugin.getConfig().getBoolean("protection.fluid-spread", false);
        boolean ex = plugin.getConfig().getBoolean("protection.explosions", false);

        inv.setItem(10, GuiUtil.button(enabled ? Material.LIME_DYE : Material.GRAY_DYE, "Enabled", "Toggle"));
        inv.setItem(12, GuiUtil.button(Material.IRON_PICKAXE, "Block Break: " + (br?"DENY":"ALLOW"), "Toggle"));
        inv.setItem(13, GuiUtil.button(Material.BRICKS, "Block Place: " + (bp?"DENY":"ALLOW"), "Toggle"));
        inv.setItem(14, GuiUtil.button(Material.WATER_BUCKET, "Fluid: " + (fl?"DENY":"ALLOW"), "Toggle"));
        inv.setItem(15, GuiUtil.button(Material.TNT, "Explosions: " + (ex?"DENY":"ALLOW"), "Toggle"));

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
            case LIME_DYE, GRAY_DYE -> {
                boolean enabled = plugin.getConfig().getBoolean("protection.enabled", true);
                plugin.getConfig().set("protection.enabled", !enabled);
            }
            case IRON_PICKAXE -> plugin.getConfig().set("protection.block-break", !plugin.getConfig().getBoolean("protection.block-break", false));
            case BRICKS -> plugin.getConfig().set("protection.block-place", !plugin.getConfig().getBoolean("protection.block-place", false));
            case WATER_BUCKET -> plugin.getConfig().set("protection.fluid-spread", !plugin.getConfig().getBoolean("protection.fluid-spread", false));
            case TNT -> plugin.getConfig().set("protection.explosions", !plugin.getConfig().getBoolean("protection.explosions", false));
        }

        plugin.saveConfig();
        open(p);
    }
}
