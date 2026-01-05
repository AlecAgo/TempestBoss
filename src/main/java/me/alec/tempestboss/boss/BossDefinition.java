package me.alec.tempestboss.boss;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.List;
import java.util.UUID;

public interface BossDefinition {

    BossType type();

    LivingEntity spawn(Location at, String difficulty);

    LivingEntity getEntity();

    void tick();

    void onDamage();

    void onDeath();

    double getHealth();

    double getMaxHealth();

    Location getCenter();

    List<UUID> getParticipants();

    /** Returns the current phase 1..3 (BossManager uses this for phase transitions). */
    int getPhase();

    /** Called by BossManager when it detects a phase transition. */
    void onPhaseChanged(int newPhase);
}
