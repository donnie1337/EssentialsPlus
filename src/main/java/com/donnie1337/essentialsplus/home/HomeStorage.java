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
                if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
                    throw new IOException("Não foi possível criar a pasta de dados do plugin.");
                }
                if (!file.createNewFile()) {
                    throw new IOException("Não foi possível criar homes.yml.");
                }
            } catch (IOException exception) {
                throw new IllegalStateException("Não foi possível criar homes.yml", exception);
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
            if (!data.isConfigurationSection(path)) continue;

            String worldName = data.getString(path + ".world", "");
            World world = plugin.getServer().getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("Home '" + name + "' ignorada: mundo '" + worldName + "' não está carregado.");
                continue;
            }

            double x = data.getDouble(path + ".x", Double.NaN);
            double y = data.getDouble(path + ".y", Double.NaN);
            double z = data.getDouble(path + ".z", Double.NaN);
            float yaw = (float) data.getDouble(path + ".yaw", 0.0D);
            float pitch = (float) data.getDouble(path + ".pitch", 0.0D);

            if (!valid(x, y, z) || !Float.isFinite(yaw) || !Float.isFinite(pitch)) {
                plugin.getLogger().warning("Home '" + name + "' ignorada: localização inválida em homes.yml.");
                continue;
            }

            homes.put(name, new Home(name, new Location(world, x, y, z, yaw, pitch)));
        }
        return Collections.unmodifiableMap(homes);
    }

    public boolean saveHome(UUID playerId, Home home) {
        String path = "players." + playerId + "." + home.name();
        Location location = home.location();
        if (!validLocation(location)) {
            plugin.getLogger().warning("Não foi possível salvar a home '" + home.name() + "': localização inválida.");
            return false;
        }

        Object previous = data.get(path);
        writeLocation(path, location);

        if (save()) return true;

        data.set(path, previous);
        return false;
    }

    public boolean deleteHome(UUID playerId, String name) {
        String path = "players." + playerId + "." + name;
        Object previous = data.get(path);
        data.set(path, null);

        if (save()) return true;

        data.set(path, previous);
        return false;
    }

    public boolean renameHome(UUID playerId, String oldName, Home renamed) {
        String oldPath = "players." + playerId + "." + oldName;
        String newPath = "players." + playerId + "." + renamed.name();
        Location location = renamed.location();
        if (!validLocation(location)) {
            plugin.getLogger().warning("Não foi possível renomear a home '" + oldName + "': localização inválida.");
            return false;
        }

        Object previousOld = data.get(oldPath);
        Object previousNew = data.get(newPath);

        writeLocation(newPath, location);
        data.set(oldPath, null);

        if (save()) return true;

        data.set(oldPath, previousOld);
        data.set(newPath, previousNew);
        return false;
    }

    private void writeLocation(String path, Location location) {
        data.set(path + ".world", location.getWorld().getName());
        data.set(path + ".x", location.getX());
        data.set(path + ".y", location.getY());
        data.set(path + ".z", location.getZ());
        data.set(path + ".yaw", location.getYaw());
        data.set(path + ".pitch", location.getPitch());
    }

    private boolean validLocation(Location location) {
        return location != null
                && location.getWorld() != null
                && valid(location.getX(), location.getY(), location.getZ())
                && Float.isFinite(location.getYaw())
                && Float.isFinite(location.getPitch());
    }

    private boolean valid(double x, double y, double z) {
        return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z);
    }

    private boolean save() {
        try {
            data.save(file);
            return true;
        } catch (IOException exception) {
            plugin.getLogger().severe("Não foi possível salvar homes.yml: " + exception.getMessage());
            return false;
        }
    }
}
