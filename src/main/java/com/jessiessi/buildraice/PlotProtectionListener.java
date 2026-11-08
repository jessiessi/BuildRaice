package com.jessiessi.buildraice;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.SpongeAbsorbEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.List;

/**
 * Schützt die Bauwelt: Spieler dürfen nur auf ihrem eigenen Plot etwas verändern,
 * und auch Wasser, Kolben oder Bäume kommen nicht über den Plotrand hinaus.
 */
public final class PlotProtectionListener implements Listener {

    private static final Component NOT_YOUR_PLOT =
            Component.text("Du kannst nur auf deinem eigenen Plot bauen.", NamedTextColor.RED);

    private static final Component ROUND_OVER =
            Component.text("Die Runde ist beendet, du kannst nicht mehr bauen.", NamedTextColor.RED);

    private final PlotManager plots;
    private final GameManager game;

    public PlotProtectionListener(PlotManager plots, GameManager game) {
        this.plots = plots;
        this.game = game;
    }

    // --- Hilfsmethoden ---

    private boolean inPlotWorld(Location location) {
        return plots.world().equals(location.getWorld());
    }

    private boolean ownsBlock(Player player, Block block) {
        return ownsPosition(player, block.getX(), block.getZ()) && block.getY() > block.getWorld().getMinHeight();
    }

    private boolean ownsPosition(Player player, int blockX, int blockZ) {
        if (game.isBuildingLocked()) {
            return false;
        }
        Plot plot = PlotGrid.plotAt(blockX, blockZ);
        return plot != null && plot.equals(plots.plotOf(player.getUniqueId()));
    }

    private boolean ownsLocation(Player player, Location location) {
        return ownsPosition(player, location.getBlockX(), location.getBlockZ());
    }

    /** Beide Blöcke liegen auf demselben Plot. Wegblöcke zählen nie als Plot. */
    private static boolean samePlot(Block first, Block second) {
        Plot plot = PlotGrid.plotAt(first.getX(), first.getZ());
        return plot != null && plot.equals(PlotGrid.plotAt(second.getX(), second.getZ()));
    }

    private void deny(Cancellable event, Player player) {
        event.setCancelled(true);
        player.sendActionBar(game.isBuildingLocked() ? ROUND_OVER : NOT_YOUR_PLOT);
    }

    private static Player playerBehind(Entity entity) {
        if (entity instanceof Player player) {
            return player;
        }
        if (entity instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            return shooter instanceof Player player ? player : null;
        }
        return null;
    }

