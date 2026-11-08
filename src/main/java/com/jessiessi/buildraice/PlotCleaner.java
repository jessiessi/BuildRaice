package com.jessiessi.buildraice;

import org.bukkit.Chunk;
import org.bukkit.HeightMap;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;

import java.util.ArrayList;
import java.util.List;

/** Setzt einen Plot auf den Ausgangszustand zurück, Reihe für Reihe, um den Server nicht zu blockieren. */
public final class PlotCleaner {

    private PlotCleaner() {
    }

    public static void clearRow(World world, Plot plot, int row) {
        int x = plot.minBlockX() + row;
        for (int z = plot.minBlockZ(); z < plot.minBlockZ() + PlotGrid.PLOT_SIZE; z++) {
            for (int y = world.getMaxHeight() - 1; y > PlotGrid.SURFACE_Y; y--) {
                restore(world.getBlockAt(x, y, z), Material.AIR);
            }
            restore(world.getBlockAt(x, PlotGrid.SURFACE_Y, z), FloorSelector.DEFAULT_FLOOR);
            for (int y = PlotGrid.SURFACE_Y - 1; y > world.getMinHeight(); y--) {
                restore(world.getBlockAt(x, y, z), Material.DIRT);
            }
        }
    }

    /** Entfernt Rüstungsständer, Rahmen, Fahrzeuge und alles andere, was kein Spieler ist. */
    public static void removeEntities(World world, Plot plot) {
        entitiesOn(world, plot).forEach(Entity::remove);
    }

    /** Entfernt, was vor der Bewertung auf dem Boden herumliegt: Items, XP-Kugeln und Geschosse. */
    public static void removeLooseEntities(World world, Plot plot) {
        for (Entity entity : entitiesOn(world, plot)) {
            if (entity instanceof Item || entity instanceof ExperienceOrb || entity instanceof Projectile) {
                entity.remove();
            }
        }
    }

    /**
     * True, wenn auf dem Plot nichts verändert wurde: nichts über dem Boden, kein Loch, der Boden
     * noch Gras und keine Entities wie Rüstungsständer oder Bilder.
     */
    public static boolean isUntouched(World world, Plot plot) {
        for (int x = plot.minBlockX(); x < plot.minBlockX() + PlotGrid.PLOT_SIZE; x++) {
            for (int z = plot.minBlockZ(); z < plot.minBlockZ() + PlotGrid.PLOT_SIZE; z++) {
                if (world.getHighestBlockYAt(x, z, HeightMap.WORLD_SURFACE) != PlotGrid.SURFACE_Y
                        || world.getBlockAt(x, PlotGrid.SURFACE_Y, z).getType() != FloorSelector.DEFAULT_FLOOR) {
                    return false;
                }
            }
        }
        return entitiesOn(world, plot).isEmpty();
    }

    /** Alle Entities auf dem Plot außer Spielern. */
    private static List<Entity> entitiesOn(World world, Plot plot) {
        List<Entity> found = new ArrayList<>();
        int firstChunkX = plot.minBlockX() >> 4;
        int firstChunkZ = plot.minBlockZ() >> 4;
        int chunks = PlotGrid.PLOT_SIZE / 16;
        for (int chunkX = firstChunkX; chunkX < firstChunkX + chunks; chunkX++) {
            for (int chunkZ = firstChunkZ; chunkZ < firstChunkZ + chunks; chunkZ++) {
                Chunk chunk = world.getChunkAt(chunkX, chunkZ);
                for (Entity entity : chunk.getEntities()) {
                    if (!(entity instanceof Player)) {
                        found.add(entity);
                    }
                }
            }
        }
        return found;
    }

    private static void restore(Block block, Material material) {
        if (block.getType() != material) {
            block.setType(material, false);
        }
    }
}
