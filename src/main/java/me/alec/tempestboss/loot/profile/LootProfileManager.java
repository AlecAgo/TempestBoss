package me.alec.tempestboss.loot.profile;

import me.alec.tempestboss.TempestBossPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class LootProfileManager {

    private final TempestBossPlugin plugin;
    private final File file;
    private FileConfiguration cfg;

    private final Map<String, LootProfile> profiles = new LinkedHashMap<>();

    public LootProfileManager(TempestBossPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "lootprofiles.yml");
        reload();
    }

    public Collection<LootProfile> all() {
        return profiles.values();
    }

    public LootProfile get(String name) {
        if (name == null) return null;
        return profiles.get(name.toLowerCase(Locale.ROOT));
    }

    public LootProfile create(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        LootProfile p = profiles.get(key);
        if (p != null) return p;
        p = new LootProfile(name);
        profiles.put(key, p);
        save();
        return p;
    }

    public void delete(String name) {
        if (name == null) return;
        profiles.remove(name.toLowerCase(Locale.ROOT));
        save();
    }

    public void reload() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        cfg = YamlConfiguration.loadConfiguration(file);
        profiles.clear();

        ConfigurationSection root = cfg.getConfigurationSection("profiles");
        if (root == null) {
            // create a default profile
            LootProfile def = new LootProfile("default");
            def.rollsPerPlayer = 2;
            profiles.put("default", def);
            save();
            return;
        }

        for (String name : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(name);
            if (s == null) continue;
            LootProfile p = new LootProfile(name);
            p.rollsPerPlayer = Math.max(0, s.getInt("rollsPerPlayer", 2));

            List<?> raw = s.getList("items");
            if (raw != null) {
                for (Object o : raw) {
                    if (o instanceof ItemStack it) p.items.add(it);
                }
            }

            profiles.put(name.toLowerCase(Locale.ROOT), p);
        }
    }

    public void save() {
        cfg.set("profiles", null);
        for (LootProfile p : profiles.values()) {
            String path = "profiles." + p.name;
            cfg.set(path + ".rollsPerPlayer", p.rollsPerPlayer);
            cfg.set(path + ".items", p.items);
        }
        try { cfg.save(file); }
        catch (IOException e) { plugin.getLogger().warning("Failed to save lootprofiles.yml: " + e.getMessage()); }
    }
}
