package com.jessiessi.buildraice;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /bestenliste öffnet die Gesamtpunkte als Dialog, auch per Klick auf den Link aus /ende. */
public final class LeaderboardCommand implements CommandExecutor {

    private final Leaderboard leaderboard;

    public LeaderboardCommand(Leaderboard leaderboard) {
        this.leaderboard = leaderboard;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            player.showDialog(leaderboard.dialog());
        } else {
            leaderboard.lines().forEach(sender::sendMessage);
        }
        return true;
    }
}
