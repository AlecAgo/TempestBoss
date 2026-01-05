package me.alec.tempestboss.util;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.util.Vector;

public class Particles {

    public static void sparks(World w, Location center, int amount) {
        w.spawnParticle(Particle.ELECTRIC_SPARK, center, amount, 0.3, 0.4, 0.3, 0.02);
    }

    public static void burst(World w, Location center, int amount) {
        w.spawnParticle(Particle.ELECTRIC_SPARK, center, amount, 0.7, 0.6, 0.7, 0.05);
        w.spawnParticle(Particle.END_ROD, center, Math.max(6, amount / 8), 0.6, 0.5, 0.6, 0.01);
    }

    public static void ring(World w, Location center, double radius, int points) {
        for (int i = 0; i < points; i++) {
            double a = (Math.PI * 2) * (i / (double) points);
            double x = Math.cos(a) * radius;
            double z = Math.sin(a) * radius;
            Location p = center.clone().add(x, 0, z);
            w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0, 0, 0, 0);
            w.spawnParticle(Particle.END_ROD, p, 1, 0, 0, 0, 0);
        }
    }

    public static void ringDust(World w, Location center, double radius, int points, Color color, float size) {
        Particle.DustOptions dust = new Particle.DustOptions(color, size);
        for (int i = 0; i < points; i++) {
            double a = (Math.PI * 2) * (i / (double) points);
            double x = Math.cos(a) * radius;
            double z = Math.sin(a) * radius;
            Location p = center.clone().add(x, 0, z);
            w.spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, dust);
        }
    }

    public static void orbit(World w, Location center, double radius, int points) {
        long t = System.currentTimeMillis();
        double base = (t % 3600) / 3600.0 * Math.PI * 2;
        for (int i = 0; i < points; i++) {
            double a = base + (Math.PI * 2) * (i / (double) points);
            double x = Math.cos(a) * radius;
            double z = Math.sin(a) * radius;
            double y = Math.sin(a * 2) * 0.25;
            Location p = center.clone().add(x, y, z);
            w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0, 0, 0, 0);
        }
        w.spawnParticle(Particle.DUST, center, 3, 0.2, 0.2, 0.2, 0,
                new Particle.DustOptions(Color.fromRGB(140, 80, 255), 1.2f));
    }

    public static void trail(World w, Location origin, Vector dir, int points) {
        Vector step = dir.clone().normalize().multiply(0.25);
        Location p = origin.clone();
        for (int i = 0; i < points; i++) {
            w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0, 0, 0, 0);
            p.add(step);
        }
    }

    public static void line(World w, Location from, Location to, int points, Particle particle) {
        for (int i=0;i<=points;i++) {
            double t = i/(double)points;
            double x = from.getX() + (to.getX()-from.getX())*t;
            double y = from.getY() + (to.getY()-from.getY())*t;
            double z = from.getZ() + (to.getZ()-from.getZ())*t;
            w.spawnParticle(particle, x, y, z, 1, 0,0,0,0);
        }
    }
}
