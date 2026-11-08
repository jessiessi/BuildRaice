package com.jessiessi.buildraice;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;

/**
 * Bringt Spieler beim Join an ihren Platz und hält sie dort: vor dem Start in der Lobby-Box
 * (Adventure), danach auf ihrem Plot (Creative), den sie nicht verlassen können.
 */
public final class PlayerListener implements Listener {

    private final PlotManager plots;
    private final GameManager game;

    public PlayerListener(PlotManager plots, GameManager game) {
        this.plots = plots;
        this.game = game;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Plot plot = plots.plotOf(player.getUniqueId());
        if (!game.isStarted()) {
            game.sendToBox(player);
        } else if (plot != null) {
            // Rückkehrer bekommen ihren alten Plot zurück.
            if (!isOnPlot(player.getLocation(), plot)) {
                player.teleport(plots.homeOf(plot));
            }
            player.setGameMode(GameMode.CREATIVE);
            FloorSelector.giveTool(player);
        } else {
            game.assignPlot(player);
        }
        game.onPlayerJoined(player);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!game.isStarted() || !player.getWorld().equals(plots.world())) {
            return;
        }
        // Beim Bauen bleibt jeder auf seinem Plot, bei der Bewertung alle auf dem besuchten Plot.
        Plot plot = game.isBuildingLocked() ? game.viewedPlot() : plots.plotOf(player.getUniqueId());
        boolean confined = plot != null;
        Location to = event.getTo();
        double x = confined ? plot.clampX(to.getX()) : to.getX();
        double y = Math.min(to.getY(), PlotGrid.MAX_FLY_Y);
        double z = confined ? plot.clampZ(to.getZ()) : to.getZ();
        if (x != to.getX() || y != to.getY() || z != to.getZ()) {
            Location limited = to.clone();
            limited.set(x, y, z);
            event.setTo(limited);
        }
    }

    /** In einem Fahrzeug greift das Move-Event nicht, deshalb endet die Fahrt am Plotrand. */
    @EventHandler
    public void onVehicleMove(VehicleMoveEvent event) {
        if (game.isBuildingLocked()) {
            return;
        }
        for (Entity passenger : event.getVehicle().getPassengers()) {
            if (passenger instanceof Player player) {
                Plot plot = plots.plotOf(player.getUniqueId());
                if (plot != null && player.getWorld().equals(plots.world()) && !isOnPlot(event.getTo(), plot)) {
                    event.getVehicle().eject();
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (player.getWorld().equals(plots.world())) {
            boolean hasPlot = game.isStarted() && plots.plotOf(player.getUniqueId()) != null;
            player.setGameMode(hasPlot ? GameMode.CREATIVE : GameMode.ADVENTURE);
        } else if (event.getFrom().equals(plots.world())) {
            player.setGameMode(Bukkit.getDefaultGameMode());
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (player.getWorld().equals(plots.world())) {
            Plot plot = game.isStarted() ? plots.plotOf(player.getUniqueId()) : null;
            event.setRespawnLocation(plot == null ? LobbyBox.spawn(plots.world()) : plots.homeOf(plot));
        }
    }

    private boolean isOnPlot(Location location, Plot plot) {
        return plots.world().equals(location.getWorld())
                && plot.equals(PlotGrid.plotAt(location.getBlockX(), location.getBlockZ()));
    }
}
