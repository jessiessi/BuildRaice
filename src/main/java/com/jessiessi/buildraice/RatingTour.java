package com.jessiessi.buildraice;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Die Bewertung nach einer Runde: Alle werden von Plot zu Plot geschickt und stimmen mit den fünf
 * Blöcken in der Mitte der Hotbar ab, also mit den Tasten 3-7. Die Punkte kommen auf das Gesamtkonto
 * in der Sidebar.
 *
 * <p>Echter Spectator-Modus geht hier nicht: Dort öffnen die Zahlentasten das Spectator-Menü und der
 * Server erfährt nichts davon. Zuschauer sind deshalb im Adventure-Modus, fliegen und sind unsichtbar.
 */
public final class RatingTour {

    private static final int SECONDS_PER_PLOT = 30;
    /** Die Blöcke liegen mittig in den Slots 2-6 (Tasten 3-7). */
    private static final int FIRST_RATING_SLOT = 2;
    /** Vor jedem Plot ist dieser leere Slot gewählt, sonst löst die Taste des aktuellen Slots nichts aus. */
    private static final int NEUTRAL_SLOT = 8;
    private static final List<Rating> RATINGS = List.of(
            new Rating("Schlecht", Material.RED_CONCRETE, NamedTextColor.RED),
            new Rating("Geht so", Material.ORANGE_CONCRETE, NamedTextColor.GOLD),
            new Rating("Ok", Material.YELLOW_CONCRETE, NamedTextColor.YELLOW),
            new Rating("Super", Material.LIME_CONCRETE, NamedTextColor.GREEN),
            new Rating("Wunderschön", Material.GREEN_CONCRETE, NamedTextColor.DARK_GREEN));

    private record Rating(String label, Material material, NamedTextColor color) {
    }

    private final JavaPlugin plugin;
    private final PlotManager plots;
    private final Leaderboard leaderboard;
    private final Runnable onFinished;
    private final List<Plot> tour;
    private final Map<Plot, Integer> roundPoints = new HashMap<>();
    private final Map<Plot, Integer> gameTotals = new HashMap<>();
    private final Map<UUID, Integer> votes = new HashMap<>();
    private final BossBar bossBar;

    private int index = -1;
    private int secondsLeft;
    private BukkitTask task;
    private Plot winner;

    public RatingTour(JavaPlugin plugin, PlotManager plots, Leaderboard leaderboard, Runnable onFinished) {
        this.plugin = plugin;
        this.plots = plots;
        this.leaderboard = leaderboard;
        this.onFinished = onFinished;
        this.tour = new ArrayList<>(plots.allPlots());
        this.tour.sort(Comparator.comparingInt(Plot::x).thenComparingInt(Plot::z));
        this.bossBar = BossBar.bossBar(Component.empty(), 1f, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);

        for (Plot plot : tour) {
            roundPoints.put(plot, 0);
            leaderboard.ensure(plots.ownerOf(plot));
        }
    }

