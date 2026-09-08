package com.donnie1337.essentialsplus.home;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class HomeService {
    private final JavaPlugin plugin;
    private final HomeStorage storage;
    private final Map<UUID, Map<String, Home>> cache = new LinkedHashMap<>();

    public HomeService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.storage = new HomeStorage(plugin);
    }

    public Map<String, Home> homes(Player player) {
        return cache.computeIfAbsent(player.getUniqueId(), storage::loadHomes);
    }

    public Home getHome(Player player, String name) {
        return homes(player).get(name.toLowerCase());
    }

    public boolean setHome(Player player, String name) {
        String normalized = normalize(name);
        Map<String, Home> homes = new LinkedHashMap<>(homes(player));
        homes.put(normalized, new Home(normalized, player.getLocation()));
        cache.put(player.getUniqueId(), homes);
        storage.saveHome(player.getUniqueId(), homes.get(normalized));
        return true;
    }

    public boolean deleteHome(Player player, String name) {
        String normalized = normalize(name);
        Map<String, Home> homes = new LinkedHashMap<>(homes(player));
        if (homes.remove(normalized) == null) return false;
        cache.put(player.getUniqueId(), homes);
        storage.deleteHome(player.getUniqueId(), normalized);
        return true;
    }

    public void unload(Player player) {
        cache.remove(player.getUniqueId());
    }

    private String normalize(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Nome da home invalido.");
        String normalized = name.toLowerCase();
        if (!normalized.matches("[a-z0-9_-]{1,32}")) throw new IllegalArgumentException("Nome da home invalido.");
        return normalized;
    }
}
