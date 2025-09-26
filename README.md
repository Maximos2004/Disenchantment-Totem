This is the official public repo for the Minecraft Server Plugin:

# ✨ Disenchantment Totem ✨
Introducing the Disenchantment Totem, a mystical structure that creates a dynamic zone of null magic. **Inside its radius, all enchantments are temporarily disabled (De-enchanted)** from your gear, **only to return the moment you step out (Re-enchanted)**.

It's the ultimate tool for creating fair PvP arenas, neutral trading hubs, or unique challenge zones where skill matters more than gear!

![Totem showcase](https://cdn.modrinth.com/data/cached_images/3d2cb916190631c54e580f5c4a43175e9e6e0d3e.gif)


## 🔮 Core Features
- **Seamless Enchantment Suppression:** Automatically and temporarily removes enchantments from all items in a player's inventory upon entering the totem's radius.
- **Automatic Restoration:** Enchantments are instantly and safely returned the moment a player leaves the totem's influence.
- **Inventory Safe:** Reliably tracks items even when they are moved in your inventory, dropped, or stored in containers within the zone.
- **Golden Apple Conversion:** As a unique strategic feature, Enchanted Golden Apples are temporarily converted into regular Golden Apples inside the zone (toggleable in config).
- **Expandable Radius:** Increase the totem's power and range by placing books on its four lecterns. More books mean a larger area of effect!
- **Stunning & Configurable Effects:** Features beautiful, server-friendly particle effects that visually represent the totem's power. Every single effect can be turned on or off in the config!
- **Built-in Protection:** Prevents players from using Enchanting Tables and Anvils while inside the totem's radius.

## ⚙️ How to Build a Totem
Building your first totem is simple. Construct the following multi-layer structure:
- Layer 1 (Bottom): Place 1 Bookshelf in the center, surrounded by 4 Lecterns on each side.
- Layer 2 (Middle): Place 1 Bookshelf on top of the first one, surrounded by 4 End Rods.
- Layer 3 (Top): Place 1 Obsidian block on top of the second bookshelf.
To Activate: Right-click the top of the Obsidian block with an End Crystal! (Crystal has to go in last!)
- (Optional): Add 4 Books on the Lecterns to expand the radius of the Totem

![How to Build Totem](https://cdn.modrinth.com/data/cached_images/b15dd7b9ce47886cebd60cc5960d7753e0a7e508.png)

## 🔧 Highly Configurable
You can easily toggle in the Config file:
- All three particle effects individually (floor, book, sucking).
- The ambient idle sound.
- The enter/exit messages shown to players.
- The Enchanted Golden Apple conversion feature.
- The enchanting & anvil prevention feature.
