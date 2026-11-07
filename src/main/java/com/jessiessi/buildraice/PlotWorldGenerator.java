package com.jessiessi.buildraice;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

import java.util.List;
import java.util.Random;

/** Erzeugt die flache Plot-Welt: Bedrock, Erde und darauf Gras (Plot) oder Beton (Weg). */
public final class PlotWorldGenerator extends ChunkGenerator {

    @Override
    public void generateSurface(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData chunk) {
        int bedrockY = worldInfo.getMinHeight();
        chunk.setRegion(0, bedrockY, 0, 16, bedrockY + 1, 16, Material.BEDROCK);
        chunk.setRegion(0, bedrockY + 1, 0, 16, PlotGrid.SURFACE_Y, 16, Material.DIRT);

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int blockX = chunkX * 16 + x;
                int blockZ = chunkZ * 16 + z;
                chunk.setBlock(x, PlotGrid.SURFACE_Y, z, surfaceAt(blockX, blockZ));
            }
        }
    }

    private static Material surfaceAt(int blockX, int blockZ) {
        if (PlotGrid.plotAt(blockX, blockZ) != null) {
            return Material.GRASS_BLOCK;
        }
        return PlotGrid.isBorder(blockX, blockZ) ? Material.WHITE_CONCRETE : Material.GRAY_CONCRETE;
    }

    @Override
    public BiomeProvider getDefaultBiomeProvider(WorldInfo worldInfo) {
        return new BiomeProvider() {
            @Override
            public Biome getBiome(WorldInfo info, int x, int y, int z) {
                return Biome.PLAINS;
            }

            @Override
            public List<Biome> getBiomes(WorldInfo info) {
                return List.of(Biome.PLAINS);
            }
        };
    }

    @Override
    public Location getFixedSpawnLocation(World world, Random random) {
        return new Location(world, PlotGrid.SPAWN_X + 0.5, PlotGrid.SURFACE_Y + 1, PlotGrid.SPAWN_Z + 0.5);
    }
}
