package com.jessiessi.buildraice;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

/**
 * Nimmt die Hotbar-Tasten als Stimmen an und hält die Abstimm-Items an ihrem Platz. Der Slotwechsel
 * selbst wird abgelehnt, damit niemand mit einem Block in der Hand gesehen wird.
 */
public final class RatingListener implements Listener {

    private final GameManager game;

    public RatingListener(GameManager game) {
        this.game = game;
    }

    @EventHandler
    public void onHotbarKey(PlayerItemHeldEvent event) {
        if (game.tour() != null) {
            game.tour().vote(event.getPlayer(), event.getNewSlot());
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (game.tour() != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (game.tour() != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (game.tour() != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (game.tour() != null) {
            event.setCancelled(true);
        }
    }
}
