package com.MaxAkt.disenchantmenttotem;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public final class DisenchantmentTotem extends JavaPlugin {

    private TotemManager totemManager;
    public NamespacedKey itemDataKey;

    @Override
    public void onEnable() {
        // --- Unique Key for Item Data ---
        this.itemDataKey = new NamespacedKey(this, "disenchanted_item_id");

        // --- Configuration ---
        saveDefaultConfig();
        // Add ConfigManager
        ConfigManager configManager = new ConfigManager(this); // Initialize ConfigManager

        // --- Manager and Listener Setup ---
        this.totemManager = new TotemManager(this, configManager); // Pass configManager
        getServer().getPluginManager().registerEvents(new TotemListener(this, totemManager), this);

        // --- Load Data ---
        try {
            totemManager.loadTotems();
            totemManager.loadPlayerEnchants();
        } catch (IOException e) {
            getLogger().severe("Failed to load totem or player data!");
            e.printStackTrace();
        }

        getLogger().info("DisenchantmentTotem has been enabled!");
    }

    @Override
    public void onDisable() {
        // --- Save Data ---
        try {
            if (totemManager != null) {
                getServer().getOnlinePlayers().forEach(totemManager::enchantPlayer);
                totemManager.saveTotems();
                totemManager.savePlayerEnchants();
            }
        } catch (IOException e) {
            getLogger().severe("Failed to save totem or player data!");
            e.printStackTrace();
        }
        getLogger().info("DisenchantmentTotem has been disabled!");
    }
}