
package me.alec.tempestboss;

import me.alec.tempestboss.arena.*;
import me.alec.tempestboss.boss.*;
import me.alec.tempestboss.cinematic.CinematicController;
import me.alec.tempestboss.fight.*;
import me.alec.tempestboss.loot.LootManager;
import me.alec.tempestboss.loot.profile.LootProfile;
import me.alec.tempestboss.loot.profile.LootProfileManager;
import me.alec.tempestboss.util.MusicUtil;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;


public class BossManager implements Listener {

    private final TempestBossPlugin plugin;
    private final ArenaManager arenaManager;
    private final LootManager lootManager;
    private final LootProfileManager lootProfileManager;
    private final CinematicController cinematic;

    private final MiniMessage mm = MiniMessage.miniMessage();

    // multi arena fights
    private final Map<String, Fight> fights = new HashMap<>();
    private BukkitTask arenaTick;

    // personal music toggle
    private final Set<UUID> musicOff = new HashSet<>();

    // GUI boss selection helper (per admin)
    private final Map<UUID, BossType> selectedBoss = new HashMap<>();

    // loot chest locks per fight
    private final Map<String, LootChestLock> lootLocks = new HashMap<>();

    public BossManager(TempestBossPlugin plugin, ArenaManager arenaManager, LootManager lootManager, LootProfileManager lootProfileManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
        this.lootManager = lootManager;
        this.lootProfileManager = lootProfileManager;
        this.cinematic = new CinematicController(plugin);

        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        startArenaTick();
    }

    public Collection<Fight> getActiveFights() {
        return fights.values();
    }

    public boolean isArenaRunning(Arena a) {
        return fights.containsKey(a.id.toLowerCase(Locale.ROOT));
    }

    public boolean isArenaProtectedNow(Arena a) {
        Fight f = fights.get(a.id.toLowerCase(Locale.ROOT));
        if (f == null) return false;
        return switch (f.state) {
            case COUNTDOWN, INTRO, FIGHTING, PHASE_TRANSITION -> true;
            default -> false;
        };
    }

    public Arena findArenaAt(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        for (Arena a : arenaManager.all()) {
            if (!a.enabled || a.region == null) continue;
            if (a.region.contains(loc)) return a;
        }
        return null;
    }

    public void togglePersonalMusic(Player p) {
        UUID id = p.getUniqueId();
        if (musicOff.contains(id)) {
            musicOff.remove(id);
            p.sendMessage("§aBoss music: ON");
        } else {
            musicOff.add(id);
            p.sendMessage("§cBoss music: OFF");
        }
    }

    public BossType getSelectedBossType(Player p) {
        return selectedBoss.getOrDefault(p.getUniqueId(), BossType.TEMPEST);
    }

    public void setSelectedBossType(Player p, BossType t) {
        selectedBoss.put(p.getUniqueId(), t);
    }

    public void reloadAll() {
        arenaManager.reload();
        lootManager.reload();
    }

    public void stopAllFights() {
        for (String id : new ArrayList<>(fights.keySet())) {
            endFight(id, true);
        }
    }

    private void startArenaTick() {
        int period = plugin.getConfig().getInt("arenas.enter-check-period-ticks", 10);
        arenaTick = new BukkitRunnable() {
            @Override
            public void run() {
                tickArenas();
                tickFights();
                tickLootLocks();
            }
        }.runTaskTimer(plugin, period, period);
    }

    private void tickArenas() {
        for (Arena a : arenaManager.all()) {
            if (!a.enabled || a.region == null) continue;

            // ensure fight exists if manual? we only create on trigger or admin
            if (a.trigger == ArenaEnums.TriggerMode.ENTER) {
                if (isArenaRunning(a)) continue;
                if (!a.canStartNow()) continue;

                int count = countEligiblePlayersInside(a);
                int required = a.minPlayers;

                // requirement UI
                showRequirementUI(a, count, required);

                if (count >= required) {
                    startCountdownOrSpawn(a);
                }
            }
        }
    }

