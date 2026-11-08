package com.jessiessi.buildraice;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /stop bricht die laufende Runde ab und schickt alle zurück in die Box. Der Befehl ersetzt das
 * /stop von Minecraft; in der Konsole fährt stop den Server deshalb weiterhin herunter.
 */
public final class StopCommand implements CommandExecutor {

    private final GameManager game;

    public StopCommand(GameManager game) {
        this.game = game;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            Bukkit.shutdown();
            return true;
        }
        game.abortRound(sender);
        return true;
    }
}
