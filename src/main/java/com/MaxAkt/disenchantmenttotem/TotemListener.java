package com.MaxAkt.disenchantmenttotem;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Objects;

public record TotemListener(DisenchantmentTotem plugin, TotemManager totemManager) implements Listener {

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();

        // --- Totem Creation ---
        if (event.getItem() != null && event.getItem().getType() == Material.END_CRYSTAL && clickedBlock.getType() == Material.OBSIDIAN) {
            Location crystalLocation = clickedBlock.getLocation().add(0, 1, 0);
            if (totemManager.isLocationPartOfATotem(crystalLocation)) return;

            new BukkitRunnable() {
                @Override
                public void run() {
                    totemManager.tryCreateTotem(crystalLocation);
                }
            }.runTaskLater(plugin, 1L);
            return;
        }

        // --- Lectern Sound Feedback (Adding a book) ---
        if (clickedBlock.getType() == Material.LECTERN && player.getInventory().getItemInMainHand().getType().toString().endsWith("BOOK")) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.getWorld().playSound(clickedBlock.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 1.2f, 1.0f);
                }
            }.runTaskLater(plugin, 1L);
        }
    }


    @EventHandler
    public void onCrystalDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof EnderCrystal crystal)) return;
        totemManager.destroyTotem(crystal.getLocation(), true);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (totemManager.isLocationPartOfATotem(event.getBlock().getLocation())) {
            totemManager.destroyTotem(event.getBlock().getLocation(), false);
        }
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        handlePistonAction(event.getBlocks());
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        handlePistonAction(event.getBlocks());
    }

    private void handlePistonAction(List<Block> movedBlocks) {
        for (Block block : movedBlocks) {
            if (totemManager.isLocationPartOfATotem(block.getLocation())) {
                totemManager.destroyTotem(block.getLocation(), false);
                break;
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() != event.getTo().getBlockX() ||
                event.getFrom().getBlockZ() != event.getTo().getBlockZ() ||
                event.getFrom().getBlockY() != event.getTo().getBlockY()) {
            totemManager.handlePlayerMovement(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        new BukkitRunnable() {
            @Override
            public void run() {
                totemManager.handlePlayerMovement(event.getPlayer());
            }
        }.runTaskLater(plugin, 1L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        totemManager.enchantPlayer(event.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory != null && clickedInventory.getType() == InventoryType.LECTERN) {
            if (event.getSlot() == 0 && event.getAction().name().contains("PICKUP")) {
                if (totemManager.isLocationPartOfATotem(clickedInventory.getLocation())) {
                    Objects.requireNonNull(clickedInventory.getLocation()).getWorld().playSound(clickedInventory.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.7f);
                }
            }
        }

        boolean isInside = totemManager.isPlayerInAnyTotemRadius(player);
        ItemStack currentItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();

        if (isInside) {
            if (event.getAction().name().equals("MOVE_TO_OTHER_INVENTORY") && clickedInventory != null && clickedInventory.getType() == InventoryType.PLAYER) {
                if (totemManager.enchantSingleItem(currentItem, player.getUniqueId())) {
                    event.setCurrentItem(currentItem);
                }
            }
            else if (clickedInventory != null && clickedInventory.getType() != InventoryType.PLAYER) {
                if (totemManager.enchantSingleItem(cursorItem, player.getUniqueId())) {
                    event.setCursor(cursorItem);
                }
            }
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (totemManager.isPlayerInAnyTotemRadius(player)) {
                    totemManager.disenchantPlayer(player);
                }
            }
        }.runTaskLater(plugin, 1L);
    }

    @EventHandler
    public void onEnchantItem(EnchantItemEvent event) {
        ConfigManager config = new ConfigManager(plugin);
        if (!config.preventEnchantingInZone) return;

        Player player = event.getEnchanter();
        if (totemManager.isPlayerInAnyTotemRadius(player)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "The totem's dark energy prevents you from enchanting items here.");
            player.playSound(player.getLocation(), Sound.BLOCK_REDSTONE_TORCH_BURNOUT, 1.0F, 1.0F);
        }
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ConfigManager config = new ConfigManager(plugin);
        if (!config.preventEnchantingInZone) return;

        if (event.getView().getPlayer() instanceof Player player) {
            if (totemManager.isPlayerInAnyTotemRadius(player)) {
                event.setResult(null);
            }
        }
    }
}