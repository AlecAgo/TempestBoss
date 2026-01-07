
package me.alec.tempestboss.boss;

import me.alec.tempestboss.BossManager;
import me.alec.tempestboss.TempestBossPlugin;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.WitherSkeleton;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AbyssalLichBoss implements BossDefinition {

    private final TempestBossPlugin plugin;
    private final BossManager manager;

    private LivingEntity entity;
    private Location lastSpawn;
    private double maxHealth;
    private int lastPhase = -1;

    // BossDefinition requires this
    private final List<UUID> participants = new ArrayList<>();

    public AbyssalLichBoss(TempestBossPlugin plugin, BossManager manager, Location spawn, String difficulty) {
        this.plugin = plugin;
        this.manager = manager;
        spawn(spawn, difficulty);
    }

    @Override
    public BossType type() {
        return BossType.ABYSSAL_LICH;
    }

    @Override
    public LivingEntity spawn(Location at, String difficulty) {
        this.lastSpawn = at;

        // remove old entity if any
        if (this.entity != null && this.entity.isValid()) {
            this.entity.remove();
        }

        // difficulty -> health
        this.maxHealth = switch (difficulty == null ? "NORMAL" : difficulty.toUpperCase()) {
            case "HARD" -> 450.0;
            case "MYTHIC" -> 650.0;
            default -> 300.0;
        };

        this.entity = at.getWorld().spawn(at, WitherSkeleton.class, skel -> {
            skel.setCustomNameVisible(true);
            skel.setCustomName("§5Abyssal Lich");
            skel.setRemoveWhenFarAway(false);
            skel.setPersistent(true);
        });

        // Paper/Spigot 1.21.11: Attribute.MAX_HEALTH exists, not GENERIC_MAX_HEALTH [2](https://learn.microsoft.com/en-us/minecraft/creator/scriptapi/minecraft/server/itemstack?view=minecraft-bedrock-stable)[3](https://www.spigotmc.org/resources/authors/alecago007.1199884/)
        AttributeInstance inst = entity.getAttribute(Attribute.MAX_HEALTH);
        if (inst != null) inst.setBaseValue(maxHealth);
        entity.setHealth(maxHealth);

        this.lastPhase = getPhase();
        return this.entity;
    }

    @Override
    public LivingEntity getEntity() {
        return entity;
    }

    @Override
    public void tick() {
        if (entity == null || !entity.isValid() || entity.isDead()) return;

        int phase = getPhase();
        if (phase != lastPhase) {
            lastPhase = phase;
            onPhaseChanged(phase);
        }

        // add boss AI/spells here later
    }

    @Override
    public void onDamage() {
        // optional hook
    }

    @Override
    public void onDeath() {
        // optional hook (BossManager handles fight cleanup)
    }

    @Override
    public double getHealth() {
        return (entity != null && entity.isValid()) ? entity.getHealth() : 0.0;
    }

    @Override
    public double getMaxHealth() {
        return maxHealth;
    }

    @Override
    public Location getCenter() {
        if (entity != null && entity.isValid()) return entity.getLocation();
        return lastSpawn;
    }

    @Override
    public List<UUID> getParticipants() {
        return participants;
    }

    @Override
    public int getPhase() {
        double hp = getHealth();
        if (hp <= 0) return 99;

        double pct = hp / Math.max(1.0, maxHealth);
        if (pct <= 0.33) return 3;
        if (pct <= 0.66) return 2;
        return 1;
    }

    @Override
    public void onPhaseChanged(int newPhase) {
        // no-op for now; later you can trigger abilities/sounds per phase
    }
}
