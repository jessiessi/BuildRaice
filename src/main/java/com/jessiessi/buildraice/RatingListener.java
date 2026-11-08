package com.jessiessi.buildraice;

import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import io.papermc.paper.event.player.PlayerTrackEntityEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Nimmt Hotbar-Tasten und Mausrad als Stimmen an und hält die Abstimm-Items an ihrem Platz.
 * Den anderen Spielern wird die Hand dabei leer angezeigt, damit niemand die Wertung verrät.
 */
public final class RatingListener implements Listener {

    private final JavaPlugin plugin;
    private final GameManager game;

    public RatingListener(JavaPlugin plugin, GameManager game) {
        this.plugin = plugin;
        this.game = game;
    }

    @EventHandler
    public void onHotbarKey(PlayerItemHeldEvent event) {
        if (game.tour() != null) {
            game.tour().vote(event.getPlayer(), event.getNewSlot());
        }
    }

    /**
     * Der Server schickt den neuen Block in der Hand gleich nach diesem Event an alle anderen.
     * Einen Tick später überschreiben wir das bei ihnen mit einer leeren Hand.
     */
    @EventHandler
    public void onEquipmentChanged(EntityEquipmentChangedEvent event) {
        if (game.tour() != null && event.getEntity() instanceof Player voter
                && event.getEquipmentChanges().containsKey(EquipmentSlot.HAND)) {
            Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getOnlinePlayers().stream()
                    .filter(other -> !other.equals(voter))
                    .forEach(other -> hideHand(other, voter)));
        }
    }

    /** Wer einen Spieler neu zu sehen bekommt, erhält dessen Ausrüstung mit; auch dann die Hand leeren. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTrack(PlayerTrackEntityEvent event) {
        if (game.tour() != null && event.getEntity() instanceof Player voter) {
            Player viewer = event.getPlayer();
            Bukkit.getScheduler().runTask(plugin, () -> hideHand(viewer, voter));
        }
    }

    private void hideHand(Player viewer, Player voter) {
        if (game.tour() != null && viewer.isOnline() && voter.isOnline()) {
            viewer.sendEquipmentChange(voter, EquipmentSlot.HAND, ItemStack.empty());
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
