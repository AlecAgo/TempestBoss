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
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class MusicSelectGui implements Listener {

    private final TempestBossPlugin plugin;
    private final BossManager bossManager;

    private final Component title = Component.text("Select Boss Music").color(NamedTextColor.LIGHT_PURPLE);

    private final Map<UUID, BossType> editing = new HashMap<>();

    public MusicSelectGui(TempestBossPlugin plugin, BossManager bossManager) {
        this.plugin = plugin;
        this.bossManager = bossManager;
    }

    public void open(Player p, BossType type) {
        editing.put(p.getUniqueId(), type);

        Inventory inv = Bukkit.createInventory(p, 54, title);

        List<Material> discs = new ArrayList<>();
        for (Material m : Material.values()) {
            if (m.name().startsWith("MUSIC_DISC_")) discs.add(m);
        }
        discs.sort(Comparator.comparing(Enum::name));

        int slot = 0;
        for (Material disc : discs) {
            if (slot >= 45) break;
            inv.setItem(slot++, discButton(disc));
        }

        inv.setItem(49, button(Material.BARRIER, "Back", "Return"));

        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.1f);
    }

    private ItemStack discButton(Material disc) {
        ItemStack it = new ItemStack(disc);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Component.text(disc.name().replace("MUSIC_DISC_", "")).color(NamedTextColor.AQUA));
        meta.lore(java.util.List.of(Component.text("Click to set as theme").color(NamedTextColor.GRAY)));
        it.setItemMeta(meta);
        return it;
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
        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (clicked.getType() == Material.BARRIER) {
            p.closeInventory();
            Bukkit.dispatchCommand(p, "bossspawn");
            return;
        }

        if (!clicked.getType().name().startsWith("MUSIC_DISC_")) return;

        BossType type = editing.getOrDefault(p.getUniqueId(), BossType.TEMPEST);
        String path = "bosses." + type.name() + ".music-disc";

        plugin.getConfig().set(path, clicked.getType().name());
        plugin.saveConfig();

        p.sendMessage("§aSet " + type.name() + " music to " + clicked.getType().name());
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.4f);
    }
}