    private void startCountdownOrSpawn(Arena a) {
        String key = a.id.toLowerCase(Locale.ROOT);
        if (fights.containsKey(key)) return;

        // create boss entity now? We'll spawn at end of countdown; during countdown we create a dummy Fight without boss.
        // For simplicity, we spawn boss immediately but keep invulnerable during intro/countdown.

        Location spawn = a.bossSpawn != null ? a.bossSpawn : a.region.getCenter();
        BossDefinition boss = switch (a.bossType) {
            case ABYSSAL_LICH -> new AbyssalLichBoss(plugin, this, spawn, a.difficulty);
            default -> new TempestHeraldBoss(plugin, this, spawn, a.difficulty);
        };

        BossBar bar = BossBar.bossBar(Component.text(a.displayName), 1.0f, BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS);
        Fight f = new Fight(a, boss, bar);
        f.setState(FightState.COUNTDOWN);
        f.countdownLeft = Math.max(0, a.countdownSeconds);

        // capture participants snapshot (players in arena now)
        for (Player p : playersInside(a)) {
            f.participants.add(p.getUniqueId());
        }

        fights.put(key, f);
        a.markStarted();
        arenaManager.save();

        // if countdown 0 -> go intro/fight
        if (f.countdownLeft <= 0) {
            moveToIntroOrFight(f);
        }
    }

    private void tickFights() {
        for (Fight f : new ArrayList<>(fights.values())) {
            if (f.boss.getEntity() == null || f.boss.getEntity().isDead()) {
                // boss dead will be handled by death event; just skip
            }

            // update players inside time tracking
            for (Player p : playersInside(f.arena)) {
                f.timeInsideMillis.putIfAbsent(p.getUniqueId(), 0L);
                f.timeInsideMillis.put(p.getUniqueId(), f.timeInsideMillis.get(p.getUniqueId()) + (long)(plugin.getConfig().getInt("arenas.enter-check-period-ticks",10) * 50L));
            }

            // show bossbar to players inside arena
            updateBossBarVisibility(f);

            switch (f.state) {
                case COUNTDOWN -> tickCountdown(f);
                case INTRO -> {
                    // waiting for cinematic callback via flag; handled in moveToIntroOrFight
                }
                case FIGHTING -> {
                    // tick boss
                    f.boss.tick();
                    updateBossBarProgress(f);
                    // phase transitions cinematic (optional)
                    int phase = f.boss.getPhase();
                    // We'll let boss internal phase effects; cinematic transitions are light here
                }
                default -> {
                }
            }

            // music
            tickMusic(f);
        }
    }

