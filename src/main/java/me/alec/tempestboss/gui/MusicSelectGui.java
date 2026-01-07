
package me.alec.tempestboss.gui;

import me.alec.tempestboss.BossManager;
import me.alec.tempestboss.TempestBossPlugin;
import me.alec.tempestboss.boss.BossType;
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

public class MusicSelectGui implements Listener {

    private final TempestBossPlugin plugin;
    private final BossManager bossManager;

    private final Component title = Component.text("Boss Music").color(NamedTextColor.LIGHT_PURPLE);

    public MusicSelectGui(TempestBossPlugin plugin, BossManager bossManager) {
        this.plugin = plugin;
        this.bossManager = bossManager;
    }

    public void open(Player p, BossType selected) {
        Inventory inv = Bukkit.createInventory(p, 27, title);

        // Two bosses in your editor: TEMPEST and ABYSSAL_LICH (as used elsewhere) [1](https://lauedu74602-my.sharepoint.com/personal/alec_agochian_lau_edu/Documents/Microsoft%20Copilot%20Chat%20Files/ArenaEditorGui.java)
        inv.setItem(11, GuiUtil.button(
                Material.LIGHTNING_ROD,
                (selected == BossType.TEMPEST ? "§a" : "§7") + "Tempest",
                "Select Tempest music"
        ));

        inv.setItem(15, GuiUtil.button(
                Material.WITHER_SKELETON_SKULL,
                (selected == BossType.ABYSSAL_LICH ? "§a" : "§7") + "Abyssal Lich",
                "Select Abyssal Lich music"
        ));

        inv.setItem(26, GuiUtil.button(Material.BARRIER, "Back", "Close"));

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
            case LIGHTNING_ROD -> {
                bossManager.setSelectedBossType(p, BossType.TEMPEST);
                open(p, BossType.TEMPEST);
            }
            case WITHER_SKELETON_SKULL -> {
                bossManager.setSelectedBossType(p, BossType.ABYSSAL_LICH);
                open(p, BossType.ABYSSAL_LICH);
            }
            case BARRIER -> p.closeInventory();
        }
    }
}
