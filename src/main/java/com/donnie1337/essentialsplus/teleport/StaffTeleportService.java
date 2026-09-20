package com.donnie1337.essentialsplus.teleport;

import org.bukkit.Bukkit;
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
        if (subject == null || destination == null || !subject.isOnline() || !destination.isOnline()) return false;
        var location = destination.getLocation().clone();
        try {
            if (!location.getChunk().isLoaded()) location.getChunk().load(true);
            return subject.teleport(location);
        } catch (RuntimeException exception) {
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
