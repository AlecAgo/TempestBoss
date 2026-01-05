package me.alec.tempestboss.util;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Material;

import java.util.Locale;

public class MusicUtil {

    /**
     * Converts a MUSIC_DISC_* material to the corresponding Minecraft sound key.
     * Example: MUSIC_DISC_PIGSTEP -> minecraft:music_disc.pigstep
     */
    public static Sound discToSound(Material disc, float volume, float pitch) {
        String name = disc.name();
        if (!name.startsWith("MUSIC_DISC_")) {
            // fallback
            return Sound.sound(Key.key("minecraft:music_disc.13"), Sound.Source.RECORD, volume, pitch);
        }
        String tail = name.substring("MUSIC_DISC_".length()).toLowerCase(Locale.ROOT);
        String key = "minecraft:music_disc." + tail;
        return Sound.sound(Key.key(key), Sound.Source.RECORD, volume, pitch);
    }
}
