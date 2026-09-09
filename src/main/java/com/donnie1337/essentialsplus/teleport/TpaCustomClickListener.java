package com.donnie1337.essentialsplus.teleport;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class TpaCustomClickListener implements Listener {
    private static final String PERMISSION = "essentialsplus.tpa";

    private final Plugin plugin;
    private final TeleportService teleportService;
    private final ConcurrentMap<Integer, ButtonResult> results = new ConcurrentHashMap<>();

    public TpaCustomClickListener(Plugin plugin, TeleportService teleportService) {
        this.plugin = plugin;
        this.teleportService = teleportService;
    }

    public void register(PluginManager pluginManager) {
        registerEventType(pluginManager, "org.bukkit.event.player.PlayerCustomClickEvent");
        registerEventType(pluginManager, "io.papermc.paper.event.player.PlayerCustomClickEvent");
        plugin.getLogger().info("Botões de TPA registrados usando custom click, sem execução de comando.");
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
            // This server does not expose this custom-click API variant.
        }
    }

    private void handleCustomClick(Event event) {
        try {
            String id = invokeIdentifier(event);
            if (!TeleportService.TPA_BUTTON_KEY.asString().equalsIgnoreCase(id)) return;

            String payload = invokePayload(event);
            if (payload == null) return;
            String digits = payload.replaceAll("[^0-9-]", "");
            if (digits.isEmpty()) return;

            int token;
            try {
                token = Integer.parseInt(digits);
            } catch (NumberFormatException ignored) {
                return;
            }

            Player player = invokePlayer(event);
            if (player == null || !player.hasPermission(PERMISSION)) return;

            ButtonAction action = ButtonAction.find(token);
            if (action == null) return;

            ButtonResult previous = results.get(token);
            if (previous != null) {
                sendResultMessage(player, previous, action);
                return;
            }

            long timeout = plugin.getConfig().getLong("tpa.request-timeout-seconds", 20L) * 1000L;
            if (timeout > 0 && System.currentTimeMillis() - action.createdAt() >= timeout) {
                ButtonResult expiredResult = action.type() == ButtonActionType.CANCEL
                        ? ButtonResult.CANCEL_BLOCKED_EXPIRED
                        : ButtonResult.EXPIRED;
                results.put(token, expiredResult);
                sendResultMessage(player, expiredResult, action);
                return;
            }

            if (action.type() == ButtonActionType.CANCEL) {
                ButtonAction accept = ButtonAction.findRelated(token, ButtonActionType.ACCEPT, player.getUniqueId());
                ButtonAction deny = ButtonAction.findRelated(token, ButtonActionType.DENY, player.getUniqueId());

                if (accept != null && results.get(accept.token()) == ButtonResult.ACCEPTED) {
                    results.put(token, ButtonResult.CANCEL_BLOCKED_ACCEPTED);
                    sendResultMessage(player, ButtonResult.CANCEL_BLOCKED_ACCEPTED, action);
                    return;
                }
                if (deny != null && results.get(deny.token()) == ButtonResult.DENIED) {
                    results.put(token, ButtonResult.CANCEL_BLOCKED_DENIED);
                    sendResultMessage(player, ButtonResult.CANCEL_BLOCKED_DENIED, action);
                    return;
                }
            }

            boolean handled = teleportService.handleButton(player, token);
            if (!handled) return;

            ButtonResult result = switch (action.type()) {
                case ACCEPT -> ButtonResult.ACCEPTED;
                case DENY -> ButtonResult.DENIED;
                case CANCEL -> ButtonResult.CANCELLED;
            };
            results.put(token, result);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Ignore unsupported event representation without breaking chat processing.
        }
    }

    private void sendResultMessage(Player player, ButtonResult result, ButtonAction action) {
        String key = switch (result) {
            case ACCEPTED -> "request-already-accepted";
            case DENIED -> "request-already-denied";
            case EXPIRED -> "request-expired-cannot-respond";
            case CANCELLED -> "request-already-cancelled";
            case CANCEL_BLOCKED_ACCEPTED -> "request-cannot-cancel-accepted";
            case CANCEL_BLOCKED_DENIED -> "request-cannot-cancel-denied";
            case CANCEL_BLOCKED_EXPIRED -> "request-cannot-cancel-expired";
        };
        String raw = plugin.getConfig().getString("messages." + key, "");
        if (result == ButtonResult.CANCEL_BLOCKED_ACCEPTED || result == ButtonResult.CANCEL_BLOCKED_DENIED) {
            Player target = Bukkit.getPlayer(action.targetId());
            String name = target != null ? target.getDisplayName() : "";
            raw = raw.replace("{player}", name);
        }
        String prefix = plugin.getConfig().getString("messages.tpa-prefix", plugin.getConfig().getString("messages.prefix", ""));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + raw));
    }

    private String invokeIdentifier(Event event) throws ReflectiveOperationException {
        Object identifier;
        try {
            identifier = event.getClass().getMethod("getId").invoke(event);
        } catch (NoSuchMethodException ignored) {
            identifier = event.getClass().getMethod("getIdentifier").invoke(event);
        }
        return identifier == null ? "" : identifier.toString();
    }

    private String invokePayload(Event event) throws ReflectiveOperationException {
        try {
            Object data = event.getClass().getMethod("getData").invoke(event);
            return data == null ? null : data.toString();
        } catch (NoSuchMethodException ignored) {
            Object tag = event.getClass().getMethod("getTag").invoke(event);
            return tag == null ? null : tag.toString();
        }
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

    private enum ButtonResult {
        ACCEPTED,
        DENIED,
        EXPIRED,
        CANCELLED,
        CANCEL_BLOCKED_ACCEPTED,
        CANCEL_BLOCKED_DENIED,
        CANCEL_BLOCKED_EXPIRED
    }
}
