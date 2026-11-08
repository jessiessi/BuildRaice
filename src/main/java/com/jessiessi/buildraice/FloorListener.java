package com.jessiessi.buildraice;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/** Öffnet die Boden-Auswahl per Klick mit dem roten Farbstoff und nimmt die Wahl entgegen. */
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
