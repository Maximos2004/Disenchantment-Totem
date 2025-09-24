package com.MaxAkt.disenchantmenttotem;

import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    // --- Totem Settings ---
    public final int defaultRadius;
    public final int radiusPerBook;
    public final boolean convertEnchantedGoldenApples;
    public final boolean preventEnchantingInZone;

    // --- Effect Toggles ---
    public final boolean idleSoundEnabled;
    public final boolean floorParticlesEnabled;
    public final boolean bookParticlesEnabled;
    public final boolean suckingParticlesEnabled;

    // --- Message Toggles ---
    public final boolean showEnterExitMessages;


    public ConfigManager(DisenchantmentTotem plugin) {
        FileConfiguration config = plugin.getConfig();

        // --- Load Totem Settings ---
        this.defaultRadius = config.getInt("totem-settings.default-radius", 30);
        this.radiusPerBook = config.getInt("totem-settings.radius-per-book", 5);
        this.convertEnchantedGoldenApples = config.getBoolean("totem-settings.convert-enchanted-golden-apples", true);
        this.preventEnchantingInZone = config.getBoolean("totem-settings.prevent-enchanting-in-zone", true);

        // --- Load Effect Toggles ---
        this.idleSoundEnabled = config.getBoolean("effects.idle-sound-enabled", true);
        this.floorParticlesEnabled = config.getBoolean("effects.floor-particles-enabled", true);
        this.bookParticlesEnabled = config.getBoolean("effects.book-particles-enabled", true);
        this.suckingParticlesEnabled = config.getBoolean("effects.sucking-particles-enabled", true);

        // --- Load Message Toggles ---
        this.showEnterExitMessages = config.getBoolean("messages.show-enter-exit-messages", true);
    }
}