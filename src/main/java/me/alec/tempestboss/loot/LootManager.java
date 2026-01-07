package me.alec.tempestboss.loot;

import me.alec.tempestboss.TempestBossPlugin;
import me.alec.tempestboss.boss.BossType;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class LootManager {

    private final TempestBossPlugin plugin;
    private final File file;
    private FileConfiguration cfg;

    public LootManager(TempestBossPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "loot.yml");
        reload();
    }

    public void reload() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        this.cfg = YamlConfiguration.loadConfiguration(file);
        for (BossType t : BossType.values()) {
            cfg.addDefault("loot." + t.name(), new ArrayList<>());
        }
        cfg.options().copyDefaults(true);
        save();
    }

    public void save() {
        try { cfg.save(file); }
        catch (IOException e) { plugin.getLogger().warning("Failed to save loot.yml: " + e.getMessage()); }
    }

    @SuppressWarnings("unchecked")
    public List<ItemStack> getLootRaw(BossType type) {
        List<?> raw = cfg.getList("loot." + type.name());
        if (raw == null) return new ArrayList<>();
        List<ItemStack> out = new ArrayList<>();
        for (Object o : raw) if (o instanceof ItemStack it) out.add(it);
        return out;
    }

    public void setLoot(BossType type, List<ItemStack> items) {
        cfg.set("loot." + type.name(), items);
        save();
    }
}
