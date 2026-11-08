package com.jessiessi.buildraice;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Die Bewertung nach einer Runde: Alle werden gemeinsam in zufälliger Reihenfolge auf die bebauten
 * Plots gebracht und stimmen mit den fünf Blöcken in der Mitte der Hotbar ab, also mit den Tasten 3-7.
 * Wem der Plot gehört, bleibt bis zum Ergebnis geheim. Die Punkte kommen auf das Gesamtkonto in der Sidebar.
 *
 * <p>Echter Spectator-Modus geht hier nicht: Dort öffnen die Zahlentasten das Spectator-Menü und der
 * Server erfährt nichts davon. Zuschauer sind deshalb im Adventure-Modus, fliegen und sind unverwundbar.
 * Abgestimmt wird mit den Tasten 3-7 oder dem Mausrad. Den Block in der Hand sehen nur die Spieler
 * selbst, die anderen bekommen eine leere Hand angezeigt (siehe RatingListener).
 */
public final class RatingTour {

    private static final int SECONDS_PER_PLOT = 30;
    /** Die Blöcke liegen mittig in den Slots 2-6 (Tasten 3-7). */
    private static final int FIRST_RATING_SLOT = 2;
    /** Der leere Slot, der immer gewählt bleibt. */
    private static final int NEUTRAL_SLOT = 8;
    /** Feuerwerk über dem Sieger-Plot: so viele Raketen, alle halbe Sekunde eine. */
    private static final int FIREWORK_ROCKETS = 20;
    private static final List<Color> FIREWORK_COLORS = List.of(Color.RED, Color.ORANGE, Color.YELLOW,
            Color.LIME, Color.AQUA, Color.BLUE, Color.FUCHSIA, Color.WHITE);
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
    private final Spectators spectators;
    private final Runnable onFinished;
    private final List<Plot> tour;
    private final Map<Plot, Integer> roundPoints = new HashMap<>();
    private final Map<Plot, Integer> gameTotals = new HashMap<>();
    private final Map<UUID, Integer> votes = new HashMap<>();
    /** Bisher vergebene Punkte je Besitzer, damit /stop sie zurücknehmen kann. */
    private final Map<UUID, Integer> awarded = new HashMap<>();
    private final BossBar bossBar;

    private int index = -1;
    private int secondsLeft;
    private int rocketsLeft;
    private BukkitTask task;
    private BukkitTask fireworkTask;
    private Plot winner;

    /** @param toVisit die bebauten Plots, mindestens einer */
    public RatingTour(JavaPlugin plugin, PlotManager plots, Leaderboard leaderboard, Spectators spectators,
                      List<Plot> toVisit, Runnable onFinished) {
        this.plugin = plugin;
        this.plots = plots;
        this.leaderboard = leaderboard;
        this.spectators = spectators;
        this.onFinished = onFinished;
        this.tour = new ArrayList<>(toVisit);
        Collections.shuffle(this.tour);
        this.bossBar = BossBar.bossBar(Component.empty(), 1f, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);

        for (Plot plot : tour) {
            roundPoints.put(plot, 0);
            leaderboard.ensure(plots.ownerOf(plot));
        }
    }

