package me.alec.tempestboss.loot.profile;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class LootProfile {
    public final String name;
    public int rollsPerPlayer = 2;
    public final List<ItemStack> items = new ArrayList<>();

    public LootProfile(String name) {
        this.name = name;
    }
}
