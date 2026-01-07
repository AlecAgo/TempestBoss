
package me.alec.tempestboss;

import me.alec.tempestboss.arena.Arena;
import me.alec.tempestboss.arena.ArenaManager;
import me.alec.tempestboss.gui.LootEditorGui;
import me.alec.tempestboss.gui.MusicSelectGui;
import me.alec.tempestboss.gui.arena.ArenaListGui;
import me.alec.tempestboss.gui.category.*;
import me.alec.tempestboss.gui.loot.LootProfilesGui;
import me.alec.tempestboss.gui.loot.LootProfileEditorGui;
import me.alec.tempestboss.loot.LootManager;
import me.alec.tempestboss.loot.profile.LootProfileManager;
import me.alec.tempestboss.protection.LootChestLockListener;
import me.alec.tempestboss.protection.ProtectionListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class TempestBossPlugin extends JavaPlugin {

    private ArenaManager arenaManager;
    private LootManager lootManager;
    private LootProfileManager lootProfileManager;
    private BossManager bossManager;

    // GUIs
    private DashboardGui dashboardGui;
    private LiveFightsGui liveFightsGui;
    private LootSettingsGui lootSettingsGui;
    private CinematicSettingsGui cinematicSettingsGui;
    private ProtectionDefaultsGui protectionDefaultsGui;
    private ArenaListGui arenaListGui;
    private LootEditorGui lootEditorGui;
    private MusicSelectGui musicSelectGui;
    private LootProfilesGui lootProfilesGui;
    private LootProfileEditorGui lootProfileEditorGui;

    // ✅ MISSING FIELDS (were used but not declared)
    private me.alec.tempestboss.gui.arena.ArenaEditorGui arenaEditorGui;
    private me.alec.tempestboss.gui.arena.ArenaProtectionGui arenaProtectionGui;
    private me.alec.tempestboss.gui.arena.ArenaRewardsGui arenaRewardsGui;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.arenaManager = new ArenaManager(this);
        this.lootManager = new LootManager(this);
        this.lootProfileManager = new LootProfileManager(this);
        this.bossManager = new BossManager(this, arenaManager, lootManager, lootProfileManager);

        // register protection listeners
        getServer().getPluginManager().registerEvents(new ProtectionListener(bossManager), this);
        getServer().getPluginManager().registerEvents(new LootChestLockListener(bossManager), this);

        // GUIs
        this.dashboardGui = new DashboardGui(this, bossManager);
        this.liveFightsGui = new LiveFightsGui(this, bossManager);
        this.lootSettingsGui = new LootSettingsGui(this);
        this.cinematicSettingsGui = new CinematicSettingsGui(this);
        this.protectionDefaultsGui = new ProtectionDefaultsGui(this);
        this.arenaListGui = new ArenaListGui(this, arenaManager);

        // ✅ these classes now have proper constructors + open methods
        this.lootEditorGui = new LootEditorGui(this, lootManager);
        this.musicSelectGui = new MusicSelectGui(this, bossManager);

        this.lootProfilesGui = new LootProfilesGui(this, lootProfileManager);
        this.lootProfileEditorGui = new LootProfileEditorGui(this, lootProfileManager);

        getServer().getPluginManager().registerEvents(dashboardGui, this);
        getServer().getPluginManager().registerEvents(liveFightsGui, this);
        getServer().getPluginManager().registerEvents(lootSettingsGui, this);
        getServer().getPluginManager().registerEvents(cinematicSettingsGui, this);
        getServer().getPluginManager().registerEvents(protectionDefaultsGui, this);
        getServer().getPluginManager().registerEvents(arenaListGui, this);
        getServer().getPluginManager().registerEvents(lootEditorGui, this);
        getServer().getPluginManager().registerEvents(musicSelectGui, this);
        getServer().getPluginManager().registerEvents(lootProfilesGui, this);
        getServer().getPluginManager().registerEvents(lootProfileEditorGui, this);

        // Arena editor sub-guis
        this.arenaEditorGui = new me.alec.tempestboss.gui.arena.ArenaEditorGui(this, arenaManager);
        this.arenaProtectionGui = new me.alec.tempestboss.gui.arena.ArenaProtectionGui(this, arenaManager);
        this.arenaRewardsGui = new me.alec.tempestboss.gui.arena.ArenaRewardsGui(this, arenaManager);

        getServer().getPluginManager().registerEvents(arenaEditorGui, this);
        getServer().getPluginManager().registerEvents(arenaProtectionGui, this);
        getServer().getPluginManager().registerEvents(arenaRewardsGui, this);

        getLogger().info("TempestBoss v1.4.0 enabled");
    }

    @Override
    public void onDisable() {
        if (bossManager != null) bossManager.stopAllFights();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (!p.hasPermission("tempestboss.admin")) {
            p.sendMessage("§cNo permission.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("bossgui")) {
            dashboardGui.open(p);
            return true;
        }

        if (command.getName().equalsIgnoreCase("boss")) {
            if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
                p.sendMessage("§b/bossgui§7 - open control panel");
                p.sendMessage("§b/boss reload§7 - reload config + arenas + loot");
                p.sendMessage("§b/boss stopall§7 - stop all fights");
                p.sendMessage("§b/boss arena gui§7 - arenas GUI");
                p.sendMessage("§b/boss arena create <name>§7 - create arena");
                p.sendMessage("§b/boss fights§7 - live fights GUI");
                p.sendMessage("§b/boss loot§7 - loot editor");
                p.sendMessage("§b/boss music§7 - music picker");
                p.sendMessage("§b/boss protection§7 - protection defaults");
                p.sendMessage("§b/boss cinematic§7 - cinematic settings");
                p.sendMessage("§b/boss lootprofile create <name>§7 - create loot profile");
                return true;
            }

            if (args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                bossManager.reloadAll();
                p.sendMessage("§aReloaded.");
                return true;
            }
            if (args[0].equalsIgnoreCase("stopall")) {
                bossManager.stopAllFights();
                p.sendMessage("§aStopped all fights.");
                return true;
            }
            if (args[0].equalsIgnoreCase("fights")) {
                liveFightsGui.open(p);
                return true;
            }
            if (args[0].equalsIgnoreCase("loot")) {
                lootSettingsGui.open(p);
                return true;
            }
            if (args[0].equalsIgnoreCase("music")) {
                musicSelectGui.open(p, bossManager.getSelectedBossType(p));
                return true;
            }
            if (args[0].equalsIgnoreCase("protection")) {
                protectionDefaultsGui.open(p);
                return true;
            }
            if (args[0].equalsIgnoreCase("cinematic")) {
                cinematicSettingsGui.open(p);
                return true;
            }
            if (args[0].equalsIgnoreCase("arena")) {
                if (args.length >= 2 && args[1].equalsIgnoreCase("gui")) {
                    arenaListGui.open(p);
                    return true;
                }
                if (args.length >= 3 && args[1].equalsIgnoreCase("create")) {
                    Arena a = arenaManager.create(args[2]);
                    arenaManager.save();
                    p.sendMessage("§aCreated arena: " + a.id);
                    return true;
                }
            }
            if (args[0].equalsIgnoreCase("lootprofile")) {
                if (args.length >= 3 && args[1].equalsIgnoreCase("create")) {
                    lootProfileManager.create(args[2]);
                    lootProfileManager.save();
                    p.sendMessage("§aCreated loot profile: " + args[2]);
                    return true;
                }
                if (args.length >= 3 && args[1].equalsIgnoreCase("delete")) {
                    lootProfileManager.delete(args[2]);
                    p.sendMessage("§aDeleted loot profile: " + args[2]);
                    return true;
                }
                if (args.length >= 2 && args[1].equalsIgnoreCase("gui")) {
                    lootProfilesGui.open(p);
                    return true;
                }
                p.sendMessage("§eUse: /boss lootprofile create <name> or /boss lootprofile gui");
                return true;
            }

            return true;
        }

        return false;
    }

    public BossManager getBossManager() { return bossManager; }
    public LootEditorGui getLootEditorGui() { return lootEditorGui; }
    public LootProfileManager getLootProfileManager() { return lootProfileManager; }
    public LootProfilesGui getLootProfilesGui() { return lootProfilesGui; }
    public LootProfileEditorGui getLootProfileEditorGui() { return lootProfileEditorGui; }
    public ArenaListGui getArenaListGui() { return arenaListGui; }

    public me.alec.tempestboss.gui.arena.ArenaEditorGui getArenaEditorGui() { return arenaEditorGui; }
    public me.alec.tempestboss.gui.arena.ArenaProtectionGui getArenaProtectionGui() { return arenaProtectionGui; }
    public me.alec.tempestboss.gui.arena.ArenaRewardsGui getArenaRewardsGui() { return arenaRewardsGui; }
}
