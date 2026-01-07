package me.alec.tempestboss.util;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Material;

import java.util.Locale;

public class MusicUtil {

    public static Sound discToSound(Material disc, float volume, float pitch) {
        String name = disc.name();
        if (!name.startsWith("MUSIC_DISC_")) {
            return Sound.sound(Key.key("minecraft:music_disc.13"), Sound.Source.RECORD, volume, pitch);
        }
        String tail = name.substring("MUSIC_DISC_".length()).toLowerCase(Locale.ROOT);
        return Sound.sound(Key.key("minecraft:music_disc." + tail), Sound.Source.RECORD, volume, pitch);
    }
}
