package com.donnie1337.essentialsplus.teleport;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class StaffTeleportService {
    private final JavaPlugin plugin;

    public StaffTeleportService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Teleports the subject to the destination.
     */
    public boolean teleport(Player subject, Player destination) {
        if (destination == null || !destination.isOnline()) return false;
        return teleport(subject, destination.getLocation());
    }

    public boolean teleport(Player subject, Location destination) {
        if (subject == null || destination == null || destination.getWorld() == null
                || !subject.isOnline()) return false;
        Location location = destination.clone();
        var passengers = new java.util.ArrayList<>(subject.getPassengers());
        passengers.forEach(subject::removePassenger);
        try {
            if (!location.getChunk().isLoaded()) location.getChunk().load(true);
            boolean success = subject.teleport(location);
            for (var passenger : passengers) {
                if (passenger.isValid() && passenger.getWorld().equals(subject.getWorld())) subject.addPassenger(passenger);
            }
            return success;
        } catch (RuntimeException exception) {
            for (var passenger : passengers) {
                if (passenger.isValid() && passenger.getWorld().equals(subject.getWorld())) subject.addPassenger(passenger);
            }
            plugin.getLogger().warning("Falha no /tp para o mundo '" + location.getWorld().getName() + "': " + exception.getMessage());
            return false;
        }
    }

    public Player findPlayer(String name) {
        if (name == null || name.isBlank()) return null;
        Player exact = Bukkit.getPlayerExact(name);
        if (exact != null) return exact;
        return Bukkit.getPlayer(name);
    }

    public JavaPlugin plugin() {
        return plugin;
    }
}
