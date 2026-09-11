package com.donnie1337.essentialsplus.inspect;

import com.donnie1337.essentialsplus.bau.BauService;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class LiveInspectionService {
    private final JavaPlugin plugin;
    private final BauService bauService;
    private final Map<UUID, Inventory> activeInspections = new HashMap<>();
    private final Map<UUID, Inventory> lastBauSources = new HashMap<>();
    private BukkitTask task;

    public LiveInspectionService(JavaPlugin plugin, BauService bauService) {
        this.plugin = plugin;
        this.bauService = bauService;
    }

    public void start() {
        if (task != null) task.cancel();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshAll, 1L, 2L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        activeInspections.clear();
        lastBauSources.clear();
    }

    public void register(Player viewer, Inventory inventory) {
        if (!(inventory.getHolder() instanceof InspectHolder)) return;
        UUID viewerId = viewer.getUniqueId();
        activeInspections.put(viewerId, inventory);
        lastBauSources.remove(viewerId);
    }

    public void unregister(Player viewer) {
        UUID viewerId = viewer.getUniqueId();
        activeInspections.remove(viewerId);
        lastBauSources.remove(viewerId);
    }

    private void refreshAll() {
        Iterator<Map.Entry<UUID, Inventory>> iterator = activeInspections.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Inventory> entry = iterator.next();
            UUID viewerId = entry.getKey();
            Player viewer = Bukkit.getPlayer(viewerId);
            Inventory inspection = entry.getValue();

            if (viewer == null || viewer.getOpenInventory().getTopInventory() != inspection) {
                iterator.remove();
                lastBauSources.remove(viewerId);
                continue;
            }

            if (!(inspection.getHolder() instanceof InspectHolder holder)) {
                iterator.remove();
                lastBauSources.remove(viewerId);
                continue;
            }

            Player target = Bukkit.getPlayer(holder.target());
            if (target == null) {
                iterator.remove();
                lastBauSources.remove(viewerId);
                viewer.closeInventory();
                continue;
            }

            switch (holder.type()) {
                case PLAYER -> refreshPlayerInventory(target, inspection);
                case ENDER_CHEST -> refreshEnderChest(target, inspection);
                case BAU -> refreshBau(viewerId, target, inspection);
            }
        }
    }

    private void refreshPlayerInventory(Player target, Inventory inspection) {
        for (int slot = 0; slot < 36; slot++) {
            setIfChanged(inspection, slot, target.getInventory().getItem(slot));
        }

        setIfChanged(inspection, 45, target.getInventory().getHelmet());
        setIfChanged(inspection, 46, target.getInventory().getChestplate());
        setIfChanged(inspection, 47, target.getInventory().getLeggings());
        setIfChanged(inspection, 48, target.getInventory().getBoots());
        setIfChanged(inspection, 49, target.getInventory().getItemInOffHand());
    }

    private void refreshEnderChest(Player target, Inventory inspection) {
        Inventory source = target.getEnderChest();
        for (int slot = 0; slot < 27; slot++) {
            setIfChanged(inspection, slot, source.getItem(slot));
        }
    }

    private void refreshBau(UUID viewerId, Player target, Inventory inspection) {
        Inventory source = bauService.getActiveBau(target.getUniqueId());
        Inventory previousSource = lastBauSources.get(viewerId);

        if (source != null) {
            for (int slot = 0; slot < BauService.SIZE; slot++) {
                setIfChanged(inspection, slot, source.getItem(slot));
            }
            lastBauSources.put(viewerId, source);
            return;
        }

        if (previousSource != null) {
            bauService.loadSaved(target.getUniqueId(), inspection);
            lastBauSources.remove(viewerId);
        }
    }

    private void setIfChanged(Inventory inventory, int slot, ItemStack source) {
        ItemStack current = inventory.getItem(slot);
        if (sameItem(current, source)) return;
        inventory.setItem(slot, source == null || source.getType().isAir() ? null : source.clone());
    }

    private boolean sameItem(ItemStack first, ItemStack second) {
        if (first == null || first.getType().isAir()) return second == null || second.getType().isAir();
        if (second == null || second.getType().isAir()) return false;
        return first.isSimilar(second) && first.getAmount() == second.getAmount();
    }
}
