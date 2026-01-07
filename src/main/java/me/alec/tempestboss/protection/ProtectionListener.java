package me.alec.tempestboss.protection;

import me.alec.tempestboss.BossManager;
import me.alec.tempestboss.arena.Arena;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public class ProtectionListener implements Listener {

    private final BossManager bossManager;

    public ProtectionListener(BossManager bossManager) {
        this.bossManager = bossManager;
    }

    private Arena arenaAt(Location loc) {
        return bossManager.findArenaAt(loc);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Arena a = arenaAt(e.getBlock().getLocation());
        if (a == null) return;
        if (!bossManager.isArenaProtectedNow(a)) return;
        if (a.protectionBreak) e.setCancelled(true);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        Arena a = arenaAt(e.getBlock().getLocation());
        if (a == null) return;
        if (!bossManager.isArenaProtectedNow(a)) return;
        if (a.protectionPlace) e.setCancelled(true);
    }

    @EventHandler
    public void onFluid(BlockFromToEvent e) {
        Arena a = arenaAt(e.getBlock().getLocation());
        if (a == null) return;
        if (!bossManager.isArenaProtectedNow(a)) return;
        if (a.protectionFluid) e.setCancelled(true);
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent e) {
        // if any block affected is inside protected arena, cancel
        for (var b : e.blockList()) {
            Arena a = arenaAt(b.getLocation());
            if (a != null && bossManager.isArenaProtectedNow(a) && a.protectionExplosions) {
                e.setCancelled(true);
                return;
            }
        }
    }
}
