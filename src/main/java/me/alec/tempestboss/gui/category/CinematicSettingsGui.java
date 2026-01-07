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

public class CinematicSettingsGui implements Listener {

    private final TempestBossPlugin plugin;
    private final Component title = Component.text("Cinematics").color(NamedTextColor.LIGHT_PURPLE);

    public CinematicSettingsGui(TempestBossPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player p) {
        Inventory inv = Bukkit.createInventory(p, 27, title);

        boolean enabled = plugin.getConfig().getBoolean("cinematic.enabled", true);
        int intro = plugin.getConfig().getInt("cinematic.intro-seconds", 8);
        int phase = plugin.getConfig().getInt("cinematic.phase-transition-seconds", 3);

        inv.setItem(10, GuiUtil.button(enabled ? Material.LIME_DYE : Material.GRAY_DYE, "Enabled", "Toggle"));
        inv.setItem(12, GuiUtil.button(Material.CLOCK, "Intro Seconds: " + intro, "Left -1 / Right +1"));
        inv.setItem(14, GuiUtil.button(Material.CLOCK, "Transition Seconds: " + phase, "Left -1 / Right +1"));

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

        if (it.getType() == Material.BARRIER) {
            p.closeInventory();
            Bukkit.dispatchCommand(p, "bossgui");
            return;
        }

        if (it.getType() == Material.LIME_DYE || it.getType() == Material.GRAY_DYE) {
            boolean enabled = plugin.getConfig().getBoolean("cinematic.enabled", true);
            plugin.getConfig().set("cinematic.enabled", !enabled);
            plugin.saveConfig();
            open(p);
            return;
        }

        if (it.getType() == Material.CLOCK) {
            String name = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(it.getItemMeta().displayName());
            if (name.startsWith("Intro Seconds")) {
                int v = plugin.getConfig().getInt("cinematic.intro-seconds", 8);
                v += e.isLeftClick() ? -1 : 1;
                v = Math.max(0, Math.min(30, v));
                plugin.getConfig().set("cinematic.intro-seconds", v);
                plugin.saveConfig();
            } else {
                int v = plugin.getConfig().getInt("cinematic.phase-transition-seconds", 3);
                v += e.isLeftClick() ? -1 : 1;
                v = Math.max(0, Math.min(15, v));
                plugin.getConfig().set("cinematic.phase-transition-seconds", v);
                plugin.saveConfig();
            }
            open(p);
        }
    }
}
