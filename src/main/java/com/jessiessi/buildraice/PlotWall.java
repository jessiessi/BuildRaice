package com.jessiessi.buildraice;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.function.Consumer;

/** Die Wand um einen Plot: steht auf dem weißen Rand und reicht bis zur Welthöhe. */
public final class PlotWall {

    private PlotWall() {
    }

    public static void build(World world, Plot plot) {
        forEachBlock(world, plot, block -> block.setType(materialAt(block.getY()), false));
    }

    public static void remove(World world, Plot plot) {
        forEachBlock(world, plot, block -> block.setType(Material.AIR, false));
    }

    private static void forEachBlock(World world, Plot plot, Consumer<Block> action) {
        int minX = plot.minBlockX() - 1;
        int minZ = plot.minBlockZ() - 1;
        int maxX = plot.minBlockX() + PlotGrid.PLOT_SIZE;
        int maxZ = plot.minBlockZ() + PlotGrid.PLOT_SIZE;
        for (int x = minX; x <= maxX; x++) {
            forEachInColumn(world, x, minZ, action);
            forEachInColumn(world, x, maxZ, action);
        }
        for (int z = minZ + 1; z < maxZ; z++) {
            forEachInColumn(world, minX, z, action);
            forEachInColumn(world, maxX, z, action);
        }
    }

    private static void forEachInColumn(World world, int x, int z, Consumer<Block> action) {
        for (int y = PlotGrid.SURFACE_Y + 1; y < world.getMaxHeight(); y++) {
            action.accept(world.getBlockAt(x, y, z));
        }
    }

    /** Waagerechte Streifen: Jede Blockschicht wechselt zwischen Blau und Gelb. */
    private static Material materialAt(int y) {
        return Math.floorMod(y, 2) == 0 ? Material.BLUE_CONCRETE : Material.YELLOW_CONCRETE;
    }
}
