package me.alec.tempestboss.arena;

import org.bukkit.Location;

public class RegionCuboid implements Region {

    private final Location min;
    private final Location max;

    public RegionCuboid(Location a, Location b) {
        if (a == null || b == null || a.getWorld() == null || b.getWorld() == null) throw new IllegalArgumentException("null");
        if (!a.getWorld().equals(b.getWorld())) throw new IllegalArgumentException("world");
        double minX = Math.min(a.getX(), b.getX());
        double minY = Math.min(a.getY(), b.getY());
        double minZ = Math.min(a.getZ(), b.getZ());
        double maxX = Math.max(a.getX(), b.getX());
        double maxY = Math.max(a.getY(), b.getY());
        double maxZ = Math.max(a.getZ(), b.getZ());
        this.min = new Location(a.getWorld(), minX, minY, minZ);
        this.max = new Location(a.getWorld(), maxX, maxY, maxZ);
    }

    @Override public boolean contains(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        if (!loc.getWorld().equals(min.getWorld())) return false;
        return loc.getX()>=min.getX() && loc.getX()<=max.getX()
                && loc.getY()>=min.getY() && loc.getY()<=max.getY()
                && loc.getZ()>=min.getZ() && loc.getZ()<=max.getZ();
    }

    @Override public Location getCenter() {
        return new Location(min.getWorld(), (min.getX()+max.getX())/2.0, (min.getY()+max.getY())/2.0, (min.getZ()+max.getZ())/2.0);
    }

    @Override public double approximateRadius() {
        double dx = max.getX()-min.getX();
        double dy = max.getY()-min.getY();
        double dz = max.getZ()-min.getZ();
        return Math.sqrt(dx*dx+dy*dy+dz*dz)/2.0;
    }

    public Location min(){return min.clone();}
    public Location max(){return max.clone();}
}
