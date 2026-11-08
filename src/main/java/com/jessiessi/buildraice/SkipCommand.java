package com.jessiessi.buildraice;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/** /skip beendet die Bewertung des gezeigten Plots sofort und geht zum nächsten. */
public final class SkipCommand implements CommandExecutor {

    private final GameManager game;

    public SkipCommand(GameManager game) {
        this.game = game;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        game.skipPlot(sender);
        return true;
    }
}
