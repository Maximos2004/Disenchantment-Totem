package com.MaxAkt.disenchantmenttotem;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

import java.util.Map;

public record StoredItemData(Material originalMaterial, Map<Enchantment, Integer> enchantments) {
}