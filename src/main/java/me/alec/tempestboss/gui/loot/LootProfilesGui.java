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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LootProfilesGui implements Listener {

    private final TempestBossPlugin plugin;
    private final LootProfileManager profiles;

    private final Component title = Component.text("Loot Profiles").color(NamedTextColor.GOLD);

    public LootProfilesGui(TempestBossPlugin plugin, LootProfileManager profiles) {
        this.plugin = plugin;
        this.profiles = profiles;
    }

    public void open(Player p) {
        Inventory inv = Bukkit.createInventory(p, 54, title);

        List<LootProfile> list = new ArrayList<>(profiles.all());
        list.sort(Comparator.comparing(lp -> lp.name.toLowerCase()));

        int slot = 0;
        for (LootProfile lp : list) {
            if (slot >= 45) break;
            inv.setItem(slot++, profileItem(lp));
        }

        inv.setItem(45, GuiUtil.button(Material.ANVIL, "Create", "Use /boss lootprofile create <name>"));
        inv.setItem(49, GuiUtil.button(Material.BARRIER, "Back", "Back"));

        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.1f);
    }

    private ItemStack profileItem(LootProfile lp) {
        ItemStack it = new ItemStack(Material.BUNDLE);
        var m = it.getItemMeta();
        m.displayName(Component.text(lp.name).color(NamedTextColor.AQUA));
        m.lore(List.of(
                Component.text("Rolls/Player: " + lp.rollsPerPlayer).color(NamedTextColor.GRAY),
                Component.text("Items: " + lp.items.size()).color(NamedTextColor.GRAY),
                Component.text("Click to edit").color(NamedTextColor.DARK_GRAY)
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
            Bukkit.dispatchCommand(p, "boss loot");
            return;
        }

        if (it.getType() == Material.BUNDLE) {
            String name = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(it.getItemMeta().displayName());
            LootProfile lp = profiles.get(name);
            if (lp != null) {
                plugin.getLootProfileEditorGui().open(p, lp);
            }
        }
    }
}
