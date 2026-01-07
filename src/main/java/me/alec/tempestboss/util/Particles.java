package me.alec.tempestboss.util;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

public class Particles {

    public static void sparks(World w, Location center, int amount) {
        w.spawnParticle(Particle.ELECTRIC_SPARK, center, amount, 0.3, 0.4, 0.3, 0.02);
    }

    public static void burst(World w, Location center, int amount) {
        w.spawnParticle(Particle.ELECTRIC_SPARK, center, amount, 0.7, 0.6, 0.7, 0.05);
        w.spawnParticle(Particle.END_ROD, center, Math.max(6, amount / 8), 0.6, 0.5, 0.6, 0.01);
    }

    public static void ringDust(World w, Location center, double radius, int points, Color color, float size) {
        Particle.DustOptions dust = new Particle.DustOptions(color, size);
        for (int i=0;i<points;i++) {
            double a = (Math.PI*2) * (i/(double)points);
            double x = Math.cos(a)*radius;
            double z = Math.sin(a)*radius;
            w.spawnParticle(Particle.DUST, center.clone().add(x,0,z), 1, 0,0,0,0, dust);
        }
    }
}
