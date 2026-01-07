
package me.alec.tempestboss.boss;

import me.alec.tempestboss.BossManager;
import me.alec.tempestboss.TempestBossPlugin;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Evoker;
import org.bukkit.entity.LivingEntity;

public class TempestHeraldBoss implements BossDefinition {

    private final TempestBossPlugin plugin;
    private final BossManager manager;
    private final BossType type = BossType.TEMPEST;

    private final LivingEntity entity;
    private final double maxHealth;

    public TempestHeraldBoss(TempestBossPlugin plugin, BossManager manager, Location spawn, String difficulty) {
        this.plugin = plugin;
        this.manager = manager;

        this.entity = spawn.getWorld().spawn(spawn, Evoker.class, evoker -> {
            evoker.setCustomNameVisible(true);
            evoker.setCustomName("§bTempest Herald");
            evoker.setRemoveWhenFarAway(false);
            evoker.setPersistent(true);
        });

        this.maxHealth = switch (difficulty == null ? "NORMAL" : difficulty.toUpperCase()) {
            case "HARD" -> 400.0;
            case "MYTHIC" -> 600.0;
            default -> 260.0;
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
        if (!entity.isValid() || entity.isDead()) return;
        // minimal tick; extend later
    }

    @Override
    public void onDamage() {
        // hook point
    }
}
