package com.donnie1337.essentialsplus.bau;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

public final class BauListener implements Listener {
    private final BauService bauService;

    public BauListener(BauService bauService) {
        this.bauService = bauService;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof BauHolder) {
            bauService.save(event.getInventory());
        }
    }
}
