package com.jessiessi.buildraice;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Öffnet die Boden-Auswahl per Klick mit dem roten Farbstoff und nimmt die Wahl entgegen. Der Farbstoff
 * lässt sich weder wegwerfen noch in Behälter oder Rahmen legen; was trotzdem verloren geht, ersetzt
 * der GameManager jede Sekunde.
 */
public final class FloorListener implements Listener {

    private final JavaPlugin plugin;
    private final GameManager game;

    public FloorListener(JavaPlugin plugin, GameManager game) {
        this.plugin = plugin;
        this.game = game;
    }

    /** Läuft auch für schon abgebrochene Klicks: Linksklicks in die Luft kommen immer so an. */
    @EventHandler
    public void onToolClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() == Action.PHYSICAL
                || !FloorSelector.isTool(event.getItem())) {
            return;
        }
        // Kein Abbauen, Färben oder Platzieren mit dem Farbstoff.
        event.setCancelled(true);
        game.openFloorMenu(event.getPlayer());
    }

    /** Klick auf Rahmen, Rüstungsständer & Co.: nichts hineinlegen, stattdessen die Auswahl öffnen. */
    @EventHandler
    public void onToolClickEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() == EquipmentSlot.HAND
                && FloorSelector.isTool(event.getPlayer().getInventory().getItemInMainHand())) {
            event.setCancelled(true);
            game.openFloorMenu(event.getPlayer());
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (FloorSelector.isTool(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    /** Bei offener Truhe o. Ä. darf der Farbstoff nicht bewegt werden, damit er nicht dort landet. */
    @EventHandler
    public void onMoveIntoContainer(InventoryClickEvent event) {
        if (!isContainer(event.getView())) {
            return;
        }
        ItemStack hotbar = event.getHotbarButton() >= 0
                ? event.getWhoClicked().getInventory().getItem(event.getHotbarButton()) : null;
        if (FloorSelector.isTool(event.getCurrentItem()) || FloorSelector.isTool(event.getCursor())
                || FloorSelector.isTool(hotbar)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDragIntoContainer(InventoryDragEvent event) {
        if (isContainer(event.getView()) && FloorSelector.isTool(event.getOldCursor())) {
            event.setCancelled(true);
        }
    }

    /** Ein fremdes Inventar ist offen, nicht nur das eigene. Die Boden-Auswahl regelt ihre Klicks selbst. */
    private static boolean isContainer(InventoryView view) {
        InventoryType type = view.getTopInventory().getType();
        return type != InventoryType.CRAFTING && type != InventoryType.CREATIVE
                && !(view.getTopInventory().getHolder() instanceof FloorSelector.Menu);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof FloorSelector.Menu)) {
            return;
        }
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (event.getClickedInventory() != event.getView().getTopInventory() || clicked == null
                || !FloorSelector.isOption(clicked.getType()) || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        game.changeFloor(player, clicked.getType());
        Bukkit.getScheduler().runTask(plugin, () -> player.closeInventory());
    }

    @EventHandler
    public void onMenuDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof FloorSelector.Menu) {
            event.setCancelled(true);
        }
    }
}
