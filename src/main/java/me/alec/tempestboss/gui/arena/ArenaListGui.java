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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ArenaListGui implements Listener {

    private final TempestBossPlugin plugin;
    private final ArenaManager arenaManager;

    private final Component title = Component.text("Arenas").color(NamedTextColor.GOLD);

    public ArenaListGui(TempestBossPlugin plugin, ArenaManager arenaManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
    }

    public void open(Player p) {
        Inventory inv = Bukkit.createInventory(p, 54, title);

        List<Arena> arenas = new ArrayList<>(arenaManager.all());
        arenas.sort(Comparator.comparing(a -> a.id.toLowerCase()));

        int slot = 0;
        for (Arena a : arenas) {
            if (slot >= 45) break;
            inv.setItem(slot++, arenaItem(a));
        }

        inv.setItem(45, GuiUtil.button(Material.ANVIL, "Create", "Create a new arena"));
        inv.setItem(49, GuiUtil.button(Material.BARRIER, "Back", "Back"));

        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.1f);
    }

    private ItemStack arenaItem(Arena a) {
        Material mat = a.enabled ? Material.LIME_BANNER : Material.RED_BANNER;
        ItemStack it = new ItemStack(mat);
        var meta = it.getItemMeta();
        meta.displayName(Component.text(a.displayName).color(a.enabled ? NamedTextColor.GREEN : NamedTextColor.RED));
        meta.lore(List.of(
                Component.text("ID: " + a.id).color(NamedTextColor.GRAY),
                Component.text("Boss: " + a.bossType.name()).color(NamedTextColor.GRAY),
                Component.text("Trigger: " + a.trigger.name()).color(NamedTextColor.GRAY),
                Component.text("MinPlayers: " + a.minPlayers).color(NamedTextColor.GRAY),
                Component.text("Click to edit").color(NamedTextColor.DARK_GRAY)
        ));
        it.setItemMeta(meta);
        return it;
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

        if (it.getType() == Material.ANVIL) {
            p.closeInventory();
            p.sendMessage("§eUse: /boss arena create <Name>");
            return;
        }

        var meta = it.getItemMeta();
        if (meta != null && meta.lore() != null && !meta.lore().isEmpty()) {
            String idLine = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(meta.lore().get(0));
            String id = idLine.replace("ID: ", "").trim();
            Arena a = arenaManager.get(id);
            if (a != null) {
                plugin.getArenaEditorGui().open(p, a);
            }
        }
    }
}
