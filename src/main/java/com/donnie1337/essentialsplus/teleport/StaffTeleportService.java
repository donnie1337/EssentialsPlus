package com.donnie1337.essentialsplus.teleport;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class StaffTeleportService {
    private final JavaPlugin plugin;

    public StaffTeleportService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean teleport(Player player, Player target) {
        if (player == null || target == null || !player.isOnline() || !target.isOnline()) return false;
        return player.teleport(target.getLocation());
    }

    public boolean teleport(Player player, Player destination, Player target) {
        if (player == null || destination == null || target == null) return false;
        if (!destination.isOnline() || !target.isOnline()) return false;
        return target.teleport(destination.getLocation());
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
