package com.MaxAkt.disenchantmenttotem;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Lectern;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TotemManager {

    private final DisenchantmentTotem plugin;
    private final ConfigManager config;
    private final List<Totem> activeTotems = new ArrayList<>();
    private final Map<UUID, Map<String, StoredItemData>> playerStoredData = new ConcurrentHashMap<>();
    private final File totemsFile;
    private final File playerDataFile;
    private final Random random = new Random();


    public TotemManager(DisenchantmentTotem plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.config = configManager;
        this.totemsFile = new File(plugin.getDataFolder(), "totems.yml");
        this.playerDataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        startTotemEffectsTask();
    }

    // --- Totem Creation and Validation ---
    public void tryCreateTotem(Location crystalLocation) {
        List<Location> structureBlocks = new ArrayList<>();
        List<Location> lecternLocations = new ArrayList<>();

        if (!checkBlock(crystalLocation, 0, -1, 0, Material.OBSIDIAN, structureBlocks)) return;
        if (!checkBlock(crystalLocation, 0, -2, 0, Material.BOOKSHELF, structureBlocks)) return;
        if (!checkBlock(crystalLocation, 1, -2, 0, Material.END_ROD, structureBlocks)) return;
        if (!checkBlock(crystalLocation, -1, -2, 0, Material.END_ROD, structureBlocks)) return;
        if (!checkBlock(crystalLocation, 0, -2, 1, Material.END_ROD, structureBlocks)) return;
        if (!checkBlock(crystalLocation, 0, -2, -1, Material.END_ROD, structureBlocks)) return;
        if (!checkBlock(crystalLocation, 0, -3, 0, Material.BOOKSHELF, structureBlocks)) return;
        // Lecterns
        if (!checkBlock(crystalLocation, 1, -3, 0, Material.LECTERN, lecternLocations)) return;
        if (!checkBlock(crystalLocation, -1, -3, 0, Material.LECTERN, lecternLocations)) return;
        if (!checkBlock(crystalLocation, 0, -3, 1, Material.LECTERN, lecternLocations)) return;
        if (!checkBlock(crystalLocation, 0, -3, -1, Material.LECTERN, lecternLocations)) return;
        structureBlocks.addAll(lecternLocations);

        EnderCrystal foundCrystal = findCrystal(crystalLocation);
        if (foundCrystal == null) {
            plugin.getLogger().warning("Totem structure is correct, but no EnderCrystal entity was found.");
            return;
        }

        Totem newTotem = new Totem(crystalLocation.clone().add(0.5, 0.5, 0.5), foundCrystal, structureBlocks, lecternLocations);
        activeTotems.add(newTotem);
        plugin.getLogger().info("Disenchantment Totem has been created!");
        crystalLocation.getWorld().playSound(crystalLocation, Sound.BLOCK_BEACON_ACTIVATE, 1.5F, 0.8F);

        for (Player player : Bukkit.getOnlinePlayers()) {
            handlePlayerMovement(player);
        }
    }

    private boolean checkBlock(Location center, int xOff, int yOff, int zOff, Material expectedType, List<Location> blockList) {
        Location loc = center.clone().add(xOff, yOff, zOff);
        if (loc.getBlock().getType() == expectedType) {
            blockList.add(loc);
            return true;
        }
        return false;
    }

    private EnderCrystal findCrystal(Location location) {
        for (Entity entity : location.getWorld().getNearbyEntities(location, 1, 1, 1)) {
            if (entity instanceof EnderCrystal) {
                return (EnderCrystal) entity;
            }
        }
        return null;
    }

    // --- Totem Destruction ---
    public void destroyTotem(Location location, boolean isCrystal) {
        activeTotems.removeIf(totem -> {
            boolean shouldRemove = false;
            if (isCrystal && totem.crystal().getLocation().getBlock().getLocation().equals(location.getBlock().getLocation())) {
                shouldRemove = true;
            } else if (!isCrystal && totem.blocks().stream().anyMatch(l -> l.getBlock().getLocation().equals(location.getBlock().getLocation()))) {
                shouldRemove = true;
            }

            if (shouldRemove) {
                if (!totem.crystal().isDead()) {
                    totem.crystal().remove();
                }

                Location totemCenter = totem.center();
                totemCenter.getWorld().spawnParticle(Particle.SMOKE_LARGE, totemCenter, 50, 0.5, 0.5, 0.5);
                totemCenter.getWorld().playSound(totemCenter, Sound.ENTITY_GENERIC_EXPLODE, 0.7F, 1.2F);
                totemCenter.getWorld().playSound(totemCenter, Sound.BLOCK_BEACON_DEACTIVATE, 1.5F, 0.8F);
                plugin.getLogger().info("A Disenchantment Totem was destroyed.");

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            handlePlayerMovement(p);
                        }
                    }
                }.runTaskLater(plugin, 1L);
            }
            return shouldRemove;
        });
    }

    // --- Player Enchantment Logic ---
    public void handlePlayerMovement(Player player) {
        boolean isInside = isPlayerInAnyTotemRadius(player);
        if (isInside) {
            disenchantPlayer(player);
        } else {
            enchantPlayer(player);
        }
    }

    public void disenchantPlayer(Player player) {
        Map<String, StoredItemData> originalData = playerStoredData.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>());
        boolean changed = false;

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null) continue;

            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;

            boolean hasEnchants = meta.hasEnchants();
            boolean isEnchantedGapple = item.getType() == Material.ENCHANTED_GOLDEN_APPLE;

            if (hasEnchants || (isEnchantedGapple && config.convertEnchantedGoldenApples)) {
                if (meta.getPersistentDataContainer().has(plugin.itemDataKey, PersistentDataType.STRING)) {
                    continue;
                }

                String uniqueID = UUID.randomUUID().toString();
                meta.getPersistentDataContainer().set(plugin.itemDataKey, PersistentDataType.STRING, uniqueID);
                originalData.put(uniqueID, new StoredItemData(item.getType(), new ConcurrentHashMap<>(item.getEnchantments())));

                if (hasEnchants) {
                    for (Enchantment enchantment : new ArrayList<>(meta.getEnchants().keySet())) {
                        meta.removeEnchant(enchantment);
                    }
                }
                item.setItemMeta(meta);

                if (isEnchantedGapple && config.convertEnchantedGoldenApples) {
                    item.setType(Material.GOLDEN_APPLE);
                }

                player.getInventory().setItem(i, item);
                changed = true;
            }
        }

        if (changed) {
            player.playSound(player.getLocation(), Sound.BLOCK_REDSTONE_TORCH_BURNOUT, 1.0F, 1.0F);
            if (config.showEnterExitMessages) {
                player.sendMessage(ChatColor.GRAY + "You feel the enchantments on your gear fade away...");
            }
        }
    }

    public void enchantPlayer(Player player) {
        Map<String, StoredItemData> storedData = playerStoredData.get(player.getUniqueId());
        if (storedData == null || storedData.isEmpty()) return;

        boolean changed = false;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (enchantSingleItem(item, player.getUniqueId())) {
                player.getInventory().setItem(i, item);
                changed = true;
            }
        }

        if (changed) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7F, 1.5F);
            if (config.showEnterExitMessages) {
                player.sendMessage(ChatColor.AQUA + "You feel the enchantments on your gear return!");
            }
        }
        if (storedData.isEmpty()) {
            playerStoredData.remove(player.getUniqueId());
        }
    }

    public boolean enchantSingleItem(ItemStack item, UUID playerUUID) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.getPersistentDataContainer().has(plugin.itemDataKey, PersistentDataType.STRING)) return false;

        String uniqueID = meta.getPersistentDataContainer().get(plugin.itemDataKey, PersistentDataType.STRING);
        Map<String, StoredItemData> storedPlayerData = playerStoredData.get(playerUUID);

        if (storedPlayerData != null && storedPlayerData.containsKey(uniqueID)) {
            StoredItemData data = storedPlayerData.get(uniqueID);
            if (data != null) {
                item.setType(data.originalMaterial());

                if (!data.enchantments().isEmpty()) {
                    for (Map.Entry<Enchantment, Integer> entry : data.enchantments().entrySet()) {
                        meta.addEnchant(entry.getKey(), entry.getValue(), true);
                    }
                }

                meta.getPersistentDataContainer().remove(plugin.itemDataKey);
                item.setItemMeta(meta);

                storedPlayerData.remove(uniqueID);
                return true;
            }
        }
        return false;
    }

    // --- Utility and State Checks ---
    public boolean isPlayerInAnyTotemRadius(Player player) {
        for (Totem totem : activeTotems) {
            if (totem.center().getWorld().equals(player.getWorld())) {
                int radius = totem.getCurrentRadius(config.defaultRadius, config.radiusPerBook);
                if (totem.center().distance(player.getLocation()) <= radius) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if a given location is part of any active totem's structure.
     * This method is PUBLIC because it's used by the TotemListener.
     * @param location The location to check.
     * @return True if the location is part of a totem, false otherwise.
     */
    public boolean isLocationPartOfATotem(Location location) {
        for (Totem totem : activeTotems) {
            if (totem.crystal().getLocation().getBlock().getLocation().equals(location.getBlock().getLocation()))
                return true;
            for (Location blockLoc : totem.blocks()) {
                if (blockLoc.getBlock().getLocation().equals(location.getBlock().getLocation())) return true;
            }
        }
        return false;
    }

    // --- Particle Effects & Sounds ---
    private void startTotemEffectsTask() {
        new BukkitRunnable() {
            private int tickCounter = 0;

            @Override
            public void run() {
                if (activeTotems.isEmpty()) return;

                for (Totem totem : new ArrayList<>(activeTotems)) {
                    Location center = totem.center();
                    World world = center.getWorld();

                    if (world == null || totem.crystal().isDead()) {
                        destroyTotem(totem.center(), false);
                        continue;
                    }

                    int radius = totem.getCurrentRadius(config.defaultRadius, config.radiusPerBook);

                    if (config.floorParticlesEnabled) {
                        int floorParticles = 300 + (totem.countBooks() * 150);
                        for (int i = 0; i < floorParticles; i++) {
                            double theta = 2 * Math.PI * random.nextDouble();
                            double distance = radius * Math.sqrt(random.nextDouble());
                            double x = center.getX() + distance * Math.cos(theta);
                            double z = center.getZ() + distance * Math.sin(theta);
                            int blockX = (int) Math.floor(x);
                            int blockZ = (int) Math.floor(z);
                            double groundY = world.getHighestBlockYAt(blockX, blockZ);
                            world.spawnParticle(Particle.ENCHANTMENT_TABLE, x, groundY + 1.5, z, 0, 0, 0, 0, 0.1);
                        }
                    }

                    if (config.bookParticlesEnabled) {
                        for (Location lecternLoc : totem.lecternLocations()) {
                            Block block = lecternLoc.getBlock();
                            if (block.getState() instanceof Lectern lecternState && lecternState.getInventory().getItem(0) != null) {
                                Location spawnPoint = lecternLoc.clone().add(0.5, 1.2, 0.5);
                                Vector direction = center.toVector().subtract(spawnPoint.toVector());
                                direction.normalize().multiply(0.05);
                                for (int j = 0; j < 3; j++) {
                                    world.spawnParticle(Particle.REVERSE_PORTAL, spawnPoint, 0, direction.getX(), direction.getY(), direction.getZ(), 1);
                                }
                            }
                        }
                    }

                    if (config.suckingParticlesEnabled) {
                        for (int i = 0; i < 10; i++) {
                            Vector offset = Vector.getRandom().subtract(new Vector(0.5, 0.5, 0.5)).normalize().multiply(Math.random() * 2 + 0);
                            Location spawnPoint = center.clone().add(offset);
                            Vector velocity = center.toVector().subtract(spawnPoint.toVector()).normalize().multiply(0.3);
                            world.spawnParticle(Particle.CRIT_MAGIC, spawnPoint, 0, velocity.getX(), velocity.getY(), velocity.getZ(), 1);
                        }
                    }

                    if (config.idleSoundEnabled && tickCounter % 20 == 0) {
                        for (Player player : world.getPlayers()) {
                            if (player.getLocation().distance(center) <= 12) {
                                player.playSound(center, Sound.BLOCK_CONDUIT_AMBIENT_SHORT, 0.4F, 0.7F);
                            }
                        }
                    }
                }
                tickCounter++;
            }
        }.runTaskTimer(plugin, 0, 2L);
    }

    // --- Data Persistence ---
    public void saveTotems() throws IOException {
        FileConfiguration config = new YamlConfiguration();
        List<Map<String, Object>> totemList = new ArrayList<>();
        for (Totem totem : activeTotems) {
            totemList.add(totem.center().serialize());
        }
        config.set("totems", totemList);
        config.save(totemsFile);
    }

    @SuppressWarnings("unchecked")
    public void loadTotems() throws IOException {
        if (!totemsFile.exists()) return;
        FileConfiguration config = YamlConfiguration.loadConfiguration(totemsFile);
        List<Map<?, ?>> totemList = config.getMapList("totems");
        for (Map<?, ?> map : totemList) {
            Location center = Location.deserialize((Map<String, Object>) map);
            tryCreateTotem(center.getBlock().getLocation());
        }
    }

    public void savePlayerEnchants() throws IOException {
        FileConfiguration dataConfig = new YamlConfiguration();
        for (Map.Entry<UUID, Map<String, StoredItemData>> playerEntry : playerStoredData.entrySet()) {
            String uuid = playerEntry.getKey().toString();
            for (Map.Entry<String, StoredItemData> itemEntry : playerEntry.getValue().entrySet()) {
                String itemID = itemEntry.getKey();
                StoredItemData data = itemEntry.getValue();
                String basePath = uuid + "." + itemID;

                dataConfig.set(basePath + ".material", data.originalMaterial().name());
                for (Map.Entry<Enchantment, Integer> enchantEntry : data.enchantments().entrySet()) {
                    String enchantPath = basePath + ".enchantments." + enchantEntry.getKey().getKey();
                    dataConfig.set(enchantPath, enchantEntry.getValue());
                }
            }
        }
        dataConfig.save(playerDataFile);
    }

    public void loadPlayerEnchants() throws IOException {
        if (!playerDataFile.exists()) return;
        FileConfiguration dataConfig = YamlConfiguration.loadConfiguration(playerDataFile);
        playerStoredData.clear();

        for (String uuidString : dataConfig.getKeys(false)) {
            UUID uuid = UUID.fromString(uuidString);
            Map<String, StoredItemData> itemMap = new ConcurrentHashMap<>();
            ConfigurationSection playerSection = dataConfig.getConfigurationSection(uuidString);
            if (playerSection == null) continue;

            for (String itemID : playerSection.getKeys(false)) {
                ConfigurationSection itemSection = playerSection.getConfigurationSection(itemID);
                if (itemSection == null) continue;

                Material material = Material.getMaterial(itemSection.getString("material", "STONE"));
                Map<Enchantment, Integer> enchantMap = new ConcurrentHashMap<>();
                ConfigurationSection enchantSection = itemSection.getConfigurationSection("enchantments");
                if (enchantSection != null) {
                    for (String enchantKeyString : enchantSection.getKeys(false)) {
                        NamespacedKey key = NamespacedKey.fromString(enchantKeyString);
                        if (key != null) {
                            Enchantment enchantment = Enchantment.getByKey(key);
                            if (enchantment != null) {
                                enchantMap.put(enchantment, enchantSection.getInt(enchantKeyString));
                            }
                        }
                    }
                }
                itemMap.put(itemID, new StoredItemData(material, enchantMap));
            }
            playerStoredData.put(uuid, itemMap);
        }
    }
}