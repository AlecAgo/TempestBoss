package me.alec.tempestboss.boss;

import me.alec.tempestboss.BossManager;
import me.alec.tempestboss.TempestBossPlugin;
import me.alec.tempestboss.util.Particles;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vex;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class AbyssalLichBoss implements BossDefinition {

    private final TempestBossPlugin plugin;
    private final BossManager mgr;
    private final MiniMessage mm = MiniMessage.miniMessage();

    private final Location center;
    private WitherSkeleton entity;
    private final List<UUID> participants = new ArrayList<>();

    private long lastNova=0,lastMark=0,lastBlink=0,lastPrison=0,lastWraiths=0;
    private int phase = 1;

    public AbyssalLichBoss(TempestBossPlugin plugin, BossManager mgr, Location at, String difficulty) {
        this.plugin = plugin;
        this.mgr = mgr;
        this.center = at.clone();
        spawn(at, difficulty);
    }

    @Override public BossType type(){return BossType.ABYSSAL_LICH;}

    @Override
    public WitherSkeleton spawn(Location at, String difficulty) {
        World w = at.getWorld();
        if (w == null) return null;

        entity = w.spawn(at, WitherSkeleton.class);
        entity.setRemoveWhenFarAway(false);
        entity.setCustomNameVisible(true);
        entity.setGlowing(true);

        double base = plugin.getConfig().getDouble("bosses.ABYSSAL_LICH.max-health", 560.0);
        double hp = switch (difficulty) {
            case "HARD" -> base * 1.25;
            case "MYTHIC" -> base * 1.6;
            default -> base;
        };

        AttributeInstance max = entity.getAttribute(Attribute.MAX_HEALTH);
        if (max != null) max.setBaseValue(hp);
        entity.setHealth(hp);

        AttributeInstance fr = entity.getAttribute(Attribute.FOLLOW_RANGE);
        if (fr != null) fr.setBaseValue(plugin.getConfig().getDouble("bosses.ABYSSAL_LICH.follow-range", 42.0));

        String name = plugin.getConfig().getString("bosses.ABYSSAL_LICH.display-name", "<dark_purple>Abyssal Lich</dark_purple>");
        entity.customName(mm.deserialize(name));

        double rad = plugin.getConfig().getDouble("boss.defaults.arena-radius", 45.0);
        for (Player p : w.getPlayers()) {
            if (p.getLocation().distanceSquared(at) <= rad*rad) participants.add(p.getUniqueId());
        }

        return entity;
    }

    @Override public WitherSkeleton getEntity(){return entity;}

    @Override
    public void tick() {
        if (entity == null || entity.isDead()) return;

        // update phase
        double pct = entity.getHealth() / getMaxHealth();
        phase = (pct > 0.66) ? 1 : (pct > 0.33 ? 2 : 3);

        double intensity = plugin.getConfig().getDouble("bosses.ABYSSAL_LICH.particles.intensity", 1.4);
        World w = entity.getWorld();
        Location aura = entity.getLocation().add(0, 1.0, 0);
        Particles.ringDust(w, aura.clone().add(0, -0.8, 0), 1.2, (int)(34*intensity), Color.fromRGB(30, 0, 70), 1.5f);
        Particles.sparks(w, aura, (int)(7*intensity));

        long now = System.currentTimeMillis();
        if (now - lastNova > cd("nova")) { lastNova = now; voidNova(); }
        if (now - lastMark > cd("curse")) { lastMark = now; curseMark(); }
        if (phase >= 2 && now - lastBlink > cd("blink")) { lastBlink = now; blinkSlam(); }
        if (phase >= 2 && now - lastWraiths > cd("wraiths")) { lastWraiths = now; summonWraiths(); }
        if (phase >= 3 && now - lastPrison > cd("prison")) { lastPrison = now; bonePrison(); }
    }

    private long cd(String key) {
        String base = "bosses.ABYSSAL_LICH.cooldowns.";
        long s = switch (key) {
            case "nova" -> plugin.getConfig().getLong(base+"nova-seconds",12);
            case "curse" -> plugin.getConfig().getLong(base+"curse-mark-seconds",11);
            case "blink" -> plugin.getConfig().getLong(base+"blink-seconds",10);
            case "prison" -> plugin.getConfig().getLong(base+"prison-seconds",18);
            case "wraiths" -> plugin.getConfig().getLong(base+"summon-wraiths-seconds",22);
            default -> 12;
        };
        return s*1000L;
    }

    private double dmg(String key) {
        String base = "bosses.ABYSSAL_LICH.damage.";
        return switch (key) {
            case "nova" -> plugin.getConfig().getDouble(base+"nova",7);
            case "curse" -> plugin.getConfig().getDouble(base+"curse-mark",6);
            case "blink" -> plugin.getConfig().getDouble(base+"blink-slam",7.5);
            case "prison" -> plugin.getConfig().getDouble(base+"prison",3);
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

    private void voidNova() {
        World w = entity.getWorld();
        Location c = entity.getLocation().clone();
        double intensity = plugin.getConfig().getDouble("bosses.ABYSSAL_LICH.particles.intensity", 1.4);

        w.playSound(c, Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 0.6f, 1.4f);

        new BukkitRunnable() {
            double r=1.0;
            @Override public void run() {
                if (entity == null || entity.isDead()) { cancel(); return; }
                Particles.ringDust(w, c.clone().add(0,0.15,0), r, (int)(70*intensity), Color.fromRGB(120,0,200), 2.0f);
                for (Player p : arenaPlayers()) {
                    double d = p.getLocation().distance(c);
                    if (Math.abs(d - r) < 1.0) {
                        p.damage(dmg("nova"), entity);
                        Vector kb = p.getLocation().toVector().subtract(c.toVector()).normalize().multiply(0.95);
                        kb.setY(0.25);
                        p.setVelocity(kb);
                    }
                }
                r += 1.1;
                if (r > 16) cancel();
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void curseMark() {
        List<Player> players = arenaPlayers();
        if (players.isEmpty()) return;
        Player target = players.get(new Random().nextInt(players.size()));
        World w = entity.getWorld();
        double intensity = plugin.getConfig().getDouble("bosses.ABYSSAL_LICH.particles.intensity", 1.4);

        w.playSound(target.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.HOSTILE, 0.8f, 1.2f);

        new BukkitRunnable() {
            int pulses=0;
            @Override public void run() {
                if (entity == null || entity.isDead() || !target.isOnline()) { cancel(); return; }
                Location l = target.getLocation().clone().add(0,0.2,0);
                Particles.ringDust(w, l, 1.7, (int)(54*intensity), Color.fromRGB(0,200,255), 1.7f);
                pulses++;
                if (pulses>=3) {
                    cancel();
                    w.playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, SoundCategory.HOSTILE, 0.5f, 0.8f);
                    Particles.burst(w, target.getLocation().add(0,1.0,0), (int)(80*intensity));
                    target.damage(dmg("curse"), entity);
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    private void summonWraiths() {
        World w = entity.getWorld();
        Location c = entity.getLocation();
        double intensity = plugin.getConfig().getDouble("bosses.ABYSSAL_LICH.particles.intensity", 1.4);
        w.playSound(c, Sound.BLOCK_END_PORTAL_SPAWN, SoundCategory.HOSTILE, 0.6f, 0.8f);
        int count = 3;
        for (int i=0;i<count;i++) {
            Location s = c.clone().add((Math.random()*8)-4, 1.0, (Math.random()*8)-4);
            w.spawn(s, Vex.class, vex -> {
                vex.setCustomName("Wraith");
                vex.setCustomNameVisible(false);
                vex.setCharging(true);
                vex.setGlowing(true);
            });
            Particles.burst(w, s, (int)(30*intensity));
        }
    }

    private void blinkSlam() {
        List<Player> players = arenaPlayers();
        if (players.isEmpty()) return;
        Player target = players.get(new Random().nextInt(players.size()));

        Location to = target.getLocation().clone();
        Vector back = to.getDirection().multiply(-1).normalize().multiply(2.0);
        Location dest = to.clone().add(back);
        dest.setY(to.getY());

        entity.teleport(dest);
        World w = entity.getWorld();
        double intensity = plugin.getConfig().getDouble("bosses.ABYSSAL_LICH.particles.intensity", 1.4);
        w.playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 0.8f, 0.9f);
        Particles.burst(w, dest.clone().add(0,1.0,0), (int)(50*intensity));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (entity == null || entity.isDead()) return;
            Location slam = entity.getLocation();
            w.playSound(slam, Sound.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.HOSTILE, 0.8f, 1.2f);
            Particles.ringDust(w, slam.clone().add(0,0.1,0), 2.6, (int)(80*intensity), Color.fromRGB(0,120,255), 2.1f);
            for (Player p : arenaPlayers()) {
                if (p.getLocation().distanceSquared(slam) <= 5.0*5.0) {
                    p.damage(dmg("blink"), entity);
                    Vector kb = p.getLocation().toVector().subtract(slam.toVector()).normalize().multiply(1.0);
                    kb.setY(0.28);
                    p.setVelocity(kb);
                }
            }
        }, 8L);
    }

    // Phase 3: bone prison boundary (soft damage on crossing)
    private void bonePrison() {
        List<Player> players = arenaPlayers();
        if (players.isEmpty()) return;
        Player target = players.get(new Random().nextInt(players.size()));
        Location c = target.getLocation().clone();
        World w = entity.getWorld();
        double r = 6.0;
        double intensity = plugin.getConfig().getDouble("bosses.ABYSSAL_LICH.particles.intensity", 1.4);

        w.playSound(c, Sound.BLOCK_BONE_BLOCK_BREAK, SoundCategory.HOSTILE, 0.9f, 0.8f);

        new BukkitRunnable() {
            int ticks=0;
            @Override public void run() {
                if (entity == null || entity.isDead()) { cancel(); return; }
                ticks += 10;
                Location ringC = c.clone();
                ringC.setY(ringC.getWorld().getHighestBlockYAt(ringC) + 0.2);
                Particles.ringDust(w, ringC, r, (int)(70*intensity), Color.fromRGB(240,240,240), 1.6f);

                for (Player p : players) {
                    double d = p.getLocation().distance(c);
                    if (d > r + 0.6) {
                        Vector push = c.toVector().subtract(p.getLocation().toVector()).normalize().multiply(1.1);
                        push.setY(0.25);
                        p.setVelocity(push);
                        if (ticks % 20 == 0) p.damage(dmg("prison"), entity);
                    }
                }

                if (ticks >= 8*20) cancel();
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    @Override
    public void onDamage() {
        if (entity == null) return;
        Particles.sparks(entity.getWorld(), entity.getLocation().add(0, 1.2, 0), 14);
    }

    @Override
    public void onDeath() {
        if (entity != null && entity.isValid()) entity.remove();
    }

    @Override
    public double getHealth(){return entity==null?0:entity.getHealth();}

    @Override
    public double getMaxHealth(){
        if (entity==null) return 1;
        AttributeInstance max = entity.getAttribute(Attribute.MAX_HEALTH);
        return max==null?1:max.getValue();
    }

    @Override
    public Location getCenter(){return center;}

    @Override
    public List<UUID> getParticipants(){return participants;}

    @Override
    public int getPhase(){return phase;}

    @Override
    public void onPhaseChanged(int newPhase){
        if (entity == null) return;
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WITHER_AMBIENT, SoundCategory.HOSTILE, 0.9f, 1.1f);
    }
}
