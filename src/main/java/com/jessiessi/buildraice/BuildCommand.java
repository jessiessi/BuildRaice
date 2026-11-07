package com.jessiessi.buildraice;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

/**
 * /build &lt;Gegenstand&gt; &lt;Minuten&gt; holt alle aus der Box, gibt ihnen ihre Plots und startet
 * die Runde. Der Gegenstand darf Leerzeichen enthalten.
 */
public final class BuildCommand implements CommandExecutor {

    private final PlotManager plots;
    private final GameManager game;
    private final Leaderboard leaderboard;

    public BuildCommand(PlotManager plots, GameManager game, Leaderboard leaderboard) {
        this.plots = plots;
        this.game = game;
        this.leaderboard = leaderboard;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            return false;
        }
        int minutes;
        try {
            minutes = Integer.parseInt(args[args.length - 1]);
        } catch (NumberFormatException e) {
            minutes = 0;
        }
        if (minutes <= 0) {
            sender.sendMessage(Component.text("Die Zeit muss eine ganze Zahl von Minuten über 0 sein.",
                    NamedTextColor.RED));
            return true;
        }
        if (game.isResetting()) {
            sender.sendMessage(Component.text("Die Plots werden noch geleert, einen Moment bitte.",
                    NamedTextColor.YELLOW));
            return true;
        }
        if (game.isRoundInProgress()) {
            sender.sendMessage(Component.text("Es läuft bereits eine Runde.", NamedTextColor.RED));
            return true;
        }
        List<Player> waiting = Bukkit.getOnlinePlayers().stream()
                .filter(player -> plots.plotOf(player.getUniqueId()) == null)
                .map(player -> (Player) player)
                .toList();
        if (waiting.isEmpty() && !plots.hasPlots()) {
            sender.sendMessage(Component.text("Es ist noch niemand zum Bauen da.", NamedTextColor.RED));
            return true;
        }
        // Nach /ende beginnt hier ein neues Spiel mit neuen Punkten.
        leaderboard.resetIfFinished();
        waiting.forEach(game::assignPlot);
        game.startRound(String.join(" ", Arrays.copyOf(args, args.length - 1)), minutes);
        return true;
    }
}
