package me.alec.tempestboss.arena;

import me.alec.tempestboss.TempestBossPlugin;
import me.alec.tempestboss.boss.BossType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ArenaManager {

    private final TempestBossPlugin plugin;
    private final File file;
    private FileConfiguration cfg;

    private final Map<String, Arena> arenas = new LinkedHashMap<>();

    public ArenaManager(TempestBossPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "arenas.yml");
        reload();
    }

    public Collection<Arena> all() { return arenas.values(); }

    public Arena get(String id) { return arenas.get(id.toLowerCase(Locale.ROOT)); }

    public Arena create(String id) {
        String key = id.toLowerCase(Locale.ROOT);
        Arena a = new Arena(id);
        arenas.put(key, a);
        save();
        return a;
    }

    public void delete(String id) {
        arenas.remove(id.toLowerCase(Locale.ROOT));
        save();
    }

    public void reload() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        cfg = YamlConfiguration.loadConfiguration(file);
        arenas.clear();

        ConfigurationSection root = cfg.getConfigurationSection("arenas");
        if (root == null) return;

        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;

            Arena a = new Arena(id);
            a.displayName = s.getString("displayName", id);
            a.enabled = s.getBoolean("enabled", true);
            a.trigger = ArenaEnums.TriggerMode.valueOf(s.getString("trigger", "MANUAL"));
            a.bossType = BossType.valueOf(s.getString("bossType", "TEMPEST"));
            a.difficulty = s.getString("difficulty", "NORMAL").toUpperCase(Locale.ROOT);
            a.minPlayers = Math.max(1, s.getInt("minPlayers", 1));
            a.countdownSeconds = Math.max(0, s.getInt("countdownSeconds", plugin.getConfig().getInt("arenas.countdown-seconds-default", 5)));
            a.intro = ArenaEnums.IntroMode.valueOf(s.getString("intro", "INHERIT"));
            a.lootDelivery = ArenaEnums.LootDelivery.valueOf(s.getString("lootDelivery", "INHERIT"));
            a.lootProfile = s.getString("lootProfile", "");

            a.cooldownSeconds = Math.max(0, s.getLong("cooldownSeconds", 120));
            a.lastStartMillis = s.getLong("lastStartMillis", 0);

            a.protectionBreak = s.getBoolean("protection.block-break", false);
            a.protectionPlace = s.getBoolean("protection.block-place", false);
            a.protectionFluid = s.getBoolean("protection.fluid-spread", false);
            a.protectionExplosions = s.getBoolean("protection.explosions", false);

            a.rewardEligibility = s.getString("rewards.eligibility", "DAMAGE_OR_TIME");
            a.rewardMinDamage = s.getDouble("rewards.minDamage", 1.0);
            a.rewardMinTimeSeconds = s.getInt("rewards.minTimeSeconds", 10);

            String rtype = s.getString("region.type", "sphere");
            if (rtype.equalsIgnoreCase("sphere")) {
                Location c = parseLoc(s.getString("region.center"));
                double r = s.getDouble("region.radius", 45.0);
                if (c != null) a.region = new RegionSphere(c, r);
            } else {
                Location p1 = parseLoc(s.getString("region.pos1"));
                Location p2 = parseLoc(s.getString("region.pos2"));
                if (p1 != null && p2 != null) a.region = new RegionCuboid(p1, p2);
            }

            a.bossSpawn = parseLoc(s.getString("bossSpawn"));

            arenas.put(id.toLowerCase(Locale.ROOT), a);
        }
    }

    public void save() {
        cfg.set("arenas", null);
        for (Arena a : arenas.values()) {
            String path = "arenas." + a.id;
            cfg.set(path + ".displayName", a.displayName);
            cfg.set(path + ".enabled", a.enabled);
            cfg.set(path + ".trigger", a.trigger.name());
            cfg.set(path + ".bossType", a.bossType.name());
            cfg.set(path + ".difficulty", a.difficulty);
            cfg.set(path + ".minPlayers", a.minPlayers);
            cfg.set(path + ".countdownSeconds", a.countdownSeconds);
            cfg.set(path + ".intro", a.intro.name());
            cfg.set(path + ".lootDelivery", a.lootDelivery.name());
            cfg.set(path + ".lootProfile", a.lootProfile);
            cfg.set(path + ".cooldownSeconds", a.cooldownSeconds);
            cfg.set(path + ".lastStartMillis", a.lastStartMillis);

            cfg.set(path + ".protection.block-break", a.protectionBreak);
            cfg.set(path + ".protection.block-place", a.protectionPlace);
            cfg.set(path + ".protection.fluid-spread", a.protectionFluid);
            cfg.set(path + ".protection.explosions", a.protectionExplosions);

            cfg.set(path + ".rewards.eligibility", a.rewardEligibility);
            cfg.set(path + ".rewards.minDamage", a.rewardMinDamage);
            cfg.set(path + ".rewards.minTimeSeconds", a.rewardMinTimeSeconds);

            if (a.region instanceof RegionSphere s) {
                cfg.set(path + ".region.type", "sphere");
                cfg.set(path + ".region.center", formatLoc(s.getCenter()));
                cfg.set(path + ".region.radius", s.radius());
            } else if (a.region instanceof RegionCuboid c) {
                cfg.set(path + ".region.type", "cuboid");
                cfg.set(path + ".region.pos1", formatLoc(c.min()));
                cfg.set(path + ".region.pos2", formatLoc(c.max()));
            }

            if (a.bossSpawn != null) cfg.set(path + ".bossSpawn", formatLoc(a.bossSpawn));
        }

        try { cfg.save(file); }
        catch (IOException e) { plugin.getLogger().warning("Failed to save arenas.yml: " + e.getMessage()); }
    }

    public static String formatLoc(Location l) {
        if (l == null || l.getWorld() == null) return null;
        return l.getWorld().getName() + "," + l.getX() + "," + l.getY() + "," + l.getZ() + "," + l.getYaw() + "," + l.getPitch();
    }

    public static Location parseLoc(String s) {
        if (s == null || s.isBlank()) return null;
        String[] parts = s.split(",");
        if (parts.length < 4) return null;
        World w = Bukkit.getWorld(parts[0]);
        if (w == null) return null;
        double x = Double.parseDouble(parts[1]);
        double y = Double.parseDouble(parts[2]);
        double z = Double.parseDouble(parts[3]);
        float yaw = parts.length >= 5 ? Float.parseFloat(parts[4]) : 0f;
        float pitch = parts.length >= 6 ? Float.parseFloat(parts[5]) : 0f;
        return new Location(w, x, y, z, yaw, pitch);
    }
}