    public void start() {
        Bukkit.getServer().sendMessage(Component.text(
                "Bewertung! Stimme mit den Tasten 3-7 ab: Taste 3 = schlecht (1 Punkt), Taste 7 = wunderschön (5 Punkte).", NamedTextColor.GOLD));
        Bukkit.getOnlinePlayers().forEach(this::prepareViewer);
        nextPlot();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public boolean isRunning() {
        return winner == null;
    }

    /** Für Spieler, die während der Bewertung oder der Siegerehrung dazukommen. */
    public void addViewer(Player player) {
        prepareViewer(player);
        player.teleport(viewpointOf(isRunning() ? tour.get(index) : winner));
        player.setFlying(true);
    }

    /** Eine Hotbar-Taste wurde gedrückt. Bis zum Ablauf der Zeit zählt die letzte Wahl. */
    public void vote(Player player, int slot) {
        int rating = slot - FIRST_RATING_SLOT;
        if (!isRunning() || rating < 0 || rating >= RATINGS.size()) {
            return;
        }
        UUID voter = player.getUniqueId();
        UUID owner = plots.ownerOf(tour.get(index));
        // Wer allein spielt, darf den eigenen Plot bewerten, sonst könnte es gar keine Punkte geben.
        if (voter.equals(owner) && tour.size() > 1) {
            player.sendActionBar(Component.text("Deinen eigenen Plot kannst du nicht bewerten.", NamedTextColor.RED));
            return;
        }
        int points = rating + 1;
        votes.put(voter, points);
        plugin.getLogger().info(player.getName() + " bewertet den Plot von " + Leaderboard.nameOf(owner)
                + " mit " + points);
        player.sendActionBar(Component.text("Deine Wertung: " + RATINGS.get(rating).label() + " – "
                + Leaderboard.pointsText(points), RATINGS.get(rating).color()));
    }

    /** Stoppt die Tour und nimmt den Spielern die Abstimm-Items und die Bossbar. */
    public void close() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.hideBossBar(bossBar);
            player.getInventory().clear();
        }
    }

    private void tick() {
        secondsLeft--;
        if (secondsLeft > 0) {
            updateBossBar();
            return;
        }
        commitVotes();
        nextPlot();
    }

    private void nextPlot() {
        index++;
        if (index >= tour.size()) {
            finish();
            return;
        }
        votes.clear();
        secondsLeft = SECONDS_PER_PLOT;
        updateBossBar();
        Location viewpoint = viewpointOf(tour.get(index));
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.teleport(viewpoint);
            player.setFlying(true);
            player.getInventory().setHeldItemSlot(NEUTRAL_SLOT);
        }
    }

    private void commitVotes() {
        Plot plot = tour.get(index);
        int points = votes.values().stream().mapToInt(Integer::intValue).sum();
        roundPoints.put(plot, points);
        gameTotals.put(plot, leaderboard.add(plots.ownerOf(plot), points));
    }

    private void finish() {
        task.cancel();
        task = null;

        List<Plot> ranking = new ArrayList<>(tour);
        ranking.sort(Comparator.comparingInt((Plot plot) -> roundPoints.get(plot)).reversed());
        winner = ranking.getFirst();

        Bukkit.getServer().sendMessage(Component.text("--- Ergebnis der Runde ---", NamedTextColor.GOLD));
        int place = 0;
        List<String> firstPlace = new ArrayList<>();
        for (int i = 0; i < ranking.size(); i++) {
            Plot plot = ranking.get(i);
            // Bei Punktgleichheit teilen sich die Spieler den Platz.
            if (i == 0 || !roundPoints.get(plot).equals(roundPoints.get(ranking.get(i - 1)))) {
                place = i + 1;
            }
            String name = Leaderboard.nameOf(plots.ownerOf(plot));
            if (place == 1) {
                firstPlace.add(name);
            }
            Bukkit.getServer().sendMessage(Component.text(
                    "Platz " + place + ": " + name + " (" + Leaderboard.pointsText(roundPoints.get(plot))
                            + ", insgesamt " + gameTotals.get(plot) + ")",
                    place == 1 ? NamedTextColor.GREEN : NamedTextColor.WHITE));
        }

        Title title = Title.title(
                Component.text("Platz 1", NamedTextColor.GOLD),
                Component.text(String.join(" & ", firstPlace), NamedTextColor.GREEN));
        Location viewpoint = viewpointOf(winner);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.hideBossBar(bossBar);
            player.getInventory().clear();
            player.teleport(viewpoint);
            player.setFlying(true);
            player.showTitle(title);
        }
        onFinished.run();
    }

    private void prepareViewer(Player player) {
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setInvulnerable(true);
        leaderboard.showTo(player);

        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        if (isRunning()) {
            for (int rating = 0; rating < RATINGS.size(); rating++) {
                inventory.setItem(FIRST_RATING_SLOT + rating, ratingItem(rating));
            }
            inventory.setHeldItemSlot(NEUTRAL_SLOT);
            player.showBossBar(bossBar);
        }
        for (Player other : Bukkit.getOnlinePlayers()) {
            player.hidePlayer(plugin, other);
            other.hidePlayer(plugin, player);
        }
    }

    private static ItemStack ratingItem(int index) {
        Rating rating = RATINGS.get(index);
        ItemStack item = new ItemStack(rating.material());
        item.editMeta(meta -> meta.displayName(Component.text(rating.label() + " – " + Leaderboard.pointsText(index + 1), rating.color())
                .decoration(TextDecoration.ITALIC, false)));
        return item;
    }

    private void updateBossBar() {
        String owner = Leaderboard.nameOf(plots.ownerOf(tour.get(index)));
        bossBar.name(Component.text(String.format("Plot von %s (%d/%d) – %02d:%02d",
                owner, index + 1, tour.size(), secondsLeft / 60, secondsLeft % 60)));
        bossBar.progress((float) secondsLeft / SECONDS_PER_PLOT);
    }

    /** Standpunkt über dem Weg vor dem Plot, mit Blick schräg nach unten auf den Plot. */
    private Location viewpointOf(Plot plot) {
        return new Location(
                plots.world(),
                plot.minBlockX() + PlotGrid.PLOT_SIZE / 2.0,
                PlotGrid.SURFACE_Y + 22,
                plot.minBlockZ() - PlotGrid.ROAD_SIZE / 2.0,
                0f,
                30f);
    }
}
