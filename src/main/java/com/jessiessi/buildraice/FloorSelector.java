package com.jessiessi.buildraice;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * Die Boden-Auswahl: Mit dem roten Farbstoff öffnet sich ein Fenster, in dem man den Boden des
 * eigenen Plots gegen Gras, Stein, Sand, Schnee oder eine Betonfarbe tauschen kann.
 */
public final class FloorSelector {

    public static final Material DEFAULT_FLOOR = Material.GRASS_BLOCK;

    private static final NamespacedKey TOOL_KEY = new NamespacedKey("buildraice", "floor_tool");
    private static final int TOOL_SLOT = 8;
    private static final Component TITLE = Component.text("Boden auswählen");

    /** Erste Reihe: Naturblöcke, darunter die 16 Betonfarben. */
    private static final List<Material> NATURE = List.of(
            Material.GRASS_BLOCK, Material.STONE, Material.SAND, Material.SNOW_BLOCK);
    private static final int[] NATURE_SLOTS = {1, 3, 5, 7};
    private static final List<Material> CONCRETE = List.of(
            Material.WHITE_CONCRETE, Material.LIGHT_GRAY_CONCRETE, Material.GRAY_CONCRETE, Material.BLACK_CONCRETE,
            Material.BROWN_CONCRETE, Material.RED_CONCRETE, Material.ORANGE_CONCRETE, Material.YELLOW_CONCRETE,
            Material.LIME_CONCRETE, Material.GREEN_CONCRETE, Material.CYAN_CONCRETE, Material.LIGHT_BLUE_CONCRETE,
            Material.BLUE_CONCRETE, Material.PURPLE_CONCRETE, Material.MAGENTA_CONCRETE, Material.PINK_CONCRETE);
    private static final int[] CONCRETE_SLOTS = {9, 10, 11, 12, 13, 14, 15, 16, 17, 19, 20, 21, 22, 23, 24, 25};

    private FloorSelector() {
    }

    /** Das geöffnete Auswahlfenster. Daran erkennt der Listener seine Klicks. */
    public static final class Menu implements InventoryHolder {
        private final Inventory inventory;

        private Menu(Material current) {
            this.inventory = Bukkit.createInventory(this, 27, TITLE);
            for (int i = 0; i < NATURE.size(); i++) {
                inventory.setItem(NATURE_SLOTS[i], option(NATURE.get(i), current));
            }
            for (int i = 0; i < CONCRETE.size(); i++) {
                inventory.setItem(CONCRETE_SLOTS[i], option(CONCRETE.get(i), current));
            }
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    public static void openMenu(Player player, Material current) {
        player.openInventory(new Menu(current).getInventory());
    }

    public static boolean isOption(Material material) {
        return NATURE.contains(material) || CONCRETE.contains(material);
    }

    public static boolean isTool(ItemStack item) {
        return item != null && item.getType() == Material.RED_DYE
                && item.getPersistentDataContainer().has(TOOL_KEY, PersistentDataType.BYTE);
    }

    /** Legt den Farbstoff in den letzten Hotbar-Slot, falls der Spieler noch keinen hat. */
    public static void giveTool(Player player) {
        PlayerInventory inventory = player.getInventory();
        for (ItemStack item : inventory.getContents()) {
            if (isTool(item)) {
                return;
            }
        }
        ItemStack tool = new ItemStack(Material.RED_DYE);
        tool.editMeta(meta -> {
            meta.displayName(Component.text("Boden ändern", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(Component.text("Klicken, um den Boden deines Plots auszuwählen", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)));
            meta.getPersistentDataContainer().set(TOOL_KEY, PersistentDataType.BYTE, (byte) 1);
        });
        if (inventory.getItem(TOOL_SLOT) == null) {
            inventory.setItem(TOOL_SLOT, tool);
        } else {
            inventory.addItem(tool);
        }
    }

    /**
     * Tauscht die Bodenschicht des Plots. Nur Blöcke, die noch der alte Boden sind, werden ersetzt,
     * damit Gebautes in der Bodenschicht erhalten bleibt.
     */
    public static void replace(World world, Plot plot, Material from, Material to) {
        for (int x = plot.minBlockX(); x < plot.minBlockX() + PlotGrid.PLOT_SIZE; x++) {
            for (int z = plot.minBlockZ(); z < plot.minBlockZ() + PlotGrid.PLOT_SIZE; z++) {
                Block block = world.getBlockAt(x, PlotGrid.SURFACE_Y, z);
                if (block.getType() == from) {
                    block.setType(to, false);
                }
            }
        }
    }

    private static ItemStack option(Material material, Material current) {
        ItemStack item = new ItemStack(material);
        if (material == current) {
            item.editMeta(meta -> {
                meta.setEnchantmentGlintOverride(true);
                meta.lore(List.of(Component.text("Aktueller Boden", NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false)));
            });
        }
        return item;
    }
}