    public void start() {
        explain();
        Bukkit.getOnlinePlayers().forEach(this::prepareViewer);
        nextPlot();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public boolean isRunning() {
        return winner == null;
    }

    /** Der Plot, auf dem gerade alle stehen: der bewertete oder am Ende der Sieger-Plot. */
    public Plot currentPlot() {
        if (!isRunning()) {
            return winner;
        }
        return index >= 0 && index < tour.size() ? tour.get(index) : null;
    }

    /** Für Spieler, die während der Bewertung oder der Siegerehrung dazukommen. */
    public void addViewer(Player player) {
        prepareViewer(player);
        Plot plot = currentPlot();
        if (plot != null) {
            player.teleport(viewpointOf(plot));
            player.setFlying(true);
        }
    }

    /** /skip: der Plot bekommt keine Punkte, die bisherigen Stimmen verfallen, weiter geht es zum nächsten. */
    public boolean skip() {
        if (!isRunning() || index < 0 || index >= tour.size()) {
            return false;
        }
        votes.clear();
        commitVotes();
        nextPlot();
        return true;
    }

    /** Eine Hotbar-Taste wurde gedrückt. Bis zum Ablauf der Zeit zählt die letzte Wahl. */
    public void vote(Player player, int slot) {
        int rating = slot - FIRST_RATING_SLOT;
        if (!isRunning() || index < 0 || rating < 0 || rating >= RATINGS.size() || spectators.contains(player)) {
            return;
        }
        UUID voter = player.getUniqueId();
        UUID owner = plots.ownerOf(tour.get(index));
        // Wer allein mitspielt, darf den eigenen Plot bewerten, sonst könnte es gar keine Punkte geben.
        long players = Bukkit.getOnlinePlayers().stream().filter(online -> !spectators.contains(online)).count();
        if (voter.equals(owner) && players > 1) {
            player.sendActionBar(Component.text("Deinen eigenen Plot kannst du nicht bewerten.", NamedTextColor.RED));
            return;
        }
        int points = rating + 1;
        votes.put(voter, points);
        plugin.getLogger().info(player.getName() + " bewertet den Plot von " + Leaderboard.nameOf(owner)
                + " mit " + points);
        player.sendActionBar(Component.text("Deine Wertung: " + RATINGS.get(rating).label() + " – "
                + Leaderboard.pointsText(points), RATINGS.get(rating).color()));
        markChoice(player, rating);
    }

    /** /stop während der Bewertung: Die Runde zählt nicht, schon vergebene Punkte werden abgezogen. */
    public void revertPoints() {
        if (isRunning()) {
            awarded.forEach((owner, points) -> leaderboard.add(owner, -points));
            awarded.clear();
        }
    }

    /** Stoppt die Tour und nimmt den Spielern die Abstimm-Items und die Bossbar. */
    public void close() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (fireworkTask != null) {
            fireworkTask.cancel();
            fireworkTask = null;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.hideBossBar(bossBar);
            player.getInventory().clear();
        }
    }

