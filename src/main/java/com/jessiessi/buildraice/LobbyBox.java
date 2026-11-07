package com.jessiessi.buildraice;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

/** Die geschlossene Glasbox, in der Spieler ohne Plot auf den Start warten. */
public final class LobbyBox {

    private LobbyBox() {
    }

    /** Baut Wände und Dach. Der Boden ist der Weg, der dort ohnehin liegt. */
    public static void build(World world) {
        int bottom = PlotGrid.SURFACE_Y + 1;
        int top = PlotGrid.SURFACE_Y + PlotGrid.BOX_HEIGHT;
        for (int x = PlotGrid.BOX_MIN; x <= PlotGrid.BOX_MAX; x++) {
            for (int z = PlotGrid.BOX_MIN; z <= PlotGrid.BOX_MAX; z++) {
                boolean wall = x == PlotGrid.BOX_MIN || x == PlotGrid.BOX_MAX
                        || z == PlotGrid.BOX_MIN || z == PlotGrid.BOX_MAX;
                for (int y = bottom; y <= top; y++) {
                    if (wall || y == top) {
                        world.getBlockAt(x, y, z).setType(Material.GLASS, false);
                    }
                }
            }
        }
    }

    public static Location spawn(World world) {
        return new Location(world, PlotGrid.SPAWN_X + 0.5, PlotGrid.SURFACE_Y + 1, PlotGrid.SPAWN_Z + 0.5);
    }
}
