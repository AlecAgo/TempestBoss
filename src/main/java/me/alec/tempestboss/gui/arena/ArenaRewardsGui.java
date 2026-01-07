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

public class ArenaRewardsGui implements Listener {

    private final TempestBossPlugin plugin;
    private final ArenaManager arenaManager;
    private Arena editing;

    private final Component title = Component.text("Reward Rules").color(NamedTextColor.GREEN);

    public ArenaRewardsGui(TempestBossPlugin plugin, ArenaManager arenaManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
    }

    public void open(Player p, Arena a) {
        this.editing = a;
        Inventory inv = Bukkit.createInventory(p, 27, title);

        inv.setItem(10, GuiUtil.button(Material.PAPER, "Eligibility: " + a.rewardEligibility, "Cycle"));
        inv.setItem(12, GuiUtil.button(Material.IRON_SWORD, "Min Damage: " + a.rewardMinDamage, "Left -1 / Right +1"));
        inv.setItem(14, GuiUtil.button(Material.CLOCK, "Min Time: " + a.rewardMinTimeSeconds + "s", "Left -5 / Right +5"));

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
            case PAPER -> {
                editing.rewardEligibility = switch (editing.rewardEligibility) {
                    case "ALL_PRESENT" -> "DAMAGE_ONLY";
                    case "DAMAGE_ONLY" -> "TIME_ONLY";
                    case "TIME_ONLY" -> "DAMAGE_OR_TIME";
                    default -> "ALL_PRESENT";
                };
            }
            case IRON_SWORD -> {
                if (e.isLeftClick()) editing.rewardMinDamage = Math.max(0, editing.rewardMinDamage - 1.0);
                if (e.isRightClick()) editing.rewardMinDamage = editing.rewardMinDamage + 1.0;
            }
            case CLOCK -> {
                if (e.isLeftClick()) editing.rewardMinTimeSeconds = Math.max(0, editing.rewardMinTimeSeconds - 5);
                if (e.isRightClick()) editing.rewardMinTimeSeconds = Math.min(3600, editing.rewardMinTimeSeconds + 5);
            }
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
