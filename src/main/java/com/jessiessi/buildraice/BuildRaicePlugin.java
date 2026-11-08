package com.jessiessi.buildraice;

import org.bukkit.*;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class BuildRaicePlugin extends JavaPlugin {

    public static final String WORLD_NAME = "buildraice";

    private GameManager game;

    @Override
    public void onEnable() {
        getDataFolder().mkdirs();

        World world = createWorld();
        PlotManager plots = new PlotManager(this, world);
        Leaderboard leaderboard = new Leaderboard(this);
        game = new GameManager(this, plots, leaderboard);

        getServer().getPluginManager().registerEvents(new PlotProtectionListener(plots, game), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(plots, game), this);
        getServer().getPluginManager().registerEvents(new RatingListener(game), this);

        PlotCommand plotCommand = new PlotCommand(plots, game);
        PluginCommand command = Objects.requireNonNull(getCommand("plot"));
        command.setExecutor(plotCommand);
        command.setTabCompleter(plotCommand);
        Objects.requireNonNull(getCommand("build")).setExecutor(new BuildCommand(plots, game, leaderboard));
        Objects.requireNonNull(getCommand("ende")).setExecutor(new EndCommand(game));
        Objects.requireNonNull(getCommand("bestenliste")).setExecutor(new LeaderboardCommand(leaderboard));
    }

    @Override
    public void onDisable() {
        if (game != null) {
            game.shutdown();
        }
    }

    private World createWorld() {
        World world = new WorldCreator(WORLD_NAME)
                .generator(new PlotWorldGenerator())
                .generateStructures(false)
                .createWorld();
        Objects.requireNonNull(world, "Bauwelt konnte nicht erstellt werden");

        world.setDifficulty(Difficulty.PEACEFUL);
        world.setStorm(false);
        world.setTime(6000);
        world.setGameRule(GameRules.PVP, false);
        world.setGameRule(GameRules.ADVANCE_TIME, false);
        world.setGameRule(GameRules.ADVANCE_WEATHER, false);
        world.setGameRule(GameRules.SPAWN_MOBS, false);
        world.setGameRule(GameRules.MOB_GRIEFING, false);
        world.setGameRule(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, 0);
        world.setGameRule(GameRules.RANDOM_TICK_SPEED, 0);
        world.setGameRule(GameRules.SHOW_ADVANCEMENT_MESSAGES, false);
        world.setGameRule(GameRules.SPAWN_MOBS, false);
        world.setGameRule(GameRules.SPAWN_MONSTERS, false);
        LobbyBox.build(world);
        return world;
    }
}