    private void tickCountdown(Fight f) {
        // if cancel countdown if below min
        boolean cancelIfBelow = plugin.getConfig().getBoolean("arenas.cancel-countdown-if-below-min", true);
        int eligible = countEligiblePlayersInside(f.arena);

        if (cancelIfBelow && eligible < f.arena.minPlayers) {
            // reset fight
            endFight(f.arena.id, true);
            return;
        }

        // decrement countdown each second
        long elapsed = System.currentTimeMillis() - f.stateSinceMillis;
        if (elapsed >= 1000) {
            f.stateSinceMillis = System.currentTimeMillis();
            f.countdownLeft -= 1;

            for (Player p : playersInside(f.arena)) {
                p.sendActionBar(Component.text("Boss starts in " + f.countdownLeft + "s"));
                p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.4f, 1.2f);
            }

            if (f.countdownLeft <= 0) {
                moveToIntroOrFight(f);
            }
        }
    }

    private void moveToIntroOrFight(Fight f) {
        boolean cinematicEnabled = plugin.getConfig().getBoolean("cinematic.enabled", true);
        boolean doIntro = switch (f.arena.intro) {
            case ON -> true;
            case OFF -> false;
            case INHERIT -> cinematicEnabled;
        };

        if (doIntro) {
            f.setState(FightState.INTRO);
            List<Player> inside = playersInside(f.arena);
            transitioningInvuln(f, true);
            cinematic.intro(f.boss.getCenter(), f.boss.getEntity(), inside, () -> {
                transitioningInvuln(f, false);
                f.setState(FightState.FIGHTING);
            });
        } else {
            f.setState(FightState.FIGHTING);
        }
    }

    private void transitioningInvuln(Fight f, boolean invuln) {
        try {
            if (f.boss.getEntity() != null) f.boss.getEntity().setInvulnerable(invuln);
        } catch (Throwable ignored) {}
    }

    private void tickMusic(Fight f) {
        if (!plugin.getConfig().getBoolean("music.enabled", true)) return;
        long periodMs = plugin.getConfig().getLong("music.restart-every-seconds", 90) * 1000L;
        long since = System.currentTimeMillis() - f.stateSinceMillis;
        // don't reuse stateSinceMillis; use modulo on current time
        if ((System.currentTimeMillis() / periodMs) == ( (System.currentTimeMillis()-plugin.getConfig().getInt("arenas.enter-check-period-ticks",10)*50L) / periodMs)) {
            // same bucket; do nothing
            return;
        }

        float vol = (float) plugin.getConfig().getDouble("music.volume", 0.9);
        float pitch = (float) plugin.getConfig().getDouble("music.pitch", 1.0);
        Material disc = getBossMusicDisc(f.arena.bossType);
        Sound s = MusicUtil.discToSound(disc, vol, pitch);

        for (Player p : playersInside(f.arena)) {
            if (musicOff.contains(p.getUniqueId())) continue;
            p.playSound(s);
        }
    }

    private Material getBossMusicDisc(BossType type) {
        String path = "bosses." + type.name() + ".music-disc";
        String discName = plugin.getConfig().getString(path, type == BossType.TEMPEST ? "MUSIC_DISC_PIGSTEP" : "MUSIC_DISC_OTHERSIDE");
        try { return Material.valueOf(discName); } catch (Exception ignored) {}
        return Material.MUSIC_DISC_13;
    }

    private void updateBossBarVisibility(Fight f) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            boolean inside = f.arena.region != null && f.arena.region.contains(p.getLocation());
            if (inside) p.showBossBar(f.bossBar);
            else p.hideBossBar(f.bossBar);
        }
    }

    private void updateBossBarProgress(Fight f) {
        double hp = f.boss.getHealth();
        double max = f.boss.getMaxHealth();
        float prog = (float) Math.max(0.0, Math.min(1.0, hp / max));
        f.bossBar.progress(prog);
    }

    private void tickLootLocks() {
        lootLocks.entrySet().removeIf(e -> System.currentTimeMillis() >= e.getValue().unlockAtMillis);
    }

    public void endFight(String arenaId, boolean silent) {
        String key = arenaId.toLowerCase(Locale.ROOT);
        Fight f = fights.remove(key);
        if (f == null) return;

        // hide boss bar
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.hideBossBar(f.bossBar);
        }

        // remove boss
        try {
            if (f.boss.getEntity() != null && f.boss.getEntity().isValid()) f.boss.getEntity().remove();
        } catch (Throwable ignored) {}

        if (!silent) Bukkit.broadcast(Component.text("Fight ended in arena " + f.arena.displayName));
    }

    private List<Player> playersInside(Arena a) {
        if (a.region == null || a.region.getCenter().getWorld() == null) return List.of();
        List<Player> out = new ArrayList<>();
        for (Player p : a.region.getCenter().getWorld().getPlayers()) {
            if (a.region.contains(p.getLocation())) out.add(p);
        }
        return out;
    }

    private int countEligiblePlayersInside(Arena a) {
        int count = 0;
        int minStay = plugin.getConfig().getInt("counting.min-stay-seconds", 2);
        boolean exclSpec = plugin.getConfig().getBoolean("counting.exclude-spectators", true);
        boolean exclDead = plugin.getConfig().getBoolean("counting.exclude-dead", true);

        for (Player p : playersInside(a)) {
            if (exclSpec && p.getGameMode() == GameMode.SPECTATOR) continue;
            if (exclDead && p.isDead()) continue;
            // stay time: approximated by keeping per fight timer. For waiting phase (no fight), we skip and count immediately.
            count++;
        }
        return count;
    }

    private void showRequirementUI(Arena a, int count, int required) {
        int showEvery = plugin.getConfig().getInt("arenas.requirement-ui.show-every-ticks", 20);
        long tick = plugin.getServer().getCurrentTick();
        if (tick % showEvery != 0) return;

        if (count >= required) return;

        boolean actionbar = plugin.getConfig().getBoolean("arenas.requirement-ui.actionbar-enabled", true);
        boolean subtitle = plugin.getConfig().getBoolean("arenas.requirement-ui.subtitle-enabled", true);
        String abTpl = plugin.getConfig().getString("arenas.requirement-ui.actionbar-template", "{arena} {count}/{required}");
        String subTpl = plugin.getConfig().getString("arenas.requirement-ui.subtitle-template", "Need {needed} more");

        int needed = Math.max(0, required - count);

        String ab = abTpl.replace("{arena}", a.displayName).replace("{count}", String.valueOf(count)).replace("{required}", String.valueOf(required));
        String sub = subTpl.replace("{arena}", a.displayName).replace("{count}", String.valueOf(count)).replace("{required}", String.valueOf(required)).replace("{needed}", String.valueOf(needed));

        for (Player p : playersInside(a)) {
            if (actionbar) p.sendActionBar(mm.deserialize(ab));
            if (subtitle) p.sendTitlePart(net.kyori.adventure.title.TitlePart.SUBTITLE, mm.deserialize(sub));
        }
    }

    @EventHandler
    public void onBossHit(EntityDamageByEntityEvent e) {
        // assign damage to fight
        for (Fight f : fights.values()) {
            if (f.boss.getEntity() == null) continue;
            if (!e.getEntity().getUniqueId().equals(f.boss.getEntity().getUniqueId())) continue;

            if (e.getDamager() instanceof Player p) {
                f.participants.add(p.getUniqueId());
                f.damageDone.put(p.getUniqueId(), f.damageDone.getOrDefault(p.getUniqueId(), 0.0) + e.getFinalDamage());
            }
            f.boss.onDamage();
            return;
        }
    }

    @EventHandler
    public void onBossDeath(EntityDeathEvent e) {
        for (Fight f : new ArrayList<>(fights.values())) {
            if (f.boss.getEntity() == null) continue;
            if (!e.getEntity().getUniqueId().equals(f.boss.getEntity().getUniqueId())) continue;

            // deliver loot per arena settings
            deliverLoot(f, e.getEntity().getLocation());

            // end fight
            endFight(f.arena.id, true);
            return;
        }
    }

    private void deliverLoot(Fight f, Location at) {
        ArenaEnums.LootDelivery mode = f.arena.lootDelivery;
        if (mode == ArenaEnums.LootDelivery.INHERIT) {
            String g = plugin.getConfig().getString("loot.default-delivery", "CHEST");
            try { mode = ArenaEnums.LootDelivery.valueOf(g); } catch (Exception ignored) { mode = ArenaEnums.LootDelivery.CHEST; }
        }

        if (mode == ArenaEnums.LootDelivery.NONE) return;

        int rolls = 0;
        // if profile set, use rollsPerPlayer; else use all raw items
        var prof = (f.arena.lootProfile == null) ? null : lootProfileManager.get(f.arena.lootProfile);
        if (prof != null) rolls = Math.max(0, prof.rollsPerPlayer);

        List<ItemStack> items = resolveLootItems(f, (mode == ArenaEnums.LootDelivery.PER_PLAYER) ? rolls : (rolls > 0 ? rolls * Math.max(1, eligibleRewardPlayers(f).size()) : 0));

        if (mode == ArenaEnums.LootDelivery.DROP) {
            for (ItemStack it : items) {
                if (it == null || it.getType().isAir()) continue;
                at.getWorld().dropItemNaturally(at, it.clone());
            }
            return;
        }

        if (mode == ArenaEnums.LootDelivery.PER_PLAYER) {
            for (UUID id : eligibleRewardPlayers(f)) {
                Player p = Bukkit.getPlayer(id);
                if (p == null) continue;
                for (ItemStack it : items) {
                    if (it == null || it.getType().isAir()) continue;
                    var left = p.getInventory().addItem(it.clone());
                    for (ItemStack rest : left.values()) {
                        p.getWorld().dropItemNaturally(p.getLocation(), rest);
                    }
                }
            }
            return;
        }

        // CHEST
        Block b = at.getBlock();
        if (!b.getType().isAir()) b = at.clone().add(0, 1, 0).getBlock();
        b.setType(Material.CHEST);
        if (!(b.getState() instanceof Chest chest)) return;

        for (ItemStack it : items) {
            if (it == null || it.getType().isAir()) continue;
            chest.getBlockInventory().addItem(it.clone());
        }
        chest.update();

        // lock to eligible
        long lockSeconds = plugin.getConfig().getLong("loot.chest.lock-seconds", 30);
        lootLocks.put(f.arena.id.toLowerCase(Locale.ROOT), new LootChestLock(b.getLocation(), System.currentTimeMillis() + lockSeconds*1000L, eligibleRewardPlayers(f)));

        if (plugin.getConfig().getBoolean("loot.chest.announce", true)) {
            Bukkit.broadcast(Component.text("Loot chest spawned in " + f.arena.displayName));
        }
    }

    private Set<UUID> eligibleRewardPlayers(Fight f) {
        // Eligibility based on config + arena overrides
        String rule = f.arena.rewardEligibility;
        if (rule == null || rule.isBlank()) rule = plugin.getConfig().getString("rewards.eligibility", "DAMAGE_OR_TIME");

        double minDamage = f.arena.rewardMinDamage;
        int minTime = f.arena.rewardMinTimeSeconds;

        Set<UUID> out = new HashSet<>();

        for (UUID id : f.participants) {
            double dmg = f.damageDone.getOrDefault(id, 0.0);
            long timeMs = f.timeInsideMillis.getOrDefault(id, 0L);
            boolean ok;
            switch (rule.toUpperCase(Locale.ROOT)) {
                case "ALL_PRESENT" -> ok = true;
                case "DAMAGE_ONLY" -> ok = dmg >= minDamage;
                case "TIME_ONLY" -> ok = timeMs >= minTime * 1000L;
                default -> ok = (dmg >= minDamage) || (timeMs >= minTime * 1000L);
            }
            if (ok) out.add(id);
        }

        // fallback: if empty, allow all participants
        if (out.isEmpty()) out.addAll(f.participants);
        return out;
    }

    public LootChestLock getLootLock(String arenaId) {
        return lootLocks.get(arenaId.toLowerCase(Locale.ROOT));
    }

    public static class LootChestLock {
        public final Location chestLoc;
        public final long unlockAtMillis;
        public final Set<UUID> allowed;
        public LootChestLock(Location loc, long unlockAtMillis, Set<UUID> allowed) {
            this.chestLoc = loc;
            this.unlockAtMillis = unlockAtMillis;
            this.allowed = allowed;
        }
    }


    private List<ItemStack> resolveLootItems(Fight f, int rolls) {
        // if arena has lootProfile and it exists, roll from it
        String profName = (f.arena.lootProfile == null) ? "" : f.arena.lootProfile.trim();
        if (!profName.isEmpty()) {
            LootProfile prof = lootProfileManager.get(profName);
            if (prof != null && !prof.items.isEmpty()) {
                List<ItemStack> out = new ArrayList<>();
                Random rng = new Random();
                for (int i=0;i<rolls;i++) {
                    ItemStack pick = prof.items.get(rng.nextInt(prof.items.size()));
                    if (pick != null && !pick.getType().isAir()) out.add(pick.clone());
                }
                return out;
            }
        }
        // fallback to boss-type raw list
        List<ItemStack> raw = lootManager.getLootRaw(f.boss.type());
        if (rolls <= 0) return raw;
        // if rolls smaller than list, pick first N
        List<ItemStack> out = new ArrayList<>();
        for (int i=0;i<Math.min(rolls, raw.size());i++) out.add(raw.get(i).clone());
        return out.isEmpty() ? raw : out;
    }

}
