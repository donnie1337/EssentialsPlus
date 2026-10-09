package com.donnie1337.essentialsplus.flight;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class FlightService implements Listener {
    private static final double INITIAL_GLIDE_SPEED = 0.06D;
    private static final double MAX_GLIDE_SPEED = 0.85D;
    private static final double GLIDE_DISTANCE = 32.0D;

    private final JavaPlugin plugin;
    private final Set<UUID> gliding = new HashSet<>();
    private final Set<UUID> terrainFlight = new HashSet<>();
    private final Set<UUID> insideOwnTerrain = new HashSet<>();
    private Object terrenosManager;
    private Method terrenosFindMethod;
    private Method terrenoOwnerIdMethod;

    public FlightService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean canFly(Player player) {
        return player != null && player.hasPermission("essentialsplus.fly");
    }

    public boolean isFlying(Player player) {
        return player != null && player.getAllowFlight() && player.isFlying();
    }

    public boolean toggle(Player player) {
        if (player == null) return false;

        // O /fly agora é vinculado ao próprio terreno. Fora dele, a permissão
        // continua existindo, mas o voo não pode permanecer ativo.
        if (!isInsideOwnTerrain(player)) {
            if (terrainFlight.contains(player.getUniqueId())) {
                disableTerrainFlight(player);
            }
            return false;
        }

        if (player.getAllowFlight()) {
            disableTerrainFlight(player);
            return false;
        }

        enableTerrainFlight(player);
        return true;
    }

    public void enable(Player player) {
        if (player == null) return;
        gliding.remove(player.getUniqueId());
        player.setFallDistance(0.0F);
        player.setAllowFlight(true);
        player.setFlying(true);
    }

    public void disable(Player player) {
        if (player == null) return;

        player.setFlying(false);
        player.setAllowFlight(false);
        player.setFallDistance(0.0F);

        if (!player.isOnGround() && !isInFluid(player)) {
            gliding.add(player.getUniqueId());
            Vector velocity = player.getVelocity();
            player.setVelocity(new Vector(velocity.getX(), -INITIAL_GLIDE_SPEED, velocity.getZ()));
        } else {
            gliding.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> syncTerrainFlight(player));
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (event.getTo() != null
                && (event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ()
                || event.getFrom().getWorld() != event.getTo().getWorld())) {
            syncTerrainFlight(player);
        }

        if (!gliding.contains(uuid)) return;

        if (player.isOnGround() || isInFluid(player)) {
            stopGlide(player);
            return;
        }

        double distance = distanceToGround(player.getLocation());
        double progress = 1.0D - Math.min(1.0D, distance / GLIDE_DISTANCE);
        double verticalSpeed = INITIAL_GLIDE_SPEED
                + (MAX_GLIDE_SPEED - INITIAL_GLIDE_SPEED) * progress;

        Vector velocity = player.getVelocity();
        player.setVelocity(new Vector(velocity.getX(), -verticalSpeed, velocity.getZ()));
        player.setFallDistance(0.0F);
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player
                && event.getCause() == EntityDamageEvent.DamageCause.FALL
                && gliding.contains(player.getUniqueId())) {
            event.setCancelled(true);
            player.setFallDistance(0.0F);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        gliding.remove(uuid);
        terrainFlight.remove(uuid);
        insideOwnTerrain.remove(uuid);
    }

    private void syncTerrainFlight(Player player) {
        if (player == null || !player.isOnline()) return;

        UUID uuid = player.getUniqueId();
        boolean ownTerrain = canFly(player) && isInsideOwnTerrain(player);
        boolean wasInside = insideOwnTerrain.contains(uuid);

        if (ownTerrain) {
            insideOwnTerrain.add(uuid);
            if (!wasInside || !terrainFlight.contains(uuid)) {
                enableTerrainFlight(player);
                player.sendMessage("§a[Voo] §rModo de voo ativado.");
            }
            return;
        }

        insideOwnTerrain.remove(uuid);
        if (terrainFlight.contains(uuid)) {
            disableTerrainFlight(player);
            if (canFly(player)) {
                player.sendMessage("§c[Voo] §rModo de voo desativado.");
            }
        }
    }

    private void enableTerrainFlight(Player player) {
        if (player == null || !canFly(player)) return;
        terrainFlight.add(player.getUniqueId());
        enable(player);
    }

    private void disableTerrainFlight(Player player) {
        if (player == null) return;
        terrainFlight.remove(player.getUniqueId());

        // Criativo e espectador possuem voo nativo; não retiramos esse estado.
        switch (player.getGameMode()) {
            case CREATIVE, SPECTATOR -> {
                gliding.remove(player.getUniqueId());
                player.setFallDistance(0.0F);
            }
            default -> disable(player);
        }
    }

    private boolean isInsideOwnTerrain(Player player) {
        if (player == null || !player.isOnline()) return false;

        try {
            var terrenosPlus = plugin.getServer().getPluginManager().getPlugin("TerrenosPlus");
            if (terrenosPlus == null || !terrenosPlus.isEnabled()) {
                clearTerrenosBridge();
                return false;
            }

            if (terrenosManager == null || terrenosFindMethod == null) {
                Method getManager = terrenosPlus.getClass().getMethod("getTerrenoManager");
                terrenosManager = getManager.invoke(terrenosPlus);
                if (terrenosManager == null) return false;
                terrenosFindMethod = terrenosManager.getClass().getMethod("find", Location.class);
            }

            Object result = terrenosFindMethod.invoke(terrenosManager, player.getLocation());
            if (!(result instanceof Optional<?> optional) || optional.isEmpty()) return false;

            Object terrain = optional.get();
            if (terrenoOwnerIdMethod == null
                    || !terrenoOwnerIdMethod.getDeclaringClass().isAssignableFrom(terrain.getClass())) {
                terrenoOwnerIdMethod = terrain.getClass().getMethod("ownerId");
            }

            Object owner = terrenoOwnerIdMethod.invoke(terrain);
            return player.getUniqueId().equals(owner);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            clearTerrenosBridge();
            return false;
        }
    }

    private void clearTerrenosBridge() {
        terrenosManager = null;
        terrenosFindMethod = null;
        terrenoOwnerIdMethod = null;
    }

    private boolean isInFluid(Player player) {
        Material feet = player.getLocation().getBlock().getType();
        Material head = player.getEyeLocation().getBlock().getType();
        return feet == Material.WATER || feet == Material.LAVA
                || head == Material.WATER || head == Material.LAVA;
    }

    private double distanceToGround(Location location) {
        if (location.getWorld() == null) return GLIDE_DISTANCE;

        RayTraceResult result = location.getWorld().rayTraceBlocks(
                location.clone().add(0.0D, 0.05D, 0.0D),
                new Vector(0.0D, -1.0D, 0.0D),
                GLIDE_DISTANCE,
                FluidCollisionMode.NEVER,
                true
        );

        if (result == null || result.getHitPosition() == null) {
            return GLIDE_DISTANCE;
        }

        return Math.max(0.0D, location.getY() - result.getHitPosition().getY());
    }

    private void stopGlide(Player player) {
        gliding.remove(player.getUniqueId());
        player.setFallDistance(0.0F);
    }
}
