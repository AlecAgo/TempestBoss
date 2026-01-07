package me.alec.tempestboss.arena;

import me.alec.tempestboss.boss.BossType;
import org.bukkit.Location;

public class Arena {
    public final String id;
    public String displayName;

    public boolean enabled = true;
    public ArenaEnums.TriggerMode trigger = ArenaEnums.TriggerMode.MANUAL;

    public BossType bossType = BossType.TEMPEST;
    public String difficulty = "NORMAL";

    public int minPlayers = 1;
    public int countdownSeconds = 5;

    public ArenaEnums.IntroMode intro = ArenaEnums.IntroMode.INHERIT;
    public ArenaEnums.LootDelivery lootDelivery = ArenaEnums.LootDelivery.INHERIT;
    public String lootProfile = ""; // optional loot profile name

    public boolean protectionBreak = false;
    public boolean protectionPlace = false;
    public boolean protectionFluid = false;
    public boolean protectionExplosions = false;

    public Region region;
    public Location bossSpawn;

    public long cooldownSeconds = 120;
    public long lastStartMillis = 0;

    // reward eligibility
    public String rewardEligibility = "DAMAGE_OR_TIME";
    public double rewardMinDamage = 1.0;
    public int rewardMinTimeSeconds = 10;

    public Arena(String id) {
        this.id = id;
        this.displayName = id;
    }

    public boolean canStartNow() {
        return (System.currentTimeMillis() - lastStartMillis) >= cooldownSeconds * 1000L;
    }

    public void markStarted() {
        lastStartMillis = System.currentTimeMillis();
    }
}
