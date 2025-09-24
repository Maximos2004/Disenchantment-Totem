package com.MaxAkt.disenchantmenttotem;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Lectern;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public record Totem(Location center, EnderCrystal crystal, List<Location> blocks, List<Location> lecternLocations) {

    /**
     * Counts the number of books currently on the totem's lecterns.
     *
     * @return The total number of books.
     */
    public int countBooks() {
        int books = 0;
        for (Location loc : lecternLocations) {
            Block block = loc.getBlock();
            if (block.getState() instanceof Lectern lectern) {
                ItemStack book = lectern.getInventory().getItem(0);
                if (book != null && (book.getType() == Material.WRITABLE_BOOK || book.getType() == Material.WRITTEN_BOOK || book.getType() == Material.ENCHANTED_BOOK)) {
                    books++;
                }
            }
        }
        return books;
    }

    /**
     * Calculates the totem's current radius based on the number of books on its lecterns.
     *
     * @param defaultRadius The base radius from the config.
     * @param radiusPerBook The bonus radius per book from the config.
     * @return The calculated total radius.
     */
    public int getCurrentRadius(int defaultRadius, int radiusPerBook) {
        return defaultRadius + (countBooks() * radiusPerBook);
    }
}