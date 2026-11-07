package com.jessiessi.buildraice;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlotGridTest {

    @Test
    void plotCoversExactlyTwoByTwoChunks() {
        for (Plot plot : new Plot[] {new Plot(0, 0), new Plot(1, 2), new Plot(-1, -3)}) {
            int minX = plot.minBlockX();
            int minZ = plot.minBlockZ();
            assertEquals(0, Math.floorMod(minX, 16), "Plot beginnt an einer Chunk-Grenze");
            assertEquals(0, Math.floorMod(minZ, 16), "Plot beginnt an einer Chunk-Grenze");

            int blocks = 0;
            for (int x = minX - 1; x <= minX + 32; x++) {
                for (int z = minZ - 1; z <= minZ + 32; z++) {
                    if (plot.equals(PlotGrid.plotAt(x, z))) {
                        blocks++;
                    }
                }
            }
            assertEquals(32 * 32, blocks);
        }
    }

    @Test
    void negativeCoordinatesMapToTheirOwnPlot() {
        assertEquals(new Plot(-1, -1), PlotGrid.plotAt(-48, -48));
        assertEquals(new Plot(-1, -1), PlotGrid.plotAt(-17, -17));
        assertNull(PlotGrid.plotAt(-16, -16));
        assertNull(PlotGrid.plotAt(-1, -1));
    }

    @Test
    void plotsAreSeparatedBySixteenBlocksOfRoad() {
        assertEquals(new Plot(0, 0), PlotGrid.plotAt(31, 0));
        for (int x = 32; x < 48; x++) {
            assertNull(PlotGrid.plotAt(x, 0));
        }
        assertEquals(new Plot(1, 0), PlotGrid.plotAt(48, 0));
    }

    @Test
    void spawnIsOnTheRoad() {
        assertNull(PlotGrid.plotAt(PlotGrid.SPAWN_X, PlotGrid.SPAWN_Z));
    }

    @Test
    void clampKeepsPlayerBodyInsideThePlot() {
        Plot plot = new Plot(0, 0);
        assertEquals(0.3, plot.clampX(-5), 1e-9);
        assertEquals(31.7, plot.clampX(40), 1e-9);
        assertEquals(8.5, plot.clampZ(8.5), 1e-9);

        Plot negative = new Plot(-1, 0);
        assertEquals(-47.7, negative.clampX(-60), 1e-9);
        assertEquals(-16.3, negative.clampX(-16), 1e-9);
    }

    @Test
    void lobbyBoxStandsOnPlainRoadAroundTheSpawn() {
        for (int x = PlotGrid.BOX_MIN; x <= PlotGrid.BOX_MAX; x++) {
            for (int z = PlotGrid.BOX_MIN; z <= PlotGrid.BOX_MAX; z++) {
                assertNull(PlotGrid.plotAt(x, z));
                assertFalse(PlotGrid.isBorder(x, z));
            }
        }
        assertTrue(PlotGrid.SPAWN_X > PlotGrid.BOX_MIN && PlotGrid.SPAWN_X < PlotGrid.BOX_MAX);
        assertTrue(PlotGrid.SPAWN_Z > PlotGrid.BOX_MIN && PlotGrid.SPAWN_Z < PlotGrid.BOX_MAX);
    }

    @Test
    void borderSurroundsPlotButLeavesItUntouched() {
        assertFalse(PlotGrid.isBorder(0, 0));
        assertFalse(PlotGrid.isBorder(31, 31));
        assertTrue(PlotGrid.isBorder(32, 0));
        assertTrue(PlotGrid.isBorder(-1, 31));
        assertTrue(PlotGrid.isBorder(32, 32), "Ecke gehört zum Rand");
        assertFalse(PlotGrid.isBorder(40, 8), "Wegmitte ist kein Rand");
    }

    @Test
    void firstFreeStartsAtCenterAndNeverRepeats() {
        Set<Plot> taken = new HashSet<>();
        assertEquals(new Plot(0, 0), PlotGrid.firstFree(plot -> !taken.contains(plot)));
        for (int i = 0; i < 100; i++) {
            assertTrue(taken.add(PlotGrid.firstFree(plot -> !taken.contains(plot))));
        }
        assertEquals(100, taken.size());
    }
}
