package me.alec.tempestboss.protection;

import me.alec.tempestboss.BossManager;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class LootChestLockListener implements Listener {

    private final BossManager bossManager;

    public LootChestLockListener(BossManager bossManager) {
        this.bossManager = bossManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getClickedBlock() == null) return;
        if (e.getClickedBlock().getType() != Material.CHEST) return;

        // check all locks
        for (var f : bossManager.getActiveFights()) {
            var lock = bossManager.getLootLock(f.arena.id);
            if (lock == null) continue;
            if (!lock.chestLoc.equals(e.getClickedBlock().getLocation())) continue;

            if (System.currentTimeMillis() < lock.unlockAtMillis) {
                if (!lock.allowed.contains(e.getPlayer().getUniqueId())) {
                    e.setCancelled(true);
                    e.getPlayer().sendMessage("§cLoot is locked for eligible players.");
                }
            }
        }
    }
}
