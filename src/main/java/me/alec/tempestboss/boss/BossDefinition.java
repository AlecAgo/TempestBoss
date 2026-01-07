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
    int getPhase();
    void onPhaseChanged(int newPhase);
}
