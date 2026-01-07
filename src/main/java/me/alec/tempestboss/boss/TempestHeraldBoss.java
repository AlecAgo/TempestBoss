
package me.alec.tempestboss.boss;

import me.alec.tempestboss.BossManager;
import me.alec.tempestboss.TempestBossPlugin;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Evoker;
import org.bukkit.entity.LivingEntity;

public class TempestHeraldBoss implements BossDefinition {

    private final TempestBossPlugin plugin;
    private final BossManager manager;
    private final BossType type = BossType.TEMPEST;

    private final LivingEntity entity;
    private final double maxHealth;

    private int lastPhase = -1;

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

        // ✅ 1.21.11 uses Attribute.MAX_HEALTH (not GENERIC_MAX_HEALTH)
        AttributeInstance inst = entity.getAttribute(Attribute.MAX_HEALTH);
        if (inst != null) {
            inst.setBaseValue(maxHealth);
        }
        entity.setHealth(Math.min(maxHealth, entity.getHealth() > 0 ? maxHealth : maxHealth));
        entity.setHealth(maxHealth);

        // initialize phase
        lastPhase = getPhase();
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
        return (entity != null && entity.isValid()) ? entity.getHealth() : 0.0;
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
        if (entity == null || !entity.isValid() || entity.isDead()) return;

        int phase = getPhase();
        if (phase != lastPhase) {
            lastPhase = phase;
            onPhaseChanged(phase);
        }

        // add future boss logic here
    }

    @Override
    public void onDamage() {
        // optional hook
    }

    // ✅ required by your BossDefinition interface
    @Override
    public void onPhaseChanged(int newPhase) {
        // no-op for now; you can add sounds/attacks here later
    }
}
