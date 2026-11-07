package com.jessiessi.buildraice;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/** /plot teleportiert zum eigenen Plot, /plot info zeigt den Plot unter den Füßen. */
public final class PlotCommand implements TabExecutor {

    private final PlotManager plots;
    private final GameManager game;

    public PlotCommand(PlotManager plots, GameManager game) {
        this.plots = plots;
        this.game = game;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Dieser Befehl ist nur für Spieler.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) {
            Plot plot = game.isStarted() ? plots.plotOf(player.getUniqueId()) : null;
            if (plot == null) {
                player.sendMessage(Component.text("Du bekommst deinen Plot beim Start.", NamedTextColor.YELLOW));
                return true;
            }
            player.teleport(plots.homeOf(plot));
            player.sendMessage(Component.text("Willkommen auf deinem Plot!", NamedTextColor.GREEN));
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("info")) {
            sendInfo(player);
            return true;
        }
        return false;
    }

    private void sendInfo(Player player) {
        Location location = player.getLocation();
        Plot plot = location.getWorld().equals(plots.world())
                ? PlotGrid.plotAt(location.getBlockX(), location.getBlockZ())
                : null;
        if (plot == null) {
            player.sendMessage(Component.text("Du stehst gerade auf keinem Plot.", NamedTextColor.YELLOW));
            return;
        }
        UUID owner = plots.ownerOf(plot);
        String ownerName = owner == null ? "niemandem" : Bukkit.getOfflinePlayer(owner).getName();
        player.sendMessage(Component.text(
                "Plot " + plot.x() + ";" + plot.z() + " gehört " + ownerName + ".",
                NamedTextColor.AQUA));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return args.length == 1 && "info".startsWith(args[0].toLowerCase()) ? List.of("info") : List.of();
    }
}
