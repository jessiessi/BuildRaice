package com.jessiessi.buildraice;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Spieler, die sich mit /spectate ausgeschlossen haben. Sie schauen nur zu, bis sie /spectate erneut
 * eingeben. Die Liste steht in spectators.yml und übersteht damit Rejoin und Serverneustart.
 */
public final class Spectators {

    private final JavaPlugin plugin;
    private final File file;
    private final Set<UUID> spectators = new HashSet<>();

    public Spectators(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "spectators.yml");
        load();
    }

    public boolean contains(UUID player) {
        return spectators.contains(player);
    }

    public boolean contains(Player player) {
        return contains(player.getUniqueId());
    }

    /** Schaltet um und liefert true, wenn der Spieler jetzt zuschaut. */
    public boolean toggle(UUID player) {
        boolean nowSpectating = spectators.add(player);
        if (!nowSpectating) {
            spectators.remove(player);
        }
        save();
        return nowSpectating;
    }

    private void load() {
        if (!file.exists()) {
            return;
        }
        for (String entry : YamlConfiguration.loadConfiguration(file).getStringList("spectators")) {
            try {
                spectators.add(UUID.fromString(entry));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Ungültiger Eintrag in spectators.yml wird ignoriert: " + entry);
            }
        }
    }

    private void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("spectators", spectators.stream().map(UUID::toString).sorted().toList());
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "spectators.yml konnte nicht gespeichert werden", e);
        }
    }
}
