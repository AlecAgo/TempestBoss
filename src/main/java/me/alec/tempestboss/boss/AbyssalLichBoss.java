
package me.alec.tempestboss.boss;

import me.alec.tempestboss.BossManager;
import me.alec.tempestboss.TempestBossPlugin;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.WitherSkeleton;

public class AbyssalLichBoss implements BossDefinition {

    private final TempestBossPlugin plugin;
    private final BossManager manager;
    private final BossType type = BossType.ABYSSAL_LICH;

    private final LivingEntity entity;
    private final double maxHealth;

    public AbyssalLichBoss(TempestBossPlugin plugin, BossManager manager, Location spawn, String difficulty) {
        this.plugin = plugin;
        this.manager = manager;

        this.entity = spawn.getWorld().spawn(spawn, WitherSkeleton.class, skel -> {
            skel.setCustomNameVisible(true);
            skel.setCustomName("§5Abyssal Lich");
            skel.setRemoveWhenFarAway(false);
            skel.setPersistent(true);
        });

        this.maxHealth = switch (difficulty == null ? "NORMAL" : difficulty.toUpperCase()) {
            case "HARD" -> 450.0;
            case "MYTHIC" -> 650.0;
            default -> 300.0;
        };

        if (entity.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);
        }
        entity.setHealth(Math.min(maxHealth, entity.getMaxHealth()));
    }

    @Override
    public BossType type() {
        return type;
    }

    @Override
    public LivingEntity getEntity() {
        return entity;
    }

    @Override
    public Location getCenter() {
        return entity.getLocation();
    }

    @Override
    public double getHealth() {
        return entity.isValid() ? entity.getHealth() : 0.0;
    }

    @Override
    public double getMaxHealth() {
        return maxHealth;
    }

    @Override
    public int getPhase() {
        double hp = getHealth();
        if (hp <= 0) return 99;
        double pct = hp / maxHealth;
        if (pct <= 0.33) return 3;
        if (pct <= 0.66) return 2;
        return 1;
    }

    @Override
    public void tick() {
        // minimal tick; you can add spells/AI here later
        if (!entity.isValid() || entity.isDead()) return;
    }

    @Override
    public void onDamage() {
        // hook point for effects/sounds if desired
    }
}
