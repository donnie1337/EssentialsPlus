package com.donnie1337.essentialsplus.chat;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.lang.reflect.Method;

public final class TellCustomClickListener implements Listener {
    private final Plugin plugin;

    public TellCustomClickListener(Plugin plugin) {
        this.plugin = plugin;
    }

    public void register(PluginManager pluginManager) {
        registerEventType(pluginManager, "org.bukkit.event.player.PlayerCustomClickEvent");
        registerEventType(pluginManager, "io.papermc.paper.event.player.PlayerCustomClickEvent");
        plugin.getLogger().info("Botão de cancelamento do /tell registrado usando custom click.");
    }

    @SuppressWarnings("unchecked")
    private void registerEventType(PluginManager pluginManager, String className) {
        try {
            Class<?> rawType = Class.forName(className);
            if (!Event.class.isAssignableFrom(rawType)) return;
            Class<? extends Event> eventType = (Class<? extends Event>) rawType;
            EventExecutor executor = (listener, event) -> handleCustomClick(event);
            pluginManager.registerEvent(eventType, this, EventPriority.NORMAL, executor, plugin, true);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Servidor sem a API de custom click não registra o listener.
        }
    }

    private void handleCustomClick(Event event) {
        try {
            String id = invokeIdentifier(event);
            if (!TellListener.CANCEL_BUTTON_ID.equalsIgnoreCase(id)) return;

            Player player = invokePlayer(event);
            if (player == null || !player.hasPermission("essentialsplus.tell")) return;

            TellListener.CancelResult result = TellListener.cancelPendingTell(player);
            switch (result) {
                case CANCELLED -> player.sendMessage(TellListener.message("messages.tell.cancelled"));
                case ALREADY_SENT -> player.sendMessage(TellListener.message("messages.tell.already-sent"));
                case ALREADY_CANCELLED -> player.sendMessage(TellListener.message("messages.tell.already-cancelled"));
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Não interromper o processamento normal de chat por incompatibilidade de API.
        }
    }

    private String invokeIdentifier(Event event) throws ReflectiveOperationException {
        Object identifier;
        try {
            identifier = event.getClass().getMethod("getIdentifier").invoke(event);
        } catch (NoSuchMethodException ignored) {
            identifier = event.getClass().getMethod("getId").invoke(event);
        }
        return identifier == null ? "" : identifier.toString();
    }

    private Player invokePlayer(Event event) throws ReflectiveOperationException {
        try {
            Method getPlayer = event.getClass().getMethod("getPlayer");
            Object player = getPlayer.invoke(event);
            return player instanceof Player ? (Player) player : null;
        } catch (NoSuchMethodException ignored) {
            Method commonConnection = event.getClass().getMethod("getCommonConnection");
            Object connection = commonConnection.invoke(event);
            if (connection == null) return null;
            Method getPlayer = connection.getClass().getMethod("getPlayer");
            Object player = getPlayer.invoke(connection);
            return player instanceof Player ? (Player) player : null;
        }
    }
}
