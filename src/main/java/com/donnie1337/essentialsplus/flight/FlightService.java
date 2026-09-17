package com.donnie1337.essentialsplus.flight;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class FlightService implements Listener {
    private static final double START_SPEED = 0.06D;
    private static final double END_SPEED = 0.85D;
    private static final double ACCELERATION_DISTANCE = 32.0D;

    private final JavaPlugin plugin;
    private final Set<UUID> gliding = new HashSet<>();

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

        if (player.getAllowFlight()) {
            disable(player);
            return false;
        }

        enable(player);
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

        if (!player.isOnGround() && !player.isInWater()) {
            gliding.add(player.getUniqueId());
            Vector velocity = player.getVelocity();
            player.setVelocity(new Vector(velocity.getX(), -START_SPEED, velocity.getZ()));
        } else {
            gliding.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!gliding.contains(uuid)) return;

        if (player.isOnGround() || player.isInWater() || player.isInLava()) {
            stopGlide(player);
            return;
        }

        Location location = player.getLocation();
        double distance = distanceToGround(location);
        double progress = 1.0D - Math.min(1.0D, distance / ACCELERATION_DISTANCE);
        double speed = START_SPEED + (END_SPEED - START_SPEED) * progress;

        Vector velocity = player.getVelocity();
        player.setFallDistance(0.0F);
        player.setVelocity(new Vector(velocity.getX(), -speed, velocity.getZ()));
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && event.getCause() == EntityDamageEvent.DamageCause.FALL
                && gliding.contains(player.getUniqueId())) {
            event.setCancelled(true);
            player.setFallDistance(0.0F);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        gliding.remove(event.getPlayer().getUniqueId());
    }

    private double distanceToGround(Location location) {
        RayTraceResult result = location.getWorld().rayTraceBlocks(
                location.clone().add(0.0D, 0.05D, 0.0D),
                new Vector(0.0D, -1.0D, 0.0D),
                ACCELERATION_DISTANCE + 2.0D,
                FluidCollisionMode.NEVER,
                true
        );

        if (result == null || result.getHitPosition() == null) {
            return ACCELERATION_DISTANCE;
        }

        return Math.max(0.0D, location.toVector().distance(result.getHitPosition().toVector()));
    }

    private void stopGlide(Player player) {
        gliding.remove(player.getUniqueId());
        player.setFallDistance(0.0F);
    }
}
