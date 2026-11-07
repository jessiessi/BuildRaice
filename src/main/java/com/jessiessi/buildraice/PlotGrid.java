package com.jessiessi.buildraice;

import java.util.function.Predicate;

/**
 * Das Raster der Welt: Ein Plot besteht aus 2x2 Chunks (32x32 Blöcke), danach folgt ein Chunk
 * Weg (16 Blöcke). Plots beginnen immer an einer Chunk-Grenze.
 */
public final class PlotGrid {

    public static final int PLOT_SIZE = 32;
    public static final int ROAD_SIZE = 16;
    /** Abstand von einem Plot-Anfang zum nächsten: Plot plus Weg. */
    public static final int CELL_SIZE = PLOT_SIZE + ROAD_SIZE;
    /** Höhe der obersten Bodenschicht. */
    public static final int SURFACE_Y = -60;
    /** Höchste Y-Koordinate, bis zu der Spieler fliegen können. */
    public static final int MAX_FLY_Y = 200;
    /** Weltspawn in der Mitte der Lobby-Box. */
    public static final int SPAWN_X = -8;
    public static final int SPAWN_Z = -8;
    /** Außenmaße der Lobby-Box. Sie steht im Weg-Chunk -1;-1, innen bleiben 12x12 Blöcke. */
    public static final int BOX_MIN = -15;
    public static final int BOX_MAX = -2;
    public static final int BOX_HEIGHT = 5;

    private PlotGrid() {
    }

    /** Liefert den Plot an dieser Blockposition oder null, wenn dort Weg ist. */
    public static Plot plotAt(int blockX, int blockZ) {
        if (Math.floorMod(blockX, CELL_SIZE) >= PLOT_SIZE || Math.floorMod(blockZ, CELL_SIZE) >= PLOT_SIZE) {
            return null;
        }
        return new Plot(Math.floorDiv(blockX, CELL_SIZE), Math.floorDiv(blockZ, CELL_SIZE));
    }

    /** True für den Wegblock-Ring, der direkt an einen Plot grenzt. */
    public static boolean isBorder(int blockX, int blockZ) {
        if (plotAt(blockX, blockZ) != null) {
            return false;
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (plotAt(blockX + dx, blockZ + dz) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Sucht ringförmig von der Mitte aus den ersten freien Plot. */
    public static Plot firstFree(Predicate<Plot> isFree) {
        for (int ring = 0; ; ring++) {
            for (int x = -ring; x <= ring; x++) {
                for (int z = -ring; z <= ring; z++) {
                    if (Math.max(Math.abs(x), Math.abs(z)) != ring) {
                        continue;
                    }
                    Plot plot = new Plot(x, z);
                    if (isFree.test(plot)) {
                        return plot;
                    }
                }
            }
        }
    }
}
