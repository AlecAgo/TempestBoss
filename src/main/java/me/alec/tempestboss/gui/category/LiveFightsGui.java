package me.alec.tempestboss.gui.category;

import me.alec.tempestboss.BossManager;
import me.alec.tempestboss.TempestBossPlugin;
import me.alec.tempestboss.fight.Fight;
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
import java.util.List;

public class LiveFightsGui implements Listener {

    private final TempestBossPlugin plugin;
    private final BossManager bossManager;

    private final Component title = Component.text("Live Fights").color(NamedTextColor.RED);

    public LiveFightsGui(TempestBossPlugin plugin, BossManager bossManager) {
        this.plugin = plugin;
        this.bossManager = bossManager;
    }

    public void open(Player p) {
        Inventory inv = Bukkit.createInventory(p, 54, title);

        List<Fight> fights = new ArrayList<>(bossManager.getActiveFights());
        int slot = 0;
        for (Fight f : fights) {
            if (slot >= 45) break;
            inv.setItem(slot++, fightItem(f));
        }

        inv.setItem(49, GuiUtil.button(Material.BARRIER, "Back", "Back"));
        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.1f);
    }

    private ItemStack fightItem(Fight f) {
        ItemStack it = new ItemStack(Material.DRAGON_EGG);
        var m = it.getItemMeta();
        m.displayName(Component.text(f.arena.displayName + " (" + f.state.name() + ")").color(NamedTextColor.AQUA));
        m.lore(List.of(
                Component.text("Arena ID: " + f.arena.id).color(NamedTextColor.GRAY),
                Component.text("Boss: " + f.boss.type().name()).color(NamedTextColor.GRAY),
                Component.text("Participants: " + f.participants.size()).color(NamedTextColor.GRAY),
                Component.text("Click to force end").color(NamedTextColor.DARK_GRAY)
        ));
        it.setItemMeta(m);
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

        if (it.getType() == Material.DRAGON_EGG) {
            var meta = it.getItemMeta();
            if (meta != null && meta.lore() != null && meta.lore().size() >= 1) {
                String line = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(meta.lore().get(0));
                String id = line.replace("Arena ID: ", "").trim();
                bossManager.endFight(id, true);
                p.sendMessage("§aForce ended fight in " + id);
                open(p);
            }
        }
    }
}
