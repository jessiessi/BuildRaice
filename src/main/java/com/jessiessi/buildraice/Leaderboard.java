package com.jessiessi.buildraice;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Die Gesamtpunkte eines Spiels über alle Runden, vom ersten /build bis /ende. Sie stehen in der Sidebar
 * und in scores.yml, überstehen also einen Serverneustart. Die Liste bleibt nach /ende abrufbar,
 * bis das nächste Spiel beginnt.
 */
public final class Leaderboard {

    private static final int DIALOG_WIDTH = 300;

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, Integer> totals = new HashMap<>();

    private boolean finished;
    private Scoreboard scoreboard;
    private Objective objective;

    public Leaderboard(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "scores.yml");
        load();
    }

    public boolean isEmpty() {
        return totals.isEmpty();
    }

    public boolean isFinished() {
        return finished;
    }

    /** Nimmt den Spieler mit 0 Punkten auf, falls er noch nicht in der Liste steht. */
    public void ensure(UUID player) {
        add(player, 0);
    }

    /** Addiert Punkte und liefert die neue Gesamtpunktzahl. */
    public int add(UUID player, int points) {
        int total = totals.merge(player, points, Integer::sum);
        if (objective != null) {
            objective.getScore(nameOf(player)).setScore(total);
        }
        save();
        return total;
    }

    /** Zeigt die Sidebar, solange das Spiel läuft und es schon eine Wertung gibt. */
    public void showTo(Player player) {
        if (!finished && !totals.isEmpty()) {
            player.setScoreboard(sidebar());
        }
    }

    /** Beendet das Spiel: Die Sidebar verschwindet, die Liste bleibt per /bestenliste abrufbar. */
    public void finish() {
        finished = true;
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        Bukkit.getOnlinePlayers().forEach(player -> player.setScoreboard(main));
        scoreboard = null;
        objective = null;
        save();
    }

    /** Macht die Liste nach einem beendeten Spiel frei für das nächste. */
    public void resetIfFinished() {
        if (finished) {
            finished = false;
            totals.clear();
            save();
        }
    }

    /** Die Bestenliste als Dialog, der sich beim Spieler öffnet. */
    public Dialog dialog() {
        List<DialogBody> body = rankingLines().stream()
                .<DialogBody>map(line -> DialogBody.plainMessage(line, DIALOG_WIDTH))
                .toList();
        DialogBase base = DialogBase.builder(Component.text("Bestenliste", NamedTextColor.GOLD)).body(body).build();
        DialogType type = DialogType.notice(ActionButton.builder(Component.text("Schließen")).build());
        return Dialog.create(factory -> factory.empty().base(base).type(type));
    }

    /** Die Bestenliste als Textzeilen für die Konsole, die keinen Dialog anzeigen kann. */
    public List<Component> lines() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.text("--- Bestenliste ---", NamedTextColor.GOLD));
        lines.addAll(rankingLines());
        return lines;
    }

    /** Eine Zeile pro Spieler. Bei Punktgleichheit teilen sich die Spieler den Platz. */
    private List<Component> rankingLines() {
        List<Component> lines = new ArrayList<>();
        if (totals.isEmpty()) {
            lines.add(Component.text("Noch keine Punkte.", NamedTextColor.GRAY));
            return lines;
        }
        List<Map.Entry<UUID, Integer>> ranking = new ArrayList<>(totals.entrySet());
        ranking.sort(Map.Entry.<UUID, Integer>comparingByValue(Comparator.reverseOrder()));
        int place = 0;
        for (int i = 0; i < ranking.size(); i++) {
            int points = ranking.get(i).getValue();
            if (i == 0 || points != ranking.get(i - 1).getValue()) {
                place = i + 1;
            }
            lines.add(Component.text(
                    "Platz " + place + ": " + nameOf(ranking.get(i).getKey()) + " (" + pointsText(points) + ")",
                    place == 1 ? NamedTextColor.GREEN : NamedTextColor.WHITE));
        }
        return lines;
    }

    public static String pointsText(int points) {
        return points + (points == 1 ? " Punkt" : " Punkte");
    }

    public static String nameOf(UUID player) {
        String name = Bukkit.getOfflinePlayer(player).getName();
        return name != null ? name : player.toString().substring(0, 8);
    }

    private Scoreboard sidebar() {
        if (scoreboard == null) {
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            objective = scoreboard.registerNewObjective("punkte", Criteria.DUMMY,
                    Component.text("Punkte", NamedTextColor.GOLD));
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            totals.forEach((player, total) -> objective.getScore(nameOf(player)).setScore(total));
        }
        return scoreboard;
    }

    private void load() {
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        finished = yaml.getBoolean("finished");
        ConfigurationSection scores = yaml.getConfigurationSection("scores");
        if (scores == null) {
            return;
        }
        for (String key : scores.getKeys(false)) {
            try {
                totals.put(UUID.fromString(key), scores.getInt(key));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Ungültiger Eintrag in scores.yml wird ignoriert: " + key);
            }
        }
    }

    private void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("finished", finished);
        totals.forEach((player, total) -> yaml.set("scores." + player, total));
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "scores.yml konnte nicht gespeichert werden", e);
        }
    }
}
