package com.jessiessi.buildraice;

/** Ein Plot im Raster. x/z sind Plot-Koordinaten, keine Blockkoordinaten. */
public record Plot(int x, int z) {

    /** Abstand zum Plotrand, damit auch der Körper des Spielers (0,6 Blöcke breit) im Plot bleibt. */
    private static final double EDGE_MARGIN = 0.3;

    public int minBlockX() {
        return x * PlotGrid.CELL_SIZE;
    }

    public int minBlockZ() {
        return z * PlotGrid.CELL_SIZE;
    }

    public double clampX(double positionX) {
        return clamp(positionX, minBlockX());
    }

    public double clampZ(double positionZ) {
        return clamp(positionZ, minBlockZ());
    }

    private static double clamp(double position, int min) {
        return Math.clamp(position, min + EDGE_MARGIN, min + PlotGrid.PLOT_SIZE - EDGE_MARGIN);
    }
}
