package me.alec.tempestboss;

import me.alec.tempestboss.gui.BossSpawnGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class TempestBossPlugin extends JavaPlugin {

    private BossManager bossManager;
    private BossSpawnGui gui;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.bossManager = new BossManager(this);
        this.gui = new BossSpawnGui(this, bossManager);
        getServer().getPluginManager().registerEvents(gui, this);
        getLogger().info("TempestBoss enabled.");
    }

    @Override
    public void onDisable() {
        if (bossManager != null) bossManager.shutdown();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("bossspawn")) return false;

        if (!(sender instanceof Player p)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("force")) {
            if (!p.hasPermission("tempestboss.force")) {
                p.sendMessage("No permission.");
                return true;
            }
            bossManager.spawnBoss(p.getLocation(), p);
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("kill")) {
            if (!p.hasPermission("tempestboss.admin")) {
                p.sendMessage("No permission.");
                return true;
            }
            bossManager.despawnBoss(true);
            p.sendMessage("Boss removed.");
            return true;
        }

        if (!p.hasPermission("tempestboss.gui")) {
            p.sendMessage("No permission.");
            return true;
        }

        gui.open(p);
        return true;
    }
}
