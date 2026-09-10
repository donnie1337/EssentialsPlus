package com.donnie1337.essentialsplus.home;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Locale;
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

    public boolean enabled() {
        return plugin.getConfig().getBoolean("homes.enabled", true);
    }

    public Map<String, Home> homes(Player player) {
        return cache.computeIfAbsent(player.getUniqueId(), storage::loadHomes);
    }

    public Home getHome(Player player, String name) {
        return homes(player).get(normalize(name));
    }

    public boolean canSetHome(Player player, String name) {
        String normalized = normalize(name);
        Map<String, Home> homes = homes(player);
        if (homes.containsKey(normalized)) return true;
        return getHomeLimit(player) < 0 || homes.size() < getHomeLimit(player);
    }

    public int getHomeLimit(Player player) {
        return plugin.getConfig().getInt("homes.limits.default", 1);
    }

    public boolean setHome(Player player, String name) {
        UUID playerId = player.getUniqueId();
        String normalized = normalize(name);
        Map<String, Home> current = homes(player);
        if (!current.containsKey(normalized) && getHomeLimit(player) >= 0 && current.size() >= getHomeLimit(player)) return false;

        Home home = new Home(normalized, player.getLocation());
        if (!storage.saveHome(playerId, home)) return false;

        Map<String, Home> updated = new LinkedHashMap<>(current);
        updated.put(normalized, home);
        cache.put(playerId, updated);
        return true;
    }

    public boolean deleteHome(Player player, String name) {
        UUID playerId = player.getUniqueId();
        String normalized = normalize(name);
        Map<String, Home> current = homes(player);
        if (!current.containsKey(normalized)) return false;

        if (!storage.deleteHome(playerId, normalized)) return false;

        Map<String, Home> updated = new LinkedHashMap<>(current);
        updated.remove(normalized);
        cache.put(playerId, updated);
        return true;
    }

    public boolean renameHome(Player player, String oldName, String newName) {
        UUID playerId = player.getUniqueId();
        String oldNormalized = normalize(oldName);
        String newNormalized = normalize(newName);
        Map<String, Home> current = homes(player);
        Home home = current.get(oldNormalized);
        if (home == null || current.containsKey(newNormalized)) return false;

        Home renamed = new Home(newNormalized, home.location());
        if (!storage.renameHome(playerId, oldNormalized, renamed)) return false;

        Map<String, Home> updated = new LinkedHashMap<>(current);
        updated.remove(oldNormalized);
        updated.put(newNormalized, renamed);
        cache.put(playerId, updated);
        return true;
    }

    public JavaPlugin plugin() {
        return plugin;
    }

    public void unload(Player player) {
        cache.remove(player.getUniqueId());
    }

    private String normalize(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Nome da home invalido.");
        String normalized = name.toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9_-]{1,32}")) throw new IllegalArgumentException("Nome da home invalido.");
        return normalized;
    }
}
