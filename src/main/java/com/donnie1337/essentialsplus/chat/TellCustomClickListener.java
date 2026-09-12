package com.donnie1337.essentialsplus.chat;

import java.lang.reflect.Method;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.plugin.PluginManager;

public final class TellCustomClickListener implements Listener {

    private static final String CANCEL_BUTTON_ID = "essentialsplus:tell_cancel";
    private final Object plugin;

    public TellCustomClickListener(Object plugin) {
        this.plugin = plugin;
    }

    public void register(PluginManager pluginManager) {
        pluginManager.registerEvents(this, (org.bukkit.plugin.Plugin) plugin);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onCustomClick(PlayerEvent event) {
        if (!event.getClass().getName().equals("io.papermc.paper.event.player.PlayerCustomClickEvent")) return;

        try {
            Method getIdentifier = event.getClass().getMethod("getIdentifier");
            Object identifier = getIdentifier.invoke(event);
            if (identifier == null || !CANCEL_BUTTON_ID.equals(identifier.toString())) return;

            Player player = event.getPlayer();
            TellListener.CancelResult result = TellListener.cancelPendingMessage(player.getUniqueId());

            switch (result) {
                case CANCELLED -> player.sendMessage(TellListener.message("messages.tell.cancelled"));
                case ALREADY_SENT -> player.sendMessage(TellListener.message("messages.tell.already-sent"));
                case ALREADY_CANCELLED -> player.sendMessage(TellListener.message("messages.tell.already-cancelled"));
            }
        } catch (ReflectiveOperationException ignored) {
            // Custom click events are optional on unsupported Paper versions.
        }
    }
}
