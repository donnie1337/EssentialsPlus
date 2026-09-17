package com.donnie1337.essentialsplus.flight;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class FlightService implements Listener {
    private static final double INITIAL_GLIDE_SPEED = 0.06D;
    private static final double MAX_GLIDE_SPEED = 0.35D;

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

        if (!player.isOnGround() && !isInFluid(player)) {
            gliding.add(player.getUniqueId());

            Vector velocity = player.getVelocity();
            player.setVelocity(new Vector(velocity.getX(), -INITIAL_GLIDE_SPEED, velocity.getZ()));
        } else {
            gliding.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!gliding.contains(uuid)) return;

        if (player.isOnGround() || isInFluid(player)) {
            stopGlide(player);
            return;
        }

        Vector velocity = player.getVelocity();
        double verticalVelocity = Math.min(velocity.getY(), -INITIAL_GLIDE_SPEED);
        verticalVelocity = Math.max(verticalVelocity, -MAX_GLIDE_SPEED);

        // Preserve the player's natural horizontal movement. Only the vertical
        // velocity is limited so the player slowly descends instead of falling.
        player.setVelocity(new Vector(velocity.getX(), verticalVelocity, velocity.getZ()));
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
        gliding.remove(event.getPlayer().getUniqueId());
    }

    private boolean isInFluid(Player player) {
        Material feet = player.getLocation().getBlock().getType();
        Material head = player.getEyeLocation().getBlock().getType();
        return feet == Material.WATER || feet == Material.LAVA
                || head == Material.WATER || head == Material.LAVA;
    }

    private void stopGlide(Player player) {
        gliding.remove(player.getUniqueId());
        player.setFallDistance(0.0F);
    }
}
