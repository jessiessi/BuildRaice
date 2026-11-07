package com.jessiessi.buildraice;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/** Merkt sich, welchem Spieler welcher Plot gehört, und speichert das in plots.yml. */
public final class PlotManager {

    private final JavaPlugin plugin;
    private final File file;
    private final World world;
    private final Map<UUID, Plot> plotsByOwner = new HashMap<>();
    private final Map<Plot, UUID> ownersByPlot = new HashMap<>();

    public PlotManager(JavaPlugin plugin, World world) {
        this.plugin = plugin;
        this.world = world;
        this.file = new File(plugin.getDataFolder(), "plots.yml");
        load();
    }

    public World world() {
        return world;
    }

    public Plot plotOf(UUID owner) {
        return plotsByOwner.get(owner);
    }

    public UUID ownerOf(Plot plot) {
        return ownersByPlot.get(plot);
    }

    /** Liefert den Plot des Spielers und vergibt beim ersten Mal den nächsten freien. */
    public Plot claim(UUID owner) {
        Plot existing = plotsByOwner.get(owner);
        if (existing != null) {
            return existing;
        }
        Plot plot = PlotGrid.firstFree(candidate -> !ownersByPlot.containsKey(candidate));
        plotsByOwner.put(owner, plot);
        ownersByPlot.put(plot, owner);
        save();
        return plot;
    }

    /** Gibt alle Plots frei. Die nächste Vergabe beginnt wieder in der Mitte. */
    public void clearAll() {
        plotsByOwner.clear();
        ownersByPlot.clear();
        save();
    }

    public boolean hasPlots() {
        return !ownersByPlot.isEmpty();
    }

    public List<Plot> allPlots() {
        return List.copyOf(ownersByPlot.keySet());
    }

    /** Standpunkt in der Plotmitte, oben auf dem, was dort schon gebaut ist. */
    public Location homeOf(Plot plot) {
        int x = plot.minBlockX() + PlotGrid.PLOT_SIZE / 2;
        int z = plot.minBlockZ() + PlotGrid.PLOT_SIZE / 2;
        int y = Math.min(world.getHighestBlockYAt(x, z) + 1, PlotGrid.MAX_FLY_Y);
        return new Location(world, x + 0.5, y, z + 0.5);
    }

    private void load() {
        if (!file.exists()) {
            return;
        }
        ConfigurationSection plots = YamlConfiguration.loadConfiguration(file).getConfigurationSection("plots");
        if (plots == null) {
            return;
        }
        for (String key : plots.getKeys(false)) {
            try {
                UUID owner = UUID.fromString(key);
                Plot plot = new Plot(plots.getInt(key + ".x"), plots.getInt(key + ".z"));
                plotsByOwner.put(owner, plot);
                ownersByPlot.put(plot, owner);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Ungültiger Eintrag in plots.yml wird ignoriert: " + key);
            }
        }
    }

    private void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        plotsByOwner.forEach((owner, plot) -> {
            yaml.set("plots." + owner + ".x", plot.x());
            yaml.set("plots." + owner + ".z", plot.z());
        });
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "plots.yml konnte nicht gespeichert werden", e);
        }
    }
}
