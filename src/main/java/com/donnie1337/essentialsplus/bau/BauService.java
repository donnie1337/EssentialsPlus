package com.donnie1337.essentialsplus.bau;

import com.donnie1337.essentialsplus.inspect.InspectHolder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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
    private final YamlConfiguration data;

    public BauService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "bau.yml");
        this.tempFile = new File(plugin.getDataFolder(), "bau.yml.tmp");
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public Inventory createInventory(Player player) {
        UUID uuid = player.getUniqueId();
        BauHolder holder = new BauHolder(uuid);
        Inventory inventory = Bukkit.createInventory(holder, SIZE, TITLE);
        holder.setInventory(inventory);
        load(uuid, inventory);
        return inventory;
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

        String path = "players." + holder.owner();
        for (int slot = 0; slot < SIZE; slot++) {
            ItemStack item = inventory.getItem(slot);
            data.set(path + ".slots." + slot, item == null || item.getType().isAir() ? null : item);
        }
        saveFile();
    }

    public void saveOpenBaus() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Inventory inventory = player.getOpenInventory().getTopInventory();
            if (inventory.getHolder() instanceof BauHolder) save(inventory);
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
