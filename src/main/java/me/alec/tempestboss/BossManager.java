package me.alec.tempestboss;

import me.alec.tempestboss.boss.*;
import me.alec.tempestboss.cinematic.CinematicController;
import me.alec.tempestboss.loot.LootManager;
import me.alec.tempestboss.util.MusicUtil;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public class BossManager implements Listener {

    private final TempestBossPlugin plugin;
    private final LootManager lootManager;
    private final CinematicController cinematic;

    private BossDefinition active;
    private BossBar bossBar;

    private BukkitTask tickTask;
    private BukkitTask musicTask;

    private boolean introDone = false;
    private boolean transitioning = false;
    private int lastPhase = 1;

    private final Map<UUID, BossType> selectedType = new HashMap<>();
    private final Set<UUID> musicOff = new HashSet<>();

    // Soft boundary warnings
    private final Map<UUID, Long> lastWarnMillis = new HashMap<>();

    // Loot chest lock
    private Location lootChestLoc;
    private long lootUnlockAtMillis;
    private Set<UUID> lootAllowed = Set.of();

    public BossManager(TempestBossPlugin plugin, LootManager lootManager) {
        this.plugin = plugin;
        this.lootManager = lootManager;
        this.cinematic = new CinematicController(plugin);
        PluginManager pm = plugin.getServer().getPluginManager();
        pm.registerEvents(this, plugin);
    }

    public BossType getSelectedBossType(Player p) {
        return selectedType.getOrDefault(p.getUniqueId(), BossType.TEMPEST);
    }

    public void setSelectedBossType(Player p, BossType t) {
        selectedType.put(p.getUniqueId(), t);
    }

    public void togglePersonalMusic(Player p) {
        if (musicOff.contains(p.getUniqueId())) {
            musicOff.remove(p.getUniqueId());
            p.sendMessage("§aBoss music: ON");
        } else {
            musicOff.add(p.getUniqueId());
            p.sendMessage("§cBoss music: OFF");
        }
    }

    public boolean isActive() {
        return active != null;
    }

    public void spawnBoss(BossType type, Location at, String difficulty, Player spawner) {
        if (isActive()) {
            spawner.sendMessage("§cA boss is already active.");
            return;
        }

        this.introDone = false;
        this.transitioning = false;
        this.lastPhase = 1;
        this.lootChestLoc = null;
        this.lootAllowed = Set.of();

        BossDefinition def = switch (type) {
            case ABYSSAL_LICH -> new AbyssalLichBoss(plugin, this, at, difficulty);
            default -> new TempestHeraldBoss(plugin, this, at, difficulty);
        };
        this.active = def;

        this.bossBar = BossBar.bossBar(
                Component.text(type == BossType.TEMPEST ? "Tempest Herald" : "Abyssal Lich").color(NamedTextColor.AQUA),
                1.0f,
                BossBar.Color.PURPLE,
                BossBar.Overlay.PROGRESS
        );

        startTickLoop();
        startMusicLoop();

        spawner.sendMessage("§bBoss summoned: §f" + type.name());
    }

    public void despawnBoss(boolean silent) {
        stopTasks();
        if (bossBar != null) {
            for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(bossBar);
        }
        if (active != null) active.onDeath();
        active = null;
        bossBar = null;
        transitioning = false;
        introDone = false;
        if (!silent) Bukkit.broadcast(Component.text("The boss fight ends."));
    }

    public void shutdown() {
        despawnBoss(true);
    }

    private void stopTasks() {
        if (tickTask != null) tickTask.cancel();
        if (musicTask != null) musicTask.cancel();
        tickTask = null;
        musicTask = null;
    }

    private double arenaRadius() {
        return plugin.getConfig().getDouble("boss.defaults.arena-radius", 45.0);
    }

    public List<Player> playersInArena() {
        if (active == null || active.getCenter() == null || active.getCenter().getWorld() == null) return List.of();
        Location c = active.getCenter();
        double r2 = arenaRadius() * arenaRadius();
        List<Player> out = new ArrayList<>();
        for (Player p : c.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(c) <= r2) out.add(p);
        }
        return out;
    }

    private void startTickLoop() {
        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (active == null || active.getEntity() == null || active.getEntity().isDead()) {
                    cancel();
                    return;
                }

                // soft boundary
                softBoundary();

                // Update bossbar visibility
                Location c = active.getCenter();
                double rad2 = arenaRadius() * arenaRadius();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    boolean near = c != null && p.getWorld().equals(c.getWorld()) && p.getLocation().distanceSquared(c) <= rad2;
                    if (near) p.showBossBar(bossBar);
                    else p.hideBossBar(bossBar);
                }

                // intro cinematic once
                if (!introDone && plugin.getConfig().getBoolean("cinematic.enabled", true)) {
                    introDone = true;
                    transitioning = true;
                    List<Player> arena = playersInArena();
                    cinematic.intro(active.getCenter(), active.getEntity(), arena, () -> {
                        transitioning = false;
                    });
                }

                // phase transitions
                int phase = active.getPhase();
                if (!transitioning && phase != lastPhase) {
                    transitioning = true;
                    int newPhase = phase;
                    lastPhase = phase;
                    active.onPhaseChanged(newPhase);
                    List<Player> arena = playersInArena();
                    cinematic.phaseTransition(active.getCenter(), active.getEntity(), arena, newPhase, () -> {
                        transitioning = false;
                    });
                }

                // Tick boss only when not in cinematic transition
                if (!transitioning) {
                    active.tick();
                }

                // Update boss bar progress
                double hp = active.getHealth();
                double max = active.getMaxHealth();
                float prog = (float) Math.max(0.0, Math.min(1.0, hp / max));
                bossBar.progress(prog);
            }
        }.runTaskTimer(plugin, 1L, 2L);
    }

    private void startMusicLoop() {
        if (!plugin.getConfig().getBoolean("music.enabled", true)) return;

        long period = plugin.getConfig().getLong("music.restart-every-seconds", 90) * 20L;
        float vol = (float) plugin.getConfig().getDouble("music.volume", 0.9);
        float pitch = (float) plugin.getConfig().getDouble("music.pitch", 1.0);

        musicTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (active == null) return;
                List<Player> arena = playersInArena();

                Material disc = getBossMusicDisc(active.type());
                Sound s = MusicUtil.discToSound(disc, vol, pitch);

                for (Player p : arena) {
                    if (musicOff.contains(p.getUniqueId())) continue;
                    p.playSound(s);
                }
            }
        }.runTaskTimer(plugin, 20L, period);
    }

    private Material getBossMusicDisc(BossType type) {
        String path = "bosses." + type.name() + ".music-disc";
        String discName = plugin.getConfig().getString(path, type == BossType.TEMPEST ? "MUSIC_DISC_PIGSTEP" : "MUSIC_DISC_OTHERSIDE");
        try {
            Material m = Material.valueOf(discName);
            if (m.name().startsWith("MUSIC_DISC_")) return m;
        } catch (Exception ignored) {}
        return Material.MUSIC_DISC_13;
    }

    private void softBoundary() {
        if (active == null || active.getCenter() == null || active.getCenter().getWorld() == null) return;
        if (!plugin.getConfig().getBoolean("boss.soft-boundary.enabled", true)) return;

        Location c = active.getCenter();
        double r = arenaRadius();
        double buffer = plugin.getConfig().getDouble("boss.soft-boundary.buffer", 1.0);
        double push = plugin.getConfig().getDouble("boss.soft-boundary.pushback-strength", 1.15);
        long warnEvery = plugin.getConfig().getLong("boss.soft-boundary.warning-every-seconds", 2) * 1000L;

        for (Player p : c.getWorld().getPlayers()) {
            double d = p.getLocation().distance(c);
            if (d > r + buffer) {
                Vector v = c.toVector().subtract(p.getLocation().toVector()).normalize().multiply(push);
                v.setY(0.25);
                p.setVelocity(v);

                long now = System.currentTimeMillis();
                long last = lastWarnMillis.getOrDefault(p.getUniqueId(), 0L);
                if (now - last >= warnEvery) {
                    lastWarnMillis.put(p.getUniqueId(), now);
                    p.sendActionBar(Component.text("Return to the arena!").color(NamedTextColor.RED));
                    p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.MASTER, 0.5f, 0.8f);
                }
            }
        }
    }

    public void dropLootChest(Location at, BossType type, Collection<UUID> participants) {
        if (at == null || at.getWorld() == null) return;
        if (!plugin.getConfig().getBoolean("loot.chest.enabled", true)) return;

        Block b = at.getBlock();
        if (!b.getType().isAir()) b = at.clone().add(0, 1, 0).getBlock();

        b.setType(Material.CHEST);
        if (!(b.getState() instanceof Chest chest)) return;

        List<ItemStack> items = lootManager.getLoot(type);
        for (ItemStack it : items) {
            if (it == null || it.getType().isAir()) continue;
            chest.getBlockInventory().addItem(it);
        }
        chest.update();

        lootChestLoc = b.getLocation();
        long lockSec = plugin.getConfig().getLong("loot.chest.lock-seconds", 30);
        lootUnlockAtMillis = System.currentTimeMillis() + lockSec * 1000L;
        lootAllowed = new HashSet<>(participants);

        if (plugin.getConfig().getBoolean("loot.chest.announce", true)) {
            Bukkit.broadcast(Component.text("§6Loot chest spawned!"));
        }
    }

    @EventHandler
    public void onChestOpen(PlayerInteractEvent e) {
        if (lootChestLoc == null) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getClickedBlock() == null) return;
        if (e.getClickedBlock().getType() != Material.CHEST) return;

        Location l = e.getClickedBlock().getLocation();
        if (!l.equals(lootChestLoc)) return;

        if (System.currentTimeMillis() < lootUnlockAtMillis) {
            UUID id = e.getPlayer().getUniqueId();
            if (!lootAllowed.contains(id)) {
                e.setCancelled(true);
                e.getPlayer().sendMessage("§cLoot is locked for participants for a moment.");
            }
        }
    }

    @EventHandler
    public void onBossDeath(EntityDeathEvent e) {
        if (active == null || active.getEntity() == null) return;
        if (!e.getEntity().getUniqueId().equals(active.getEntity().getUniqueId())) return;

        Location l = e.getEntity().getLocation();
        BossType t = active.type();
        List<UUID> participants = active.getParticipants();

        Bukkit.getScheduler().runTask(plugin, () -> {
            dropLootChest(l, t, participants);
            despawnBoss(true);
        });
    }

    @EventHandler
    public void onBossHit(EntityDamageByEntityEvent e) {
        if (active == null || active.getEntity() == null) return;
        if (!e.getEntity().getUniqueId().equals(active.getEntity().getUniqueId())) return;
        active.onDamage();
    }
}
