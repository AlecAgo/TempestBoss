package me.alec.tempestboss.arena;

import org.bukkit.Location;

public interface Region {
    boolean contains(Location loc);
    Location getCenter();
    double approximateRadius();
}
