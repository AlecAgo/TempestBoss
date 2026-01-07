package me.alec.tempestboss.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class GuiUtil {

    public static ItemStack button(Material mat, String name, String lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Component.text(name).color(NamedTextColor.YELLOW));
        if (lore != null && !lore.isBlank()) {
            meta.lore(List.of(Component.text(lore).color(NamedTextColor.GRAY)));
        }
        it.setItemMeta(meta);
        return it;
    }
}
