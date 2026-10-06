package com.donnie1337.essentialsplus.spawn;

import com.donnie1337.essentialsplus.EssentialsPlus;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;

public final class SpawnService {

    private final EssentialsPlus plugin;
    private final File file;
    private Location spawn;

    public SpawnService(EssentialsPlus plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "spawn.yml");
        load();
    }

    public void load() {
        spawn = null;
        if (!file.exists()) {
            return;
        }

        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        String worldName = data.getString("spawn.world");
        if (worldName == null || worldName.isBlank()) {
            return;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("O mundo do spawn salvo não está carregado: " + worldName);
            return;
        }

        spawn = new Location(
                world,
                data.getDouble("spawn.x"),
                data.getDouble("spawn.y"),
                data.getDouble("spawn.z"),
                (float) data.getDouble("spawn.yaw"),
                (float) data.getDouble("spawn.pitch")
        );
    }

    public void setSpawn(Location location) {
        if (location.getWorld() == null) {
            throw new IllegalArgumentException("A localização do spawn precisa possuir um mundo.");
        }

        spawn = location.clone();

        YamlConfiguration data = new YamlConfiguration();
        data.set("spawn.world", location.getWorld().getName());
        data.set("spawn.x", location.getX());
        data.set("spawn.y", location.getY());
        data.set("spawn.z", location.getZ());
        data.set("spawn.yaw", location.getYaw());
        data.set("spawn.pitch", location.getPitch());

        try {
            file.getParentFile().mkdirs();
            data.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Não foi possível salvar spawn.yml", exception);
            throw new IllegalStateException("Falha ao salvar o spawn.", exception);
        }
    }

    public Optional<Location> getSpawn() {
        if (spawn == null) {
            return Optional.empty();
        }

        World currentWorld = Bukkit.getWorld(spawn.getWorld().getName());
        if (currentWorld == null) {
            return Optional.empty();
        }

        Location current = spawn.clone();
        current.setWorld(currentWorld);
        return Optional.of(current);
    }
}
