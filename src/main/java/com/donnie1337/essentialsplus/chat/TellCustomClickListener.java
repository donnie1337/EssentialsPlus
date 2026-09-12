package com.donnie1337.essentialsplus.chat;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCustomClickEvent;
import org.bukkit.plugin.PluginManager;

public final class TellCustomClickListener implements Listener {

    private static final String CANCEL_BUTTON_ID = TellListener.CANCEL_BUTTON_ID;
    private final org.bukkit.plugin.Plugin plugin;

    public TellCustomClickListener(org.bukkit.plugin.Plugin plugin) {
        this.plugin = plugin;
    }

    public void register(PluginManager pluginManager) {
        pluginManager.registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCustomClick(PlayerCustomClickEvent event) {
        NamespacedKey identifier = event.getId();
        if (identifier == null || !CANCEL_BUTTON_ID.equals(identifier.toString())) return;

        Player player = event.getPlayer();
        TellListener.CancelResult result = TellListener.cancelPendingTell(player);

        switch (result) {
            case CANCELLED -> player.sendMessage(TellListener.message("messages.tell.cancelled"));
            case ALREADY_SENT -> player.sendMessage(TellListener.message("messages.tell.already-sent"));
            case ALREADY_CANCELLED -> player.sendMessage(TellListener.message("messages.tell.already-cancelled"));
        }
    }
}
