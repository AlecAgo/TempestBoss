package me.alec.tempestboss.gui.arena;

import me.alec.tempestboss.TempestBossPlugin;
import me.alec.tempestboss.arena.*;
import me.alec.tempestboss.boss.BossType;
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

public class ArenaEditorGui implements Listener {

    private final TempestBossPlugin plugin;
    private final ArenaManager arenaManager;

    private final Component title = Component.text("Arena Editor").color(NamedTextColor.GOLD);

    private Arena editing;

    public ArenaEditorGui(TempestBossPlugin plugin, ArenaManager arenaManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
    }

    public void open(Player p, Arena a) {
        this.editing = a;

        Inventory inv = Bukkit.createInventory(p, 54, title);

        inv.setItem(10, GuiUtil.button(Material.NAME_TAG, "Arena: " + a.displayName, "ID: " + a.id));
        inv.setItem(12, GuiUtil.button(a.enabled ? Material.LIME_DYE : Material.GRAY_DYE, "Enabled", "Toggle"));
        inv.setItem(14, GuiUtil.button(Material.ENDER_PEARL, "Trigger: " + a.trigger.name(), "Toggle"));

        inv.setItem(19, GuiUtil.button(Material.NETHER_STAR, "Boss: " + a.bossType.name(), "Toggle"));
        inv.setItem(20, GuiUtil.button(Material.ANVIL, "Difficulty: " + a.difficulty, "Cycle"));

        inv.setItem(22, GuiUtil.button(Material.PLAYER_HEAD, "Min Players: " + a.minPlayers, "Left -1 / Right +1"));
        inv.setItem(23, GuiUtil.button(Material.CLOCK, "Countdown: " + a.countdownSeconds + "s", "Left -1 / Right +1"));

        // region
        inv.setItem(28, GuiUtil.button(Material.COMPASS, "Region Type", (a.region instanceof RegionCuboid) ? "Cuboid" : "Sphere"));
        inv.setItem(29, GuiUtil.button(Material.BEACON, "Set Center", "Use your position"));
        inv.setItem(30, GuiUtil.button(Material.SLIME_BALL, "Radius (Sphere)", "Left -5 / Right +5"));
        inv.setItem(31, GuiUtil.button(Material.GOLDEN_AXE, "Set Pos1 (Cuboid)", "Use your position"));
        inv.setItem(32, GuiUtil.button(Material.GOLDEN_SHOVEL, "Set Pos2 (Cuboid)", "Use your position"));
        inv.setItem(33, GuiUtil.button(Material.IRON_BOOTS, "Set Boss Spawn", "Use your position"));

        // intro & loot
        inv.setItem(37, GuiUtil.button(Material.FIREWORK_STAR, "Intro: " + a.intro.name(), "Cycle"));
        inv.setItem(38, GuiUtil.button(Material.CHEST, "Loot: " + a.lootDelivery.name(), "Cycle"));
        inv.setItem(39, GuiUtil.button(Material.BUNDLE, "Loot Profile: " + (a.lootProfile == null || a.lootProfile.isBlank() ? "(none)" : a.lootProfile), "Cycle profiles"));

        // protection
        inv.setItem(40, GuiUtil.button(Material.SHIELD, "Protection", "Edit protection rules"));

        // rewards
        inv.setItem(41, GuiUtil.button(Material.EMERALD, "Rewards", "Eligibility rules"));

        inv.setItem(49, GuiUtil.button(Material.BARRIER, "Back", "Back"));

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
        if (editing == null) return;

        switch (it.getType()) {
            case LIME_DYE, GRAY_DYE -> {
                editing.enabled = !editing.enabled;
                arenaManager.save();
                open(p, editing);
            }
            case ENDER_PEARL -> {
                editing.trigger = (editing.trigger == ArenaEnums.TriggerMode.MANUAL) ? ArenaEnums.TriggerMode.ENTER : ArenaEnums.TriggerMode.MANUAL;
                arenaManager.save();
                open(p, editing);
            }
            case NETHER_STAR -> {
                editing.bossType = (editing.bossType == BossType.TEMPEST) ? BossType.ABYSSAL_LICH : BossType.TEMPEST;
                arenaManager.save();
                open(p, editing);
            }
            case ANVIL -> {
                editing.difficulty = editing.difficulty.equals("NORMAL") ? "HARD" : (editing.difficulty.equals("HARD") ? "MYTHIC" : "NORMAL");
                arenaManager.save();
                open(p, editing);
            }
            case PLAYER_HEAD -> {
                if (e.isLeftClick()) editing.minPlayers = Math.max(1, editing.minPlayers-1);
                if (e.isRightClick()) editing.minPlayers = Math.min(99, editing.minPlayers+1);
                arenaManager.save();
                open(p, editing);
            }
            case CLOCK -> {
                if (e.isLeftClick()) editing.countdownSeconds = Math.max(0, editing.countdownSeconds-1);
                if (e.isRightClick()) editing.countdownSeconds = Math.min(120, editing.countdownSeconds+1);
                arenaManager.save();
                open(p, editing);
            }
            case COMPASS -> {
                // toggle region type
                if (editing.region instanceof RegionCuboid) {
                    var c = editing.region.getCenter();
                    editing.region = new RegionSphere(c, Math.max(10, editing.region.approximateRadius()));
                } else {
                    var c = (editing.region != null) ? editing.region.getCenter() : p.getLocation();
                    editing.region = new RegionCuboid(c.clone().add(-10,-5,-10), c.clone().add(10,5,10));
                }
                arenaManager.save();
                open(p, editing);
            }
            case BEACON -> {
                // set center
                var loc = p.getLocation();
                if (editing.region instanceof RegionSphere s) {
                    editing.region = new RegionSphere(loc, s.radius());
                } else {
                    editing.region = new RegionCuboid(loc.clone().add(-10,-5,-10), loc.clone().add(10,5,10));
                }
                arenaManager.save();
                open(p, editing);
            }
            case SLIME_BALL -> {
                if (editing.region instanceof RegionSphere s) {
                    double r = s.radius() + (e.isLeftClick() ? -5 : 5);
                    r = Math.max(5, Math.min(250, r));
                    editing.region = new RegionSphere(s.getCenter(), r);
                    arenaManager.save();
                    open(p, editing);
                } else {
                    p.sendMessage("§cRadius only applies to sphere.");
                }
            }
            case GOLDEN_AXE -> {
                // set pos1
                if (editing.region instanceof RegionCuboid c) {
                    editing.region = new RegionCuboid(p.getLocation(), c.max());
                } else {
                    editing.region = new RegionCuboid(p.getLocation(), p.getLocation().clone().add(1,1,1));
                }
                arenaManager.save();
                open(p, editing);
            }
            case GOLDEN_SHOVEL -> {
                // set pos2
                if (editing.region instanceof RegionCuboid c) {
                    editing.region = new RegionCuboid(c.min(), p.getLocation());
                } else {
                    editing.region = new RegionCuboid(p.getLocation().clone().add(-1,-1,-1), p.getLocation());
                }
                arenaManager.save();
                open(p, editing);
            }
            case IRON_BOOTS -> {
                editing.bossSpawn = p.getLocation();
                arenaManager.save();
                open(p, editing);
            }
            case FIREWORK_STAR -> {
                editing.intro = switch (editing.intro) {
                    case INHERIT -> ArenaEnums.IntroMode.ON;
                    case ON -> ArenaEnums.IntroMode.OFF;
                    case OFF -> ArenaEnums.IntroMode.INHERIT;
                };
                arenaManager.save();
                open(p, editing);
            }
            case BUNDLE -> {
                // cycle profiles: (none) -> first -> next
                var list = new java.util.ArrayList<>(plugin.getLootProfileManager().all());
                list.sort(java.util.Comparator.comparing(lp -> lp.name.toLowerCase()));
                String cur = editing.lootProfile == null ? "" : editing.lootProfile;
                int idx = -1;
                for (int i=0;i<list.size();i++) { if (list.get(i).name.equalsIgnoreCase(cur)) { idx = i; break; } }
                if (list.isEmpty()) { editing.lootProfile = ""; }
                else {
                    if (cur.isBlank()) editing.lootProfile = list.get(0).name;
                    else if (idx >= 0 && idx+1 < list.size()) editing.lootProfile = list.get(idx+1).name;
                    else editing.lootProfile = "";
                }
                arenaManager.save();
                open(p, editing);
            }
            case CHEST -> {
                editing.lootDelivery = switch (editing.lootDelivery) {
                    case INHERIT -> ArenaEnums.LootDelivery.CHEST;
                    case CHEST -> ArenaEnums.LootDelivery.DROP;
                    case DROP -> ArenaEnums.LootDelivery.PER_PLAYER;
                    case PER_PLAYER -> ArenaEnums.LootDelivery.NONE;
                    case NONE -> ArenaEnums.LootDelivery.INHERIT;
                };
                arenaManager.save();
                open(p, editing);
            }
            case SHIELD -> {
                p.closeInventory();
                plugin.getArenaProtectionGui().open(p, editing);
            }
            case EMERALD -> {
                p.closeInventory();
                plugin.getArenaRewardsGui().open(p, editing);
            }
            case BARRIER -> {
                p.closeInventory();
                plugin.getArenaListGui().open(p);
            }
        }
    }
}
