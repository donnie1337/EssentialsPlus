package com.donnie1337.essentialsplus.inspect;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public final class InspectListener implements Listener {
    private final LiveInspectionService liveInspectionService;

    public InspectListener(LiveInspectionService liveInspectionService) {
        this.liveInspectionService = liveInspectionService;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof InspectHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof InspectHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (event.getSource().getHolder() instanceof InspectHolder
                || event.getDestination().getHolder() instanceof InspectHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryPickupItem(InventoryPickupItemEvent event) {
        if (event.getInventory().getHolder() instanceof InspectHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (isInspecting(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        if (isInspecting(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player
                && event.getInventory().getHolder() instanceof InspectHolder) {
            liveInspectionService.unregister(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        liveInspectionService.unregister(event.getPlayer());
    }

    private boolean isInspecting(Player player) {
        return player.getOpenInventory().getTopInventory().getHolder() instanceof InspectHolder;
    }
}
