package com.jessiessi.buildraice;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/** /ende beendet das Spiel und veröffentlicht die Bestenliste. */
public final class EndCommand implements CommandExecutor {

    private final GameManager game;

    public EndCommand(GameManager game) {
        this.game = game;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        game.endGame(sender);
        return true;
    }
}
