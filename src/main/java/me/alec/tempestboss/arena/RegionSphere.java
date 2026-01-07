package me.alec.tempestboss.arena;

import org.bukkit.Location;

public class RegionSphere implements Region {
    private final Location center;
    private final double radius;

    public RegionSphere(Location center, double radius) {
        this.center = center.clone();
        this.radius = radius;
    }

    @Override public boolean contains(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        if (!loc.getWorld().equals(center.getWorld())) return false;
        return loc.distanceSquared(center) <= radius*radius;
    }

    @Override public Location getCenter() { return center.clone(); }
    @Override public double approximateRadius() { return radius; }
    public double radius(){return radius;}
}
