package com.donnie1337.essentialsplus.home;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class HomeStorage {
    private final JavaPlugin plugin;
    private final File file;
    private final YamlConfiguration data;

    public HomeStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "homes.yml");
        if (!file.exists()) {
            try {
                if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException exception) {
                throw new IllegalStateException("Nao foi possivel criar homes.yml", exception);
            }
        }
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public Map<String, Home> loadHomes(UUID playerId) {
        ConfigurationSection section = data.getConfigurationSection("players." + playerId);
        if (section == null) return Map.of();
        Map<String, Home> homes = new LinkedHashMap<>();
        for (String name : section.getKeys(false)) {
            String path = section.getCurrentPath() + "." + name;
            World world = plugin.getServer().getWorld(data.getString(path + ".world", ""));
            if (world == null) continue;
            Location location = new Location(world,
                    data.getDouble(path + ".x"),
                    data.getDouble(path + ".y"),
                    data.getDouble(path + ".z"),
                    (float) data.getDouble(path + ".yaw"),
                    (float) data.getDouble(path + ".pitch"));
            homes.put(name, new Home(name, location));
        }
        return Collections.unmodifiableMap(homes);
    }

    public void saveHome(UUID playerId, Home home) {
        String path = "players." + playerId + "." + home.name();
        Location location = home.location();
        data.set(path + ".world", location.getWorld().getName());
        data.set(path + ".x", location.getX());
        data.set(path + ".y", location.getY());
        data.set(path + ".z", location.getZ());
        data.set(path + ".yaw", location.getYaw());
        data.set(path + ".pitch", location.getPitch());
        save();
    }

    public void deleteHome(UUID playerId, String name) {
        data.set("players." + playerId + "." + name, null);
        save();
    }

    public void renameHome(UUID playerId, String oldName, Home renamed) {
        String oldPath = "players." + playerId + "." + oldName;
        String newPath = "players." + playerId + "." + renamed.name();
        Location location = renamed.location();
        data.set(newPath + ".world", location.getWorld().getName());
        data.set(newPath + ".x", location.getX());
        data.set(newPath + ".y", location.getY());
        data.set(newPath + ".z", location.getZ());
        data.set(newPath + ".yaw", location.getYaw());
        data.set(newPath + ".pitch", location.getPitch());
        data.set(oldPath, null);
        save();
    }

    private void save() {
        try {
            data.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Nao foi possivel salvar homes.yml: " + exception.getMessage());
        }
    }
}
