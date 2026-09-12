package com.donnie1337.essentialsplus.bau;

import com.donnie1337.essentialsplus.inspect.InspectHolder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class BauService {
    public static final int SIZE = 54;
    public static final String TITLE = "§8Baú Estendido";

    private final JavaPlugin plugin;
    private final File file;
    private final File tempFile;
    private final File backupFile;
    private final YamlConfiguration data;
    private final Map<UUID, Inventory> activeBaus = new HashMap<>();

    public BauService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "bau.yml");
        this.tempFile = new File(plugin.getDataFolder(), "bau.yml.tmp");
        this.backupFile = new File(plugin.getDataFolder(), "bau.yml.bak");
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public Inventory createInventory(Player player) {
        UUID uuid = player.getUniqueId();
        Inventory existing = activeBaus.get(uuid);
        if (existing != null) return existing;

        BauHolder holder = new BauHolder(uuid);
        Inventory inventory = Bukkit.createInventory(holder, SIZE, TITLE);
        holder.setInventory(inventory);
        load(uuid, inventory);
        activeBaus.put(uuid, inventory);
        return inventory;
    }

    public boolean hasActiveBau(UUID uuid) {
        return activeBaus.containsKey(uuid);
    }

    public Inventory getActiveBau(UUID uuid) {
        return activeBaus.get(uuid);
    }

    public void closeBau(UUID uuid) {
        Inventory inventory = activeBaus.remove(uuid);
        if (inventory != null) save(inventory);
    }

    public void closeBauIfMatches(UUID uuid, Inventory inventory) {
        Inventory active = activeBaus.get(uuid);
        if (active != inventory) return;
        activeBaus.remove(uuid);
        save(inventory);
    }

    public Inventory createInspectionInventory(UUID uuid, String targetName) {
        InspectHolder holder = new InspectHolder(uuid, InspectHolder.Type.BAU);
        Inventory inventory = Bukkit.createInventory(holder, SIZE, "§8Baú de " + targetName);
        holder.setInventory(inventory);
        load(uuid, inventory);
        return inventory;
    }

    public void loadSaved(UUID uuid, Inventory inventory) {
        load(uuid, inventory);
    }

    public void save(Inventory inventory) {
        if (!(inventory.getHolder() instanceof BauHolder holder)) return;
        writeInventory(holder.owner(), inventory);
        saveFile();
    }

    /**
     * Atualiza todos os baús abertos na memória e grava o YAML apenas uma vez.
     * Isso evita reescrever bau.yml dezenas de vezes no mesmo autosave.
     */
    public void saveOpenBaus() {
        if (activeBaus.isEmpty()) return;
        for (Inventory inventory : activeBaus.values()) {
            if (inventory.getHolder() instanceof BauHolder holder) {
                writeInventory(holder.owner(), inventory);
            }
        }
        saveFile();
    }

    public void closeAllBaus() {
        if (activeBaus.isEmpty()) return;
        for (Inventory inventory : activeBaus.values()) {
            if (inventory.getHolder() instanceof BauHolder holder) {
                writeInventory(holder.owner(), inventory);
            }
        }
        saveFile();
        activeBaus.clear();
    }

    private void writeInventory(UUID owner, Inventory inventory) {
        String path = "players." + owner + ".slots";
        for (int slot = 0; slot < SIZE; slot++) {
            ItemStack item = inventory.getItem(slot);
            data.set(path + "." + slot, item == null || item.getType().isAir() ? null : item.clone());
        }
    }

    private void load(UUID uuid, Inventory inventory) {
        String path = "players." + uuid + ".slots.";
        for (int slot = 0; slot < SIZE; slot++) {
            inventory.setItem(slot, null);
            ItemStack item = data.getItemStack(path + slot);
            if (item != null) inventory.setItem(slot, item.clone());
        }
    }

    private void saveFile() {
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Não foi possível criar a pasta de dados do EssentialsPlus.");
                return;
            }

            data.save(tempFile);

            if (file.exists()) {
                try {
                    Files.copy(file.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException backupException) {
                    plugin.getLogger().warning("Não foi possível criar o backup dos baús estendidos: " + backupException.getMessage());
                }
            }

            try {
                Files.move(tempFile.toPath(), file.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveException) {
                Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("Não foi possível salvar os baús estendidos: " + exception.getMessage());
        }
    }
}
