package com.jessiessi.buildraice;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

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
            restore(world.getBlockAt(x, PlotGrid.SURFACE_Y, z), Material.GRASS_BLOCK);
            for (int y = PlotGrid.SURFACE_Y - 1; y > world.getMinHeight(); y--) {
                restore(world.getBlockAt(x, y, z), Material.DIRT);
            }
        }
    }

    /** Entfernt Rüstungsständer, Rahmen, Fahrzeuge und alles andere, was kein Spieler ist. */
    public static void removeEntities(World world, Plot plot) {
        int firstChunkX = plot.minBlockX() >> 4;
        int firstChunkZ = plot.minBlockZ() >> 4;
        int chunks = PlotGrid.PLOT_SIZE / 16;
        for (int chunkX = firstChunkX; chunkX < firstChunkX + chunks; chunkX++) {
            for (int chunkZ = firstChunkZ; chunkZ < firstChunkZ + chunks; chunkZ++) {
                Chunk chunk = world.getChunkAt(chunkX, chunkZ);
                for (Entity entity : chunk.getEntities()) {
                    if (!(entity instanceof Player)) {
                        entity.remove();
                    }
                }
            }
        }
    }

    private static void restore(Block block, Material material) {
        if (block.getType() != material) {
            block.setType(material, false);
        }
    }
}
