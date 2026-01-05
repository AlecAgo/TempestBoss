package me.alec.tempestboss;

import me.alec.tempestboss.boss.BossType;
import me.alec.tempestboss.gui.*;
import me.alec.tempestboss.loot.LootManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class TempestBossPlugin extends JavaPlugin {

    private BossManager bossManager;
    private LootManager lootManager;

    private BossSpawnGui spawnGui;
    private MusicSelectGui musicGui;
    private LootEditorGui lootGui;
    private BossHubGui hubGui;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.lootManager = new LootManager(this);
        this.bossManager = new BossManager(this, lootManager);

        this.spawnGui = new BossSpawnGui(this, bossManager);
        this.musicGui = new MusicSelectGui(this, bossManager);
        this.lootGui = new LootEditorGui(this, lootManager);
        this.hubGui = new BossHubGui(this, bossManager, lootManager);

        getServer().getPluginManager().registerEvents(spawnGui, this);
        getServer().getPluginManager().registerEvents(musicGui, this);
        getServer().getPluginManager().registerEvents(lootGui, this);
        getServer().getPluginManager().registerEvents(hubGui, this);

        getLogger().info("TempestBoss v1.2.0 enabled");
    }

    @Override
    public void onDisable() {
        if (bossManager != null) bossManager.shutdown();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Players only.");
            return true;
        }

        // Admin only
        if (!p.hasPermission("tempestboss.admin")) {
            p.sendMessage("§cNo permission.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("bossgui")) {
            hubGui.open(p);
            return true;
        }

        if (command.getName().equalsIgnoreCase("bossspawn")) {
            spawnGui.open(p);
            return true;
        }

        if (command.getName().equalsIgnoreCase("boss")) {
            if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
                p.sendMessage("§b/bossgui§7 - open boss control panel");
                p.sendMessage("§b/boss reload§7 - reload config + loot");
                p.sendMessage("§b/boss kill§7 - remove active boss");
                p.sendMessage("§b/boss loot§7 - open loot editor");
                p.sendMessage("§b/boss music§7 - open music selector");
                p.sendMessage("§b/boss summon <tempest|lich> [normal|hard|mythic]§7 - summon quickly");
                return true;
            }

            if (args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                lootManager.reload();
                p.sendMessage("§aTempestBoss reloaded.");
                return true;
            }

            if (args[0].equalsIgnoreCase("kill")) {
                bossManager.despawnBoss(true);
                p.sendMessage("§aBoss removed.");
                return true;
            }

            if (args[0].equalsIgnoreCase("loot")) {
                lootGui.open(p, bossManager.getSelectedBossType(p));
                return true;
            }

            if (args[0].equalsIgnoreCase("music")) {
                musicGui.open(p, bossManager.getSelectedBossType(p));
                return true;
            }

            if (args[0].equalsIgnoreCase("summon")) {
                BossType type = BossType.TEMPEST;
                String diff = "NORMAL";

                if (args.length >= 2) {
                    if (args[1].equalsIgnoreCase("lich")) type = BossType.ABYSSAL_LICH;
                    if (args[1].equalsIgnoreCase("tempest")) type = BossType.TEMPEST;
                }
                if (args.length >= 3) {
                    diff = args[2].toUpperCase();
                    if (!diff.equals("NORMAL") && !diff.equals("HARD") && !diff.equals("MYTHIC")) diff = "NORMAL";
                }

                bossManager.spawnBoss(type, p.getLocation(), diff, p);
                return true;
            }

            return true;
        }

        return false;
    }
}
