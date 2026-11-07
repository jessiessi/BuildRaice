package com.jessiessi.buildraice;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Der Spielablauf, Runde für Runde: Box, /build, Bewertung, dann geleerte Plots und zurück
 * in die Box. Das geht beliebig oft, bis /ende das Spiel beendet. Die Punkte und die Plot-Vergabe
 * werden gespeichert, eine laufende Runde nicht: Ein Serverneustart bricht sie ab.
 */
public final class GameManager {

    /** So lange bleibt das Ergebnis am Sieger-Plot stehen, bevor es zurück in die Box geht. */
    private static final int RESULT_SECONDS = 15;

    private final JavaPlugin plugin;
    private final PlotManager plots;
    private final Leaderboard leaderboard;
    private final Deque<Runnable> pendingWork = new ArrayDeque<>();
    private final BukkitTask workTask;

    private boolean wallsUp;
    private boolean resetting;
    private String item;
    private long durationMillis;
    private long endsAtMillis;
    private BossBar bossBar;
    private BukkitTask roundTask;
    private BukkitTask resultTask;
    private RatingTour tour;

    public GameManager(JavaPlugin plugin, PlotManager plots, Leaderboard leaderboard) {
        this.plugin = plugin;
        this.plots = plots;
        this.leaderboard = leaderboard;
        // Wände und Plot-Reihen werden portionsweise bearbeitet, damit der Server flüssig bleibt.
        this.workTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Runnable work = pendingWork.poll();
            if (work != null) {
                work.run();
            }
        }, 1L, 1L);
    }

    /** True, sobald /build Plots vergeben hat. Davor und beim Leeren warten alle in der Box. */
    public boolean isStarted() {
        return plots.hasPlots() && !resetting;
    }

    public boolean isResetting() {
        return resetting;
    }

    /** True von /build bis die Plots wieder leer sind. */
    public boolean isRoundInProgress() {
        return bossBar != null || tour != null || resetting;
    }

    /** Während der Bewertung und beim Leeren wird nicht gebaut. */
    public boolean isBuildingLocked() {
        return tour != null || resetting;
    }

    /** Die aktuelle Bewertung oder null, solange gebaut werden darf. */
    public RatingTour tour() {
        return tour;
    }

    /** Gibt dem Spieler seinen Plot und bringt ihn dorthin. */
    public void assignPlot(Player player) {
        Plot plot = plots.claim(player.getUniqueId());
        if (wallsUp) {
            queueWall(plot);
        }
        leaderboard.ensure(player.getUniqueId());
        leaderboard.showTo(player);
        player.teleport(plots.homeOf(plot));
        player.setGameMode(GameMode.CREATIVE);
        player.sendMessage(Component.text("Das ist dein Plot, nur hier kannst du bauen.", NamedTextColor.GREEN));
    }

    /** Bringt den Spieler in die Box, ohne Zuschauer-Rechte und ohne Items aus der Runde. */
    public void sendToBox(Player player) {
        player.teleport(LobbyBox.spawn(plots.world()));
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(false);
        player.setInvulnerable(false);
        player.getInventory().clear();
        Bukkit.getOnlinePlayers().forEach(other -> {
            other.showPlayer(plugin, player);
            player.showPlayer(plugin, other);
        });
    }

    public void startRound(String item, int minutes) {
        this.item = item;
        this.durationMillis = minutes * 60_000L;
        this.endsAtMillis = System.currentTimeMillis() + durationMillis;

        wallsUp = true;
        plots.allPlots().forEach(this::queueWall);

        bossBar = BossBar.bossBar(title(), 1f, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
        Bukkit.getOnlinePlayers().forEach(player -> player.showBossBar(bossBar));
        roundTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickRound, 20L, 20L);
        Bukkit.getServer().sendMessage(Component.text(
                "Baut: " + item + "! Ihr habt " + minutes + " Minuten.", NamedTextColor.GREEN));
    }

    /** Bringt Spieler, die während der Runde oder Bewertung joinen, auf den aktuellen Stand. */
    public void onPlayerJoined(Player player) {
        leaderboard.showTo(player);
        if (tour != null) {
            tour.addViewer(player);
            return;
        }
        player.setInvulnerable(false);
        Bukkit.getOnlinePlayers().forEach(other -> {
            other.showPlayer(plugin, player);
            player.showPlayer(plugin, other);
        });
        if (bossBar != null) {
            player.showBossBar(bossBar);
        }
    }

    /** /ende: bricht eine laufende Runde ab, leert die Plots und öffnet allen die Bestenliste. */
    public void endGame(CommandSender sender) {
        if (!plots.hasPlots() && (leaderboard.isEmpty() || leaderboard.isFinished())) {
            sender.sendMessage(Component.text("Es läuft gerade kein Spiel.", NamedTextColor.RED));
            return;
        }
        resetPlotsAndGoToBox();
        leaderboard.finish();
        Bukkit.getServer().sendMessage(Component.text("Das Spiel ist beendet! ", NamedTextColor.GOLD)
                .append(Component.text("[Bestenliste öffnen]", NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.runCommand("/bestenliste"))
                        .hoverEvent(HoverEvent.showText(Component.text("Klicken, um die Bestenliste zu sehen")))));
        Bukkit.getServer().showDialog(leaderboard.dialog());
    }

    public void shutdown() {
        workTask.cancel();
        stopRound();
        stopTour();
    }

    private void tickRound() {
        long remaining = endsAtMillis - System.currentTimeMillis();
        if (remaining <= 0) {
            finishRound();
            return;
        }
        bossBar.name(title());
        bossBar.progress(Math.clamp((float) remaining / durationMillis, 0f, 1f));
    }

    private void finishRound() {
        stopRound();
        Bukkit.getServer().sendMessage(Component.text(
                "Die Zeit ist um! Die Runde ist beendet.", NamedTextColor.GOLD));
        queueWallRemoval();
        tour = new RatingTour(plugin, plots, leaderboard, this::onTourFinished);
        tour.start();
    }

    private void onTourFinished() {
        Bukkit.getServer().sendMessage(Component.text(
                "In " + RESULT_SECONDS + " Sekunden geht es zurück in die Box.", NamedTextColor.GOLD));
        resultTask = Bukkit.getScheduler().runTaskLater(plugin, this::resetPlotsAndGoToBox, RESULT_SECONDS * 20L);
    }

    /** Beendet alles Laufende, holt alle in die Box und leert danach die Plots. */
    private void resetPlotsAndGoToBox() {
        stopRound();
        stopTour();
        if (resetting) {
            return;
        }
        resetting = true;
        Bukkit.getOnlinePlayers().forEach(this::sendToBox);

        queueWallRemoval();
        for (Plot plot : plots.allPlots()) {
            for (int row = 0; row < PlotGrid.PLOT_SIZE; row++) {
                int currentRow = row;
                pendingWork.add(() -> PlotCleaner.clearRow(plots.world(), plot, currentRow));
            }
            pendingWork.add(() -> PlotCleaner.removeEntities(plots.world(), plot));
        }
        pendingWork.add(() -> {
            plots.clearAll();
            resetting = false;
            Bukkit.getServer().sendMessage(Component.text(
                    "Die Plots sind leer. Die nächste Runde beginnt mit /build.", NamedTextColor.GREEN));
        });
    }

    private void stopRound() {
        if (roundTask != null) {
            roundTask.cancel();
            roundTask = null;
        }
        if (bossBar != null) {
            Bukkit.getOnlinePlayers().forEach(player -> player.hideBossBar(bossBar));
            bossBar = null;
        }
    }

    private void stopTour() {
        if (resultTask != null) {
            resultTask.cancel();
            resultTask = null;
        }
        if (tour != null) {
            tour.close();
            tour = null;
        }
    }

    private void queueWall(Plot plot) {
        pendingWork.add(() -> PlotWall.build(plots.world(), plot));
    }

    private void queueWallRemoval() {
        if (wallsUp) {
            wallsUp = false;
            plots.allPlots().forEach(plot -> pendingWork.add(() -> PlotWall.remove(plots.world(), plot)));
        }
    }

    private Component title() {
        long seconds = Math.max(0, (endsAtMillis - System.currentTimeMillis() + 999) / 1000);
        return Component.text(item + " – " + String.format("%02d:%02d", seconds / 60, seconds % 60),
                NamedTextColor.WHITE);
    }
}