    private void explain() {
        Bukkit.getServer().sendMessage(Component.text("--- Bewertung ---", NamedTextColor.GOLD));
        Bukkit.getServer().sendMessage(Component.text(
                "Ihr besucht alle bebauten Plots gemeinsam in zufälliger Reihenfolge. Wem der Plot gehört, "
                        + "erfahrt ihr erst beim Ergebnis.", NamedTextColor.YELLOW));
        Component keys = Component.text("Stimme mit den Tasten 3-7 ab: ", NamedTextColor.YELLOW);
        for (int i = 0; i < RATINGS.size(); i++) {
            Rating rating = RATINGS.get(i);
            keys = keys.append(Component.text((i > 0 ? ", " : "") + (FIRST_RATING_SLOT + i + 1) + " = "
                    + rating.label() + " (" + (i + 1) + ")", rating.color()));
        }
        Bukkit.getServer().sendMessage(keys);
        Bukkit.getServer().sendMessage(Component.text(
                "Pro Plot hast du " + SECONDS_PER_PLOT + " Sekunden, deine letzte Wahl zählt. Deinen eigenen Plot "
                        + "kannst du nicht bewerten. Türen kannst du öffnen.", NamedTextColor.YELLOW));
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
            if (!spectators.contains(player)) {
                player.setFlying(true);
                giveRatingItems(player, -1);
            }
        }
    }

    private void commitVotes() {
        Plot plot = tour.get(index);
        int points = votes.values().stream().mapToInt(Integer::intValue).sum();
        roundPoints.put(plot, points);
        gameTotals.put(plot, leaderboard.add(plots.ownerOf(plot), points));
        awarded.merge(plots.ownerOf(plot), points, Integer::sum);
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
        celebrate(winner);
        onFinished.run();
    }

    /** Feuerwerk an zufälligen Stellen über dem Sieger-Plot. */
    private void celebrate(Plot plot) {
        rocketsLeft = FIREWORK_ROCKETS;
        fireworkTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (rocketsLeft-- <= 0) {
                fireworkTask.cancel();
                fireworkTask = null;
                return;
            }
            launchRocket(plot);
        }, 0L, 10L);
    }

    private void launchRocket(Plot plot) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        World world = plots.world();
        int x = plot.minBlockX() + 2 + random.nextInt(PlotGrid.PLOT_SIZE - 4);
        int z = plot.minBlockZ() + 2 + random.nextInt(PlotGrid.PLOT_SIZE - 4);
        int y = Math.max(world.getHighestBlockYAt(x, z), PlotGrid.SURFACE_Y) + 1;
        FireworkEffect.Type[] types = FireworkEffect.Type.values();
        FireworkEffect effect = FireworkEffect.builder()
                .with(types[random.nextInt(types.length)])
                .withColor(randomColor(random), randomColor(random))
                .withFade(randomColor(random))
                .flicker(random.nextBoolean())
                .trail(true)
                .build();
        world.spawn(new Location(world, x + 0.5, y, z + 0.5), Firework.class, firework -> {
            FireworkMeta meta = firework.getFireworkMeta();
            meta.addEffect(effect);
            meta.setPower(1 + random.nextInt(2));
            firework.setFireworkMeta(meta);
        });
    }

    private static Color randomColor(ThreadLocalRandom random) {
        return FIREWORK_COLORS.get(random.nextInt(FIREWORK_COLORS.size()));
    }

    /** Zuschauer von /spectate bleiben im Spectator-Modus und bekommen keine Abstimm-Blöcke. */
    private void prepareViewer(Player player) {
        if (spectators.contains(player)) {
            player.setGameMode(GameMode.SPECTATOR);
            leaderboard.showTo(player);
            if (isRunning()) {
                player.showBossBar(bossBar);
            }
            return;
        }
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setInvulnerable(true);
        leaderboard.showTo(player);
        player.getInventory().clear();
        if (isRunning()) {
            giveRatingItems(player, -1);
            player.showBossBar(bossBar);
        }
    }

    /** Legt die fünf Blöcke in die Hotbar-Mitte und wählt den leeren Slot, damit die Hand leer ist. */
    private void giveRatingItems(Player player, int chosen) {
        markChoice(player, chosen);
        player.getInventory().setHeldItemSlot(NEUTRAL_SLOT);
    }

    /** Setzt die fünf Blöcke neu, die gewählte Wertung leuchtet. Der gewählte Slot bleibt, wie er ist. */
    private void markChoice(Player player, int chosen) {
        PlayerInventory inventory = player.getInventory();
        for (int rating = 0; rating < RATINGS.size(); rating++) {
            inventory.setItem(FIRST_RATING_SLOT + rating, ratingItem(rating, rating == chosen));
        }
    }

    private static ItemStack ratingItem(int index, boolean chosen) {
        Rating rating = RATINGS.get(index);
        ItemStack item = new ItemStack(rating.material());
        item.editMeta(meta -> {
            meta.displayName(Component.text(rating.label() + " – " + Leaderboard.pointsText(index + 1), rating.color())
                    .decoration(TextDecoration.ITALIC, false));
            if (chosen) {
                meta.setEnchantmentGlintOverride(true);
            }
        });
        return item;
    }

    private void updateBossBar() {
        bossBar.name(Component.text(String.format("Plot %d/%d – %02d:%02d",
                index + 1, tour.size(), secondsLeft / 60, secondsLeft % 60)));
        bossBar.progress((float) secondsLeft / SECONDS_PER_PLOT);
    }

    /** Standpunkt am vorderen Plotrand, oben auf dem, was dort steht, mit Blick über den Plot. */
    private Location viewpointOf(Plot plot) {
        World world = plots.world();
        int x = plot.minBlockX() + PlotGrid.PLOT_SIZE / 2;
        int z = plot.minBlockZ() + 1;
        int y = Math.min(Math.max(world.getHighestBlockYAt(x, z), PlotGrid.SURFACE_Y) + 1, PlotGrid.MAX_FLY_Y);
        return new Location(world, x + 0.5, y, z + 0.5, 0f, 20f);
    }
}
