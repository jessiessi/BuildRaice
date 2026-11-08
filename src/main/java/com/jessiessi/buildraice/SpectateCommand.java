package com.jessiessi.buildraice;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /spectate: Zuschauen statt mitspielen, bis man /spectate noch einmal eingibt. */
public final class SpectateCommand implements CommandExecutor {

    private final GameManager game;

    public SpectateCommand(GameManager game) {
        this.game = game;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Nur Spieler können zuschauen.", NamedTextColor.RED));
            return true;
        }
        game.toggleSpectate(player);
        return true;
    }
}
