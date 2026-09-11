package com.donnie1337.essentialsplus.bau;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

public final class BauListener implements Listener {
    private final BauService bauService;

    public BauListener(BauService bauService) {
        this.bauService = bauService;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof BauHolder holder) {
            bauService.closeBauIfMatches(holder.owner(), event.getInventory());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        bauService.closeBau(player.getUniqueId());
    }
}
