package com.donnie1337.essentialsplus.chat;

import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;

public final class TellCustomClickListener implements Listener {

    private static final Key CANCEL_BUTTON_ID = Key.key(TellListener.CANCEL_BUTTON_ID);
    private final Object plugin;

    public TellCustomClickListener(Object plugin) {
        this.plugin = plugin;
    }

    public void register(PluginManager pluginManager) {
        pluginManager.registerEvents(this, (org.bukkit.plugin.Plugin) plugin);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCustomClick(PlayerCustomClickEvent event) {
        if (!CANCEL_BUTTON_ID.equals(event.getIdentifier())) return;
        if (!(event.getCommonConnection() instanceof PlayerGameConnection connection)) return;

        Player player = connection.getPlayer();
        TellListener.CancelResult result = TellListener.cancelPendingTell(player);

        switch (result) {
            case CANCELLED -> player.sendMessage(TellListener.message("messages.tell.cancelled"));
            case ALREADY_SENT -> player.sendMessage(TellListener.message("messages.tell.already-sent"));
            case ALREADY_CANCELLED -> player.sendMessage(TellListener.message("messages.tell.already-cancelled"));
        }
    }
}
