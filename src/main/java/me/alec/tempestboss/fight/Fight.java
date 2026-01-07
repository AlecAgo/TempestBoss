package me.alec.tempestboss.fight;

import me.alec.tempestboss.arena.Arena;
import me.alec.tempestboss.boss.BossDefinition;
import net.kyori.adventure.bossbar.BossBar;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class Fight {
    public final Arena arena;
    public final BossDefinition boss;
    public final BossBar bossBar;

    public FightState state = FightState.WAITING_FOR_PLAYERS;

    public int countdownLeft = 0;
    public long stateSinceMillis = System.currentTimeMillis();

    // participation tracking
    public final HashSet<UUID> participants = new HashSet<>();
    public final HashMap<UUID, Double> damageDone = new HashMap<>();
    public final HashMap<UUID, Long> timeInsideMillis = new HashMap<>();

    public Fight(Arena arena, BossDefinition boss, BossBar bossBar) {
        this.arena = arena;
        this.boss = boss;
        this.bossBar = bossBar;
    }

    public void setState(FightState s) {
        this.state = s;
        this.stateSinceMillis = System.currentTimeMillis();
    }
}
