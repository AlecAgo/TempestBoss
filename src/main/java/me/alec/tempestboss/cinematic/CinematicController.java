package me.alec.tempestboss.cinematic;

import me.alec.tempestboss.TempestBossPlugin;
import me.alec.tempestboss.util.Particles;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.List;

public class CinematicController {

    private final TempestBossPlugin plugin;

    public CinematicController(TempestBossPlugin plugin) {
        this.plugin = plugin;
    }

    public void intro(Location center, LivingEntity boss, List<Player> players, Runnable done) {
        int seconds = (int) plugin.getConfig().getLong("cinematic.intro-seconds", 8);
        int duration = seconds * 20;
        boolean freeze = plugin.getConfig().getBoolean("cinematic.freeze-players", true);

        boss.setInvulnerable(true);
        try { boss.setAI(false); } catch (Throwable ignored) {}

        if (freeze) {
            for (Player p : players) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration + 20, 10, false, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, duration + 20, 200, false, false, false));
            }
        }

        Sound thunder = Sound.sound(Key.key("minecraft:item.trident.thunder"), Sound.Source.MASTER, 1.0f, 0.9f);
        for (Player p : players) {
            p.showTitle(Title.title(
                    Component.text("The air cracks"),
                    Component.text("A presence awakens..."),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(2), Duration.ofMillis(400))
            ));
            p.playSound(thunder);
        }

        World w = center.getWorld();
        if (w != null) {
            for (int i = 0; i < 8; i++) {
                Location strike = center.clone().add((Math.random()*16)-8, 0, (Math.random()*16)-8);
                w.strikeLightningEffect(strike);
            }
        }

        boolean shake = plugin.getConfig().getBoolean("cinematic.screen-shake.enabled", true);
        double shakeStrength = plugin.getConfig().getDouble("cinematic.screen-shake.strength", 0.25);

        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (!boss.isValid() || boss.isDead()) { cancel(); return; }

                if (w != null) {
                    double r = 2.0 + (t / (double)duration) * 10.0;
                    Particles.ringDust(w, center.clone().add(0, 0.15, 0), r, 72, Color.fromRGB(120, 200, 255), 1.6f);
                    Particles.burst(w, center.clone().add(0, 1.2, 0), 20);
                }

                if (shake && t % 20 == 0) {
                    for (Player p : players) {
                        p.setVelocity(p.getVelocity().add(new Vector(0, 0.02 * shakeStrength, 0)));
                    }
                }

                t += 2;
                if (t >= duration) {
                    cancel();
                    boss.setInvulnerable(false);
                    try { boss.setAI(true); } catch (Throwable ignored) {}
                    done.run();
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    public void phaseTransition(Location center, LivingEntity boss, List<Player> players, int newPhase, Runnable done) {
        int seconds = (int) plugin.getConfig().getLong("cinematic.phase-transition-seconds", 3);
        int duration = seconds * 20;
        boolean freeze = plugin.getConfig().getBoolean("cinematic.freeze-players", true);

        boss.setInvulnerable(true);
        try { boss.setAI(false); } catch (Throwable ignored) {}

        if (freeze) {
            for (Player p : players) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration + 10, 8, false, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, duration + 10, 200, false, false, false));
            }
        }

        Sound boom = Sound.sound(Key.key("minecraft:entity.lightning_bolt.thunder"), Sound.Source.MASTER, 0.9f, 0.8f);
        for (Player p : players) {
            p.playSound(boom);
        }

        World w = center.getWorld();
        if (w != null) w.strikeLightningEffect(center);

        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (!boss.isValid() || boss.isDead()) { cancel(); return; }
                if (w != null) {
                    double r = 2.0 + (t / 6.0);
                    Particles.ringDust(w, center.clone().add(0, 0.2, 0), r, 80, Color.fromRGB(180, 80, 255), 1.9f);
                    Particles.burst(w, center.clone().add(0, 1.2, 0), 24);
                }
                t += 2;
                if (t >= duration) {
                    cancel();
                    boss.setInvulnerable(false);
                    try { boss.setAI(true); } catch (Throwable ignored) {}
                    done.run();
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }
}
