package me.alec.tempestboss.boss;

import me.alec.tempestboss.BossManager;
import me.alec.tempestboss.TempestBossPlugin;
import me.alec.tempestboss.util.Particles;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.entity.Ravager;
import org.bukkit.entity.Vex;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class TempestHeraldBoss implements BossDefinition {

    private final TempestBossPlugin plugin;
    private final BossManager mgr;
    private final MiniMessage mm = MiniMessage.miniMessage();

    private final Location center;
    private Ravager entity;
    private final List<UUID> participants = new ArrayList<>();

    private long lastShockwave=0,lastMark=0,lastCage=0,lastStep=0,lastSummon=0,lastSpear=0,lastZones=0,lastRing=0;
    private int phase = 1;

    // charged zones
    private final List<Zone> zones = new ArrayList<>();

    public TempestHeraldBoss(TempestBossPlugin plugin, BossManager mgr, Location at, String difficulty) {
        this.plugin = plugin;
        this.mgr = mgr;
        this.center = at.clone();
        spawn(at, difficulty);
    }

    @Override public BossType type(){return BossType.TEMPEST;}

    @Override
    public Ravager spawn(Location at, String difficulty) {
        World w = at.getWorld();
        if (w == null) return null;

        entity = w.spawn(at, Ravager.class);
        entity.setRemoveWhenFarAway(false);
        entity.setCustomNameVisible(true);

        double base = plugin.getConfig().getDouble("bosses.TEMPEST.max-health", 650.0);
        double hp = switch (difficulty) {
            case "HARD" -> base * 1.25;
            case "MYTHIC" -> base * 1.6;
            default -> base;
        };

        AttributeInstance max = entity.getAttribute(Attribute.MAX_HEALTH);
        if (max != null) max.setBaseValue(hp);
        entity.setHealth(hp);

        AttributeInstance fr = entity.getAttribute(Attribute.FOLLOW_RANGE);
        if (fr != null) fr.setBaseValue(plugin.getConfig().getDouble("bosses.TEMPEST.follow-range", 48.0));

        String name = plugin.getConfig().getString("bosses.TEMPEST.display-name", "<aqua>Tempest Herald</aqua>");
        entity.customName(mm.deserialize(name));

        double rad = plugin.getConfig().getDouble("boss.defaults.arena-radius", 45.0);
        for (Player p : w.getPlayers()) {
            if (p.getLocation().distanceSquared(at) <= rad*rad) participants.add(p.getUniqueId());
        }

        return entity;
    }

    @Override public Ravager getEntity(){return entity;}

    @Override
    public void tick() {
        if (entity == null || entity.isDead()) return;

        // update phase
        double hp = entity.getHealth();
        double max = getMaxHealth();
        double pct = hp / max;
        int newPhase = (pct > 0.66) ? 1 : (pct > 0.33 ? 2 : 3);
        phase = newPhase;

        double intensity = plugin.getConfig().getDouble("bosses.TEMPEST.particles.intensity", 1.4);
        World w = entity.getWorld();
        Location aura = entity.getLocation().add(0, 1.2, 0);
        Particles.orbit(w, aura, 1.8, (int)(18*intensity));
        Particles.sparks(w, aura, (int)(7*intensity));

        // tick zones
        tickZones();

        long now = System.currentTimeMillis();
        if (now - lastShockwave > cd("shockwave")) { lastShockwave = now; shockwave(); }
        if (now - lastMark > cd("static-mark")) { lastMark = now; staticMark(); }

        if (phase >= 2 && now - lastSummon > cd("summon-wisps")) { lastSummon = now; summonWisps(phase); }
        if (phase >= 2 && now - lastStep > cd("thunder-step")) { lastStep = now; thunderStep(); }

        if (phase >= 2 && now - lastZones > cd("charged-zone")) { lastZones = now; spawnChargedZones(); }
        if (phase >= 3 && now - lastSpear > cd("spear-line")) { lastSpear = now; stormSpearLine(); }
        if (phase >= 3 && now - lastRing > cd("rotating-ring")) { lastRing = now; rotatingLightningRing(); }

        if (phase >= 3 && now - lastCage > cd("cage")) { lastCage = now; lightningCage(); }
    }

    private long cd(String key) {
        String base = "bosses.TEMPEST.cooldowns.";
        long s = switch (key) {
            case "shockwave" -> plugin.getConfig().getLong(base+"shockwave-seconds",10);
            case "static-mark" -> plugin.getConfig().getLong(base+"static-mark-seconds",12);
            case "summon-wisps" -> plugin.getConfig().getLong(base+"summon-wisps-seconds",20);
            case "thunder-step" -> plugin.getConfig().getLong(base+"thunder-step-seconds",9);
            case "cage" -> plugin.getConfig().getLong(base+"cage-seconds",18);
            case "spear-line" -> plugin.getConfig().getLong(base+"spear-line-seconds",16);
            case "charged-zone" -> plugin.getConfig().getLong(base+"charged-zone-seconds",14);
            case "rotating-ring" -> plugin.getConfig().getLong(base+"rotating-ring-seconds",22);
            default -> 10;
        };
        return s * 1000L;
    }

    private double dmg(String key) {
        String base = "bosses.TEMPEST.damage.";
        return switch (key) {
            case "shockwave" -> plugin.getConfig().getDouble(base+"shockwave",6);
            case "static-mark" -> plugin.getConfig().getDouble(base+"static-mark",8);
            case "thunder-step" -> plugin.getConfig().getDouble(base+"thunder-step",7);
            case "cage-cross" -> plugin.getConfig().getDouble(base+"cage-cross",3);
            case "spear-line" -> plugin.getConfig().getDouble(base+"spear-line",6);
            case "charged-zone" -> plugin.getConfig().getDouble(base+"charged-zone",4);
            case "rotating-ring" -> plugin.getConfig().getDouble(base+"rotating-ring",5);
            default -> 5;
        };
    }

    private List<Player> arenaPlayers() {
        double rad = plugin.getConfig().getDouble("boss.defaults.arena-radius",45);
        double rad2 = rad*rad;
        List<Player> out = new ArrayList<>();
        for (Player p : entity.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(center) <= rad2) out.add(p);
        }
        return out;
    }

    private void shockwave() {
        World w = entity.getWorld();
        Location c = entity.getLocation().clone();
        double intensity = plugin.getConfig().getDouble("bosses.TEMPEST.particles.intensity", 1.4);

        w.playSound(c, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.HOSTILE, 0.9f, 1.0f);

        new BukkitRunnable() {
            double r = 1.5;
            @Override public void run() {
                if (entity == null || entity.isDead()) { cancel(); return; }
                Particles.ring(w, c.clone().add(0, 0.2, 0), r, (int)(36*intensity));
                for (Player p : arenaPlayers()) {
                    double d = p.getLocation().distance(c);
                    if (Math.abs(d - r) < 1.0) {
                        p.damage(dmg("shockwave"), entity);
                        Vector kb = p.getLocation().toVector().subtract(c.toVector()).normalize().multiply(1.15);
                        kb.setY(0.35);
                        p.setVelocity(kb);
                    }
                }
                r += 1.2;
                if (r > 18) cancel();
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void staticMark() {
        List<Player> players = arenaPlayers();
        if (players.isEmpty()) return;
        Player target = players.get(new Random().nextInt(players.size()));
        World w = entity.getWorld();
        double intensity = plugin.getConfig().getDouble("bosses.TEMPEST.particles.intensity", 1.4);

        w.playSound(target.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.HOSTILE, 0.9f, 1.2f);

        new BukkitRunnable() {
            int pulses=0;
            @Override public void run() {
                if (entity == null || entity.isDead() || !target.isOnline()) { cancel(); return; }
                Location l = target.getLocation().clone().add(0, 0.2, 0);
                Particles.ring(w, l, 1.6, (int)(26*intensity));
                pulses++;
                if (pulses>=3) {
                    cancel();
                    w.strikeLightningEffect(target.getLocation());
                    target.damage(dmg("static-mark"), entity);
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    private void thunderStep() {
        List<Player> players = arenaPlayers();
        if (players.isEmpty()) return;
        Player target = players.get(new Random().nextInt(players.size()));

        Location from = entity.getLocation();
        Vector dir = target.getLocation().toVector().subtract(from.toVector()).normalize().multiply(1.4);
        dir.setY(0.35);
        entity.setVelocity(dir);

        World w = entity.getWorld();
        double intensity = plugin.getConfig().getDouble("bosses.TEMPEST.particles.intensity", 1.4);
        Particles.trail(w, from.clone().add(0,1.0,0), dir, (int)(20*intensity));
        w.playSound(from, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 0.8f, 0.7f);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (entity == null || entity.isDead()) return;
            Location land = entity.getLocation().clone();
            w.strikeLightningEffect(land);
            Particles.burst(w, land.clone().add(0, 0.2, 0), (int)(70*intensity));
            for (Player p : arenaPlayers()) {
                if (p.getLocation().distanceSquared(land) <= 4.8*4.8) {
                    p.damage(dmg("thunder-step"), entity);
                    Vector kb = p.getLocation().toVector().subtract(land.toVector()).normalize().multiply(1.2);
                    kb.setY(0.35);
                    p.setVelocity(kb);
                }
            }
        }, 16L);
    }

    private void summonWisps(int phase) {
        World w = entity.getWorld();
        Location c = entity.getLocation();
        int count = (phase == 2) ? 2 : 4;
        double intensity = plugin.getConfig().getDouble("bosses.TEMPEST.particles.intensity", 1.4);
        w.playSound(c, Sound.BLOCK_END_PORTAL_SPAWN, SoundCategory.HOSTILE, 0.6f, 1.1f);
        for (int i=0;i<count;i++) {
            Location s = c.clone().add((Math.random()*6)-3, 1.0, (Math.random()*6)-3);
            w.spawn(s, Vex.class, vex -> {
                vex.setCustomName("Storm Wisp");
                vex.setCustomNameVisible(false);
                vex.setCharging(true);
                vex.setGlowing(true);
            });
            Particles.burst(w, s, (int)(32*intensity));
        }
    }

    private void lightningCage() {
        World w = entity.getWorld();
        double cageR = 16.0;
        double intensity = plugin.getConfig().getDouble("bosses.TEMPEST.particles.intensity", 1.4);

        w.playSound(center, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.HOSTILE, 1.0f, 0.9f);
        for (int i=0;i<14;i++) {
            double ang = (Math.PI*2) * (i/14.0);
            Location p = center.clone().add(Math.cos(ang)*cageR, 0, Math.sin(ang)*cageR);
            w.strikeLightningEffect(p);
        }

        new BukkitRunnable() {
            int t=0;
            @Override public void run() {
                if (entity == null || entity.isDead()) { cancel(); return; }
                Particles.ring(w, center.clone().add(0,0.2,0), cageR, (int)(40*intensity));
                for (Player pl : arenaPlayers()) {
                    double d = pl.getLocation().distance(center);
                    if (d > cageR + 0.6) {
                        Vector push = center.toVector().subtract(pl.getLocation().toVector()).normalize().multiply(1.2);
                        push.setY(0.25);
                        pl.setVelocity(push);
                        pl.damage(dmg("cage-cross"), entity);
                    }
                }
                t += 10;
                if (t >= 8*20) cancel();
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    // NEW: spear line strike with telegraph
    private void stormSpearLine() {
        List<Player> players = arenaPlayers();
        if (players.isEmpty()) return;
        Player target = players.get(new Random().nextInt(players.size()));

        World w = entity.getWorld();
        Location from = entity.getLocation().clone().add(0, 1.2, 0);
        Location to = target.getLocation().clone();
        to.setY(from.getY());

        w.playSound(from, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.HOSTILE, 0.7f, 1.8f);

        // telegraph line
        new BukkitRunnable() {
            int t=0;
            @Override public void run() {
                if (entity == null || entity.isDead()) { cancel(); return; }
                Particles.line(w, from, to, 30, Particle.ELECTRIC_SPARK);
                t += 2;
                if (t >= 20) {
                    cancel();
                    // strike along the line
                    for (int i=0;i<=6;i++) {
                        double k = i/6.0;
                        Location p = from.clone().add((to.getX()-from.getX())*k, 0, (to.getZ()-from.getZ())*k);
                        w.strikeLightningEffect(p);
                        for (Player pl : arenaPlayers()) {
                            if (pl.getLocation().distanceSquared(p) <= 2.2*2.2) {
                                pl.damage(dmg("spear-line"), entity);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    // NEW: charged zones that punish camping
    private void spawnChargedZones() {
        World w = entity.getWorld();
        double rad = plugin.getConfig().getDouble("boss.defaults.arena-radius",45);
        int count = 3;
        for (int i=0;i<count;i++) {
            double ang = Math.random()*Math.PI*2;
            double r = 6 + Math.random()*(rad-10);
            Location z = center.clone().add(Math.cos(ang)*r, 0, Math.sin(ang)*r);
            zones.add(new Zone(z, System.currentTimeMillis() + 9000));
            w.playSound(z, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.HOSTILE, 0.6f, 1.3f);
        }
    }

    private void tickZones() {
        if (zones.isEmpty()) return;
        World w = entity.getWorld();
        double intensity = plugin.getConfig().getDouble("bosses.TEMPEST.particles.intensity", 1.4);
        long now = System.currentTimeMillis();
        zones.removeIf(z -> z.expiresAt < now);

        for (Zone z : zones) {
            Location c = z.center.clone();
            c.setY(c.getWorld().getHighestBlockYAt(c) + 0.2);
            Particles.ringDust(w, c, 2.6, (int)(40*intensity), Color.fromRGB(80, 220, 255), 1.6f);
            for (Player p : arenaPlayers()) {
                if (p.getLocation().distanceSquared(z.center) <= 2.6*2.6) {
                    if (now - z.lastDamageMillis > 800) {
                        z.lastDamageMillis = now;
                        p.damage(dmg("charged-zone"), entity);
                        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.HOSTILE, 0.4f, 1.8f);
                    }
                }
            }
        }
    }

    // NEW: rotating lightning ring (phase 3)
    private void rotatingLightningRing() {
        World w = entity.getWorld();
        double intensity = plugin.getConfig().getDouble("bosses.TEMPEST.particles.intensity", 1.4);
        double ringR = 11.0;
        w.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.HOSTILE, 0.7f, 1.4f);

        new BukkitRunnable() {
            double a = 0;
            int ticks = 0;
            @Override public void run() {
                if (entity == null || entity.isDead()) { cancel(); return; }
                a += 0.20;
                ticks += 2;
                // draw a short arc segment
                for (int i=0;i<18;i++) {
                    double ang = a + (i/18.0)*1.2;
                    Location p = center.clone().add(Math.cos(ang)*ringR, 0.2, Math.sin(ang)*ringR);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0,0,0,0);
                }
                // damage if close to arc points
                for (Player pl : arenaPlayers()) {
                    double d = pl.getLocation().distance(center);
                    if (Math.abs(d - ringR) < 1.2) {
                        // only sometimes to avoid too much DPS
                        if (ticks % 10 == 0) pl.damage(dmg("rotating-ring"), entity);
                    }
                }
                if (ticks >= 12*20) cancel();
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    @Override
    public void onDamage() {
        if (entity == null) return;
        Particles.sparks(entity.getWorld(), entity.getLocation().add(0, 1.3, 0), 12);
    }

    @Override
    public void onDeath() {
        if (entity != null && entity.isValid()) entity.remove();
        zones.clear();
    }

    @Override
    public double getHealth() { return entity == null ? 0 : entity.getHealth(); }

    @Override
    public double getMaxHealth() {
        if (entity == null) return 1;
        AttributeInstance max = entity.getAttribute(Attribute.MAX_HEALTH);
        return max == null ? 1 : max.getValue();
    }

    @Override
    public Location getCenter() { return center; }

    @Override
    public List<UUID> getParticipants() { return participants; }

    @Override
    public int getPhase() { return phase; }

    @Override
    public void onPhaseChanged(int newPhase) {
        // boss manager handles cinematic; we can add a roar
        if (entity == null) return;
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 1.0f, 0.9f);
    }

    private static class Zone {
        final Location center;
        final long expiresAt;
        long lastDamageMillis = 0;
        Zone(Location center, long expiresAt) {
            this.center = center.clone();
            this.expiresAt = expiresAt;
        }
    }
}
