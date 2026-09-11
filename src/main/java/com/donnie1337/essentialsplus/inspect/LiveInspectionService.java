package com.donnie1337.essentialsplus.inspect;

import com.donnie1337.essentialsplus.bau.BauHolder;
import com.donnie1337.essentialsplus.bau.BauService;
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
    }

    private void refreshAll() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Inventory inspection = viewer.getOpenInventory().getTopInventory();
            if (!(inspection.getHolder() instanceof InspectHolder holder)) continue;

            Player target = Bukkit.getPlayer(holder.target());
            if (target == null) {
                viewer.closeInventory();
                continue;
            }

            switch (holder.type()) {
                case PLAYER -> refreshPlayerInventory(target, inspection);
                case ENDER_CHEST -> refreshEnderChest(target, inspection);
                case BAU -> refreshBau(target, inspection);
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

    private void refreshBau(Player target, Inventory inspection) {
        Inventory source = findOpenBau(target.getUniqueId());
        if (source == null) {
            source = bauService.createInventory(target);
        }

        for (int slot = 0; slot < BauService.SIZE; slot++) {
            setIfChanged(inspection, slot, source.getItem(slot));
        }
    }

    private Inventory findOpenBau(UUID target) {
        Player player = Bukkit.getPlayer(target);
        if (player == null) return null;

        Inventory top = player.getOpenInventory().getTopInventory();
        return top.getHolder() instanceof BauHolder holder && holder.owner().equals(target)
                ? top
                : null;
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
