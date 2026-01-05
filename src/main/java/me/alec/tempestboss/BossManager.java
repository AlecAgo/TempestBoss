package me.alec.tempestboss;

import me.alec.tempestboss.util.Particles;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.entity.Ravager;
import org.bukkit.entity.Vex;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public class BossManager implements Listener {

    private final TempestBossPlugin plugin;
    private final NamespacedKey bossKey;
    private final MiniMessage mm = MiniMessage.miniMessage();

    private Ravager boss;
    private Location spawnLoc;
    private String difficulty = "NORMAL";

    private BossBar bossBar;

    private BukkitTask tickTask;
    private BukkitTask musicTask;

    private long lastShockwave = 0;
    private long lastMark = 0;
    private long lastCage = 0;
    private long lastStep = 0;
    private long lastSummon = 0;

    private final Set<UUID> musicOff = new HashSet<>();

    private long fightStartMillis = 0;
    private long lastPlayerSeenMillis = 0;

    public BossManager(TempestBossPlugin plugin) {
        this.plugin = plugin;
        this.bossKey = new NamespacedKey(plugin, "tempest_boss");
        PluginManager pm = plugin.getServer().getPluginManager();
        pm.registerEvents(this, plugin);
    }

    public void setDifficulty(String diff) {
        this.difficulty = diff;
    }

    public void togglePersonalMusic(Player p) {
        if (musicOff.contains(p.getUniqueId())) {
            musicOff.remove(p.getUniqueId());
            p.sendMessage("Boss music: ON");
        } else {
            musicOff.add(p.getUniqueId());
            p.sendMessage("Boss music: OFF");
            String key = plugin.getConfig().getString("music.sound", "MUSIC_END");
            try {
                p.stopSound(Sound.valueOf(key));
            } catch (Exception ignored) {}
        }
    }

    public boolean isActive() {
        return boss != null && !boss.isDead() && boss.isValid();
    }

    public void spawnBoss(Location loc, Player spawner) {
        if (isActive()) {
            spawner.sendMessage("Boss is already active!");
            return;
        }

        this.spawnLoc = loc.clone();
        this.fightStartMillis = System.currentTimeMillis();
        this.lastPlayerSeenMillis = System.currentTimeMillis();

        World w = loc.getWorld();
        if (w == null) return;

        ritualEffect(loc);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Ravager r = w.spawn(loc, Ravager.class, entity -> {
                entity.getPersistentDataContainer().set(bossKey, PersistentDataType.BYTE, (byte) 1);
                entity.setCustomNameVisible(true);
                entity.setAI(true);
                entity.setRemoveWhenFarAway(false);
            });

            double baseHp = plugin.getConfig().getDouble("boss.max-health", 600.0);
            double hp = switch (difficulty) {
                case "HARD" -> baseHp * 1.25;
                case "MYTHIC" -> baseHp * 1.6;
                default -> baseHp;
            };

            Objects.requireNonNull(r.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(hp);
            r.setHealth(hp);

            AttributeInstance fr = r.getAttribute(Attribute.FOLLOW_RANGE);
            if (fr != null) fr.setBaseValue(plugin.getConfig().getDouble("boss.follow-range", 48.0));

            String bossNameRaw = plugin.getConfig().getString("boss.name", "<aqua>Tempest Herald</aqua>");
            r.customName(mm.deserialize(bossNameRaw));

            this.boss = r;

            this.bossBar = BossBar.bossBar(
                    Component.text("Tempest Herald — Phase 1"),
                    1.0f,
                    BossBar.Color.PURPLE,
                    BossBar.Overlay.PROGRESS
            );

            startTickLoop();
            startMusicLoop();

            spawner.sendMessage("§bThe Tempest Herald has been summoned!");

        }, 40L);
    }

    public void despawnBoss(boolean silent) {
        stopTasks();
        if (bossBar != null) {
            for (Player p : plugin.getServer().getOnlinePlayers()) p.hideBossBar(bossBar);
        }
        if (boss != null && boss.isValid()) boss.remove();
        boss = null;
        spawnLoc = null;
        bossBar = null;
        if (!silent) plugin.getServer().broadcast(Component.text("The storm calms..."));
    }

    public void shutdown() {
        despawnBoss(true);
    }

    private void stopTasks() {
        if (tickTask != null) tickTask.cancel();
        if (musicTask != null) musicTask.cancel();
        tickTask = null;
        musicTask = null;
    }

    private void ritualEffect(Location loc) {
        World w = loc.getWorld();
        if (w == null) return;

        w.playSound(loc, Sound.ITEM_TRIDENT_THUNDER,
                (float) plugin.getConfig().getDouble("effects.thunder-volume", 1.0),
                (float) plugin.getConfig().getDouble("effects.thunder-pitch", 1.0));

        for (int i = 0; i < 6; i++) {
            Location strike = loc.clone().add(randomBetween(-6, 6), 0, randomBetween(-6, 6));
            w.strikeLightningEffect(strike);
        }

        plugin.getServer().getScheduler().runTaskTimer(plugin, task -> {
            if (isActive()) { task.cancel(); return; }
            Particles.spiral(w, loc.clone().add(0, 0.2, 0), 2.2, 40);
        }, 0L, 4L);
    }

    private void startTickLoop() {
        tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!isActive()) {
                despawnBoss(true);
                return;
            }

            double radius = plugin.getConfig().getDouble("boss.arena-radius", 45.0);
            List<Player> nearby = getPlayersNear(spawnLoc, radius);

            if (!nearby.isEmpty()) lastPlayerSeenMillis = System.currentTimeMillis();

            long noPlayersSeconds = (System.currentTimeMillis() - lastPlayerSeenMillis) / 1000;
            if (noPlayersSeconds >= plugin.getConfig().getLong("boss.despawn-if-no-players-seconds", 45)) {
                despawnBoss(false);
                return;
            }

            long fightSeconds = (System.currentTimeMillis() - fightStartMillis) / 1000;
            if (fightSeconds >= plugin.getConfig().getLong("boss.fight-time-limit-seconds", 900)) {
                despawnBoss(false);
                return;
            }

            updateBossBar();
            aura();

            double hp = boss.getHealth();
            double max = Objects.requireNonNull(boss.getAttribute(Attribute.MAX_HEALTH)).getValue();
            double pct = hp / max;
            int phase = (pct > 0.66) ? 1 : (pct > 0.33 ? 2 : 3);

            long now = System.currentTimeMillis();

            if (phase >= 1 && now - lastShockwave > plugin.getConfig().getLong("cooldowns.shockwave-seconds", 10) * 1000L) {
                lastShockwave = now;
                doShockwave(radius);
            }

            if (phase >= 1 && now - lastMark > plugin.getConfig().getLong("cooldowns.static-mark-seconds", 12) * 1000L) {
                lastMark = now;
                doStaticMark(nearby);
            }

            if (phase >= 2 && now - lastSummon > plugin.getConfig().getLong("cooldowns.summon-wisps-seconds", 20) * 1000L) {
                lastSummon = now;
                summonWisps(phase);
            }

            if (phase >= 2 && now - lastStep > plugin.getConfig().getLong("cooldowns.thunder-step-seconds", 9) * 1000L) {
                lastStep = now;
                doThunderStep(nearby);
            }

            if (phase >= 3 && now - lastCage > plugin.getConfig().getLong("cooldowns.cage-seconds", 18) * 1000L) {
                lastCage = now;
                doLightningCage();
            }

        }, 1L, 2L);
    }

    private void startMusicLoop() {
        if (!plugin.getConfig().getBoolean("music.enabled", true)) return;

        long period = plugin.getConfig().getLong("music.restart-every-seconds", 90) * 20L;

        musicTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!isActive()) return;

            double radius = plugin.getConfig().getDouble("boss.arena-radius", 45.0);
            List<Player> nearby = getPlayersNear(spawnLoc, radius);

            String soundKey = plugin.getConfig().getString("music.sound", "MUSIC_END");
            Sound s;
            try { s = Sound.valueOf(soundKey); }
            catch (IllegalArgumentException ex) { s = Sound.MUSIC_END; }

            float vol = (float) plugin.getConfig().getDouble("music.volume", 0.9);
            float pitch = (float) plugin.getConfig().getDouble("music.pitch", 1.0);

            for (Player p : nearby) {
                if (musicOff.contains(p.getUniqueId())) continue;
                p.playSound(p.getLocation(), s, vol, pitch);
            }

        }, 20L, period);
    }

    private void updateBossBar() {
        if (bossBar == null || spawnLoc == null) return;
        double hp = boss.getHealth();
        double max = Objects.requireNonNull(boss.getAttribute(Attribute.MAX_HEALTH)).getValue();
        float prog = (float) Math.max(0, Math.min(1, hp / max));

        int phase = (prog > 0.66f) ? 1 : (prog > 0.33f ? 2 : 3);
        BossBar.Color color = (phase == 1) ? BossBar.Color.PURPLE : (phase == 2 ? BossBar.Color.BLUE : BossBar.Color.RED);

        bossBar.progress(prog);
        bossBar.color(color);
        bossBar.name(Component.text("Tempest Herald — Phase " + phase));

        double rad = plugin.getConfig().getDouble("boss.arena-radius", 45.0);
        double rad2 = rad * rad;

        for (Player p : plugin.getServer().getOnlinePlayers()) {
            boolean shouldSee = p.getWorld().equals(spawnLoc.getWorld()) && p.getLocation().distanceSquared(spawnLoc) <= rad2;
            if (shouldSee) p.showBossBar(bossBar);
            else p.hideBossBar(bossBar);
        }
    }

    private void aura() {
        World w = boss.getWorld();
        double intensity = plugin.getConfig().getDouble("particles.intensity", 1.0);
        Location center = boss.getLocation().add(0, 1.2, 0);

        Particles.orbit(w, center, 1.6, (int) (16 * intensity));
        Particles.sparks(w, center, (int) (6 * intensity));
    }

    private void doShockwave(double radius) {
        World w = boss.getWorld();
        Location c = boss.getLocation().clone();

        double dmg = plugin.getConfig().getDouble("damage.shockwave", 6.0);
        double intensity = plugin.getConfig().getDouble("particles.intensity", 1.0);

        w.playSound(c, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.0f);

        plugin.getServer().getScheduler().runTaskTimer(plugin, new org.bukkit.scheduler.BukkitRunnable() {
            double r = 1.5;
            @Override
            public void run() {
                if (!isActive()) { cancel(); return; }
                Particles.ring(w, c.clone().add(0, 0.2, 0), r, (int) (30 * intensity));

                for (Player p : getPlayersNear(spawnLoc, radius)) {
                    double d = p.getLocation().distance(c);
                    if (Math.abs(d - r) < 1.0) {
                        p.damage(dmg, boss);
                        Vector kb = p.getLocation().toVector().subtract(c.toVector()).normalize().multiply(1.1);
                        kb.setY(0.35);
                        p.setVelocity(kb);
                    }
                }

                r += 1.25;
                if (r > 18) cancel();
            }
        }.runTaskTimer(plugin, 0L, 2L));
    }

    private void doStaticMark(List<Player> nearby) {
        if (nearby.isEmpty()) return;
        Player target = nearby.get(new Random().nextInt(nearby.size()));

        World w = target.getWorld();
        w.playSound(target.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.9f, 1.2f);

        double intensity = plugin.getConfig().getDouble("particles.intensity", 1.0);

        plugin.getServer().getScheduler().runTaskTimer(plugin, new org.bukkit.scheduler.BukkitRunnable() {
            int steps = 0;
            @Override
            public void run() {
                if (!isActive() || !target.isOnline()) { cancel(); return; }
                Location l = target.getLocation().clone().add(0, 0.2, 0);
                Particles.ring(w, l, 1.4, (int) (22 * intensity));
                steps++;
                if (steps >= 3) {
                    cancel();
                    boolean visualOnly = plugin.getConfig().getBoolean("effects.lightning-visual-only", true);
                    if (visualOnly) w.strikeLightningEffect(target.getLocation());
                    else w.strikeLightning(target.getLocation());

                    if (plugin.getConfig().getBoolean("effects.lightning-damage", false)) {
                        target.damage(plugin.getConfig().getDouble("damage.static-mark", 8.0), boss);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 10L));
    }

    private void doLightningCage() {
        World w = boss.getWorld();
        Location c = spawnLoc.clone();

        double cageR = 16.0;
        double intensity = plugin.getConfig().getDouble("particles.intensity", 1.0);

        w.playSound(c, Sound.ITEM_TRIDENT_THUNDER, 1.0f, 0.9f);

        for (int i = 0; i < 14; i++) {
            double ang = (Math.PI * 2) * (i / 14.0);
            Location p = c.clone().add(Math.cos(ang) * cageR, 0, Math.sin(ang) * cageR);
            w.strikeLightningEffect(p);
        }

        long durationTicks = 8 * 20L;
        plugin.getServer().getScheduler().runTaskTimer(plugin, new org.bukkit.scheduler.BukkitRunnable() {
            long t = 0;
            @Override
            public void run() {
                if (!isActive()) { cancel(); return; }
                Particles.ring(w, c.clone().add(0, 0.2, 0), cageR, (int) (36 * intensity));

                for (Player pl : getPlayersNear(spawnLoc, plugin.getConfig().getDouble("boss.arena-radius", 45.0))) {
                    double d = pl.getLocation().distance(c);
                    if (d > cageR + 0.6) {
                        Vector push = c.toVector().subtract(pl.getLocation().toVector()).normalize().multiply(1.2);
                        push.setY(0.25);
                        pl.setVelocity(push);
                        pl.damage(plugin.getConfig().getDouble("damage.cage-cross", 3.0), boss);
                    }
                }

                t += 10;
                if (t >= durationTicks) cancel();
            }
        }.runTaskTimer(plugin, 0L, 10L));
    }

    private void doThunderStep(List<Player> nearby) {
        if (nearby.isEmpty()) return;
        Player target = nearby.get(new Random().nextInt(nearby.size()));

        Location from = boss.getLocation();
        Location to = target.getLocation();

        World w = boss.getWorld();
        w.playSound(from, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.7f);

        Vector dir = to.toVector().subtract(from.toVector()).normalize().multiply(1.4);
        dir.setY(0.35);
        boss.setVelocity(dir);

        double intensity = plugin.getConfig().getDouble("particles.intensity", 1.0);
        Particles.trail(w, from.clone().add(0, 1.0, 0), dir, (int) (18 * intensity));

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            Location land = boss.getLocation().clone();
            w.strikeLightningEffect(land);
            Particles.burst(w, land.clone().add(0, 0.2, 0), (int) (60 * intensity));
            for (Player p : getPlayersNear(land, 4.5)) {
                p.damage(plugin.getConfig().getDouble("damage.thunder-step", 7.0), boss);
                Vector kb = p.getLocation().toVector().subtract(land.toVector()).normalize().multiply(1.2);
                kb.setY(0.35);
                p.setVelocity(kb);
            }
        }, 16L);
    }

    private void summonWisps(int phase) {
        World w = boss.getWorld();
        Location c = boss.getLocation();

        int count = (phase == 2) ? 2 : 4;
        w.playSound(c, Sound.BLOCK_END_PORTAL_SPAWN, 0.6f, 1.1f);

        for (int i = 0; i < count; i++) {
            Location s = c.clone().add(randomBetween(-3, 3), 1.0, randomBetween(-3, 3));
            w.spawn(s, Vex.class, vex -> {
                vex.setCustomName("Storm Wisp");
                vex.setCustomNameVisible(false);
                vex.setCharging(true);
                vex.setSilent(false);
                vex.setRemoveWhenFarAway(true);
                vex.setGlowing(true);
            });
            Particles.burst(w, s, (int) (30 * plugin.getConfig().getDouble("particles.intensity", 1.0)));
        }
    }

    private List<Player> getPlayersNear(Location loc, double radius) {
        if (loc == null || loc.getWorld() == null) return List.of();
        double r2 = radius * radius;
        List<Player> out = new ArrayList<>();
        for (Player p : loc.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(loc) <= r2) out.add(p);
        }
        return out;
    }

    private double randomBetween(double min, double max) {
        return min + (Math.random() * (max - min));
    }

    @EventHandler
    public void onBossDeath(EntityDeathEvent e) {
        if (!isActive()) return;
        if (!(e.getEntity() instanceof Ravager r)) return;
        if (!r.equals(boss)) return;

        Location l = r.getLocation();
        World w = l.getWorld();
        w.playSound(l, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        w.strikeLightningEffect(l);
        Particles.burst(w, l.clone().add(0, 0.2, 0), 120);

        e.getDrops().add(new org.bukkit.inventory.ItemStack(Material.NETHER_STAR));
        despawnBoss(true);
    }

    @EventHandler
    public void onBossHit(EntityDamageByEntityEvent e) {
        if (!isActive()) return;
        if (!e.getEntity().equals(boss)) return;
        Particles.sparks(boss.getWorld(), boss.getLocation().add(0, 1.3, 0), (int) (10 * plugin.getConfig().getDouble("particles.intensity", 1.0)));
    }
}