    // --- Spieler verändern Blöcke ---

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (inPlotWorld(event.getBlock().getLocation()) && !ownsBlock(event.getPlayer(), event.getBlock())) {
            deny(event, event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (!inPlotWorld(block.getLocation())) {
            return;
        }
        Player player = event.getPlayer();
        boolean allowed = ownsBlock(player, block);
        // Das Kopfteil eines Betts steht nicht im Event, darf aber ebenfalls nicht über den Rand ragen.
        if (block.getBlockData() instanceof Bed bed && bed.getPart() == Bed.Part.FOOT) {
            allowed &= ownsBlock(player, block.getRelative(bed.getFacing()));
        }
        if (event instanceof BlockMultiPlaceEvent multi) {
            for (BlockState state : multi.getReplacedBlockStates()) {
                allowed &= ownsBlock(player, state.getBlock());
            }
        }
        if (!allowed) {
            deny(event, player);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (inPlotWorld(event.getBlock().getLocation()) && !ownsBlock(event.getPlayer(), event.getBlock())) {
            deny(event, event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (inPlotWorld(event.getBlock().getLocation()) && !ownsBlock(event.getPlayer(), event.getBlock())) {
            deny(event, event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent event) {
        Block clicked = event.getClickedBlock();
        if (clicked == null || !inPlotWorld(clicked.getLocation())) {
            return;
        }
        Player player = event.getPlayer();
        if (ownsBlock(player, clicked) || opensDoorOnViewedPlot(event, clicked)) {
            return;
        }
        // Gegen den Wegrand klicken, um auf dem eigenen Plot zu bauen, bleibt erlaubt.
        // Der Weg selbst reagiert nicht auf Klicks, das Platzieren prüfen die Events oben.
        boolean buildsIntoOwnPlot = event.getAction() == Action.RIGHT_CLICK_BLOCK
                && ownsBlock(player, clicked.getRelative(event.getBlockFace()));
        if (!buildsIntoOwnPlot) {
            event.setCancelled(true);
        }
    }

    /** Bei der Bewertung lassen sich Türen, Falltüren und Zauntore auf dem besuchten Plot öffnen. */
    private boolean opensDoorOnViewedPlot(PlayerInteractEvent event, Block clicked) {
        Plot viewed = game.viewedPlot();
        return viewed != null
                && event.getAction() == Action.RIGHT_CLICK_BLOCK
                && clicked.getBlockData() instanceof Openable
                && viewed.equals(PlotGrid.plotAt(clicked.getX(), clicked.getZ()));
    }

    // --- Spieler verändern Entities ---

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityPlace(EntityPlaceEvent event) {
        Player player = event.getPlayer();
        if (inPlotWorld(event.getEntity().getLocation())
                && (player == null || !ownsLocation(player, event.getEntity().getLocation()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        Player player = event.getPlayer();
        if (inPlotWorld(event.getEntity().getLocation())
                && (player == null || !ownsLocation(player, event.getEntity().getLocation()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        guardEntity(event, event.getRemover(), event.getEntity());
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        guardEntity(event, event.getDamager(), event.getEntity());
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onVehicleDamage(VehicleDamageEvent event) {
        guardEntity(event, event.getAttacker(), event.getVehicle());
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onVehicleDestroy(VehicleDestroyEvent event) {
        guardEntity(event, event.getAttacker(), event.getVehicle());
    }

    private void guardEntity(Cancellable event, Entity actor, Entity target) {
        if (!inPlotWorld(target.getLocation())) {
            return;
        }
        Player player = actor == null ? null : playerBehind(actor);
        if (player == null || !ownsLocation(player, target.getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        guardEntity(event, event.getPlayer(), event.getRightClicked());
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        guardEntity(event, event.getPlayer(), event.getRightClicked());
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onArmorStand(PlayerArmorStandManipulateEvent event) {
        guardEntity(event, event.getPlayer(), event.getRightClicked());
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        Block hit = event.getHitBlock();
        if (hit == null || !inPlotWorld(hit.getLocation())) {
            return;
        }
        Player player = playerBehind(event.getEntity());
        if (player == null || !ownsBlock(player, hit)) {
            event.setCancelled(true);
        }
    }

    // --- Nichts wächst, fließt oder wird über den Plotrand geschoben ---

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onFlow(BlockFromToEvent event) {
        if (inPlotWorld(event.getBlock().getLocation()) && !samePlot(event.getBlock(), event.getToBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent event) {
        if (inPlotWorld(event.getBlock().getLocation()) && !samePlot(event.getSource(), event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (inPlotWorld(event.getBlock().getLocation())
                && pistonCrossesPlot(event.getBlock(), event.getBlocks(), event.getDirection())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (inPlotWorld(event.getBlock().getLocation())
                && pistonCrossesPlot(event.getBlock(), event.getBlocks(), event.getDirection())) {
            event.setCancelled(true);
        }
    }

    private static boolean pistonCrossesPlot(Block piston, List<Block> moved, BlockFace direction) {
        if (!samePlot(piston, piston.getRelative(direction))) {
            return true;
        }
        for (Block block : moved) {
            if (!samePlot(piston, block) || !samePlot(piston, block.getRelative(direction))) {
                return true;
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDispense(BlockDispenseEvent event) {
        Block block = event.getBlock();
        if (inPlotWorld(block.getLocation())
                && block.getBlockData() instanceof Directional directional
                && !samePlot(block, block.getRelative(directional.getFacing()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onStructureGrow(StructureGrowEvent event) {
        if (inPlotWorld(event.getLocation())) {
            keepOnPlotOf(event.getLocation().getBlock(), event.getBlocks());
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onFertilize(BlockFertilizeEvent event) {
        if (inPlotWorld(event.getBlock().getLocation())) {
            keepOnPlotOf(event.getBlock(), event.getBlocks());
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSpongeAbsorb(SpongeAbsorbEvent event) {
        if (inPlotWorld(event.getBlock().getLocation())) {
            keepOnPlotOf(event.getBlock(), event.getBlocks());
        }
    }

    private static void keepOnPlotOf(Block origin, List<BlockState> changes) {
        changes.removeIf(state -> !samePlot(origin, state.getBlock()));
    }

    // --- Explosionen, Mobs und Portale gibt es in der Bauwelt nicht ---

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (inPlotWorld(event.getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (inPlotWorld(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (inPlotWorld(event.getLocation()) && !(event.getEntity() instanceof ArmorStand)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPortalCreate(PortalCreateEvent event) {
        if (plots.world().equals(event.getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (inPlotWorld(event.getFrom())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityPortal(EntityPortalEvent event) {
        if (inPlotWorld(event.getFrom())) {
            event.setCancelled(true);
        }
    }
}
