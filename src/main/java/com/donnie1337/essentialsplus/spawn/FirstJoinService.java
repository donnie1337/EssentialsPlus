package com.donnie1337.essentialsplus.spawn;

import com.donnie1337.essentialsplus.EssentialsPlus;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public final class FirstJoinService {

    private final EssentialsPlus plugin;
    private final File file;
    private final Set<UUID> knownPlayers = new HashSet<>();

    public FirstJoinService(EssentialsPlus plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "known-players.yml");
        load();
    }

    private void load() {
        knownPlayers.clear();
        if (!file.exists()) {
            return;
        }

        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        for (String raw : data.getStringList("players")) {
            try {
                knownPlayers.add(UUID.fromString(raw));
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("UUID inválido em known-players.yml: " + raw);
            }
        }
    }

    public boolean markAndCheckFirstJoin(UUID uuid) {
        if (!knownPlayers.add(uuid)) {
            return false;
        }

        save();
        return true;
    }

    private void save() {
        YamlConfiguration data = new YamlConfiguration();
        data.set("players", knownPlayers.stream().map(UUID::toString).sorted().toList());

        try {
            file.getParentFile().mkdirs();
            data.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Não foi possível salvar known-players.yml", exception);
        }
    }
}
