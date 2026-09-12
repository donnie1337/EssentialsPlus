package com.donnie1337.essentialsplus.teleport;

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

/** Processa os botões de TPA usando a API custom click do Spigot. */
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
        registerSpigotEvent(pluginManager);
        plugin.getLogger().info("Botões de TPA registrados usando custom click do Spigot.");
    }

    @SuppressWarnings("unchecked")
    private void registerSpigotEvent(PluginManager pluginManager) {
        try {
            Class<?> rawType = Class.forName("org.bukkit.event.player.PlayerCustomClickEvent");
            if (!Event.class.isAssignableFrom(rawType)) return;
            Class<? extends Event> eventType = (Class<? extends Event>) rawType;
            EventExecutor executor = (listener, event) -> handleCustomClick(event);
            pluginManager.registerEvent(eventType, this, EventPriority.NORMAL, executor, plugin, true);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            plugin.getLogger().warning("A API PlayerCustomClickEvent não está disponível neste servidor; botões de TPA ficarão sem ação.");
        }
    }

    private void handleCustomClick(Event event) {
        try {
            String id = event.getClass().getMethod("getId").invoke(event).toString();
            if (!TeleportService.TPA_BUTTON_KEY.asString().equalsIgnoreCase(id)) return;

            Object data = event.getClass().getMethod("getData").invoke(event);
            if (data == null) return;
            String digits = data.toString().replaceAll("[^0-9-]", "");
            if (digits.isEmpty()) return;

            int token;
            try {
                token = Integer.parseInt(digits);
            } catch (NumberFormatException ignored) {
                return;
            }

            Player player = (Player) event.getClass().getMethod("getPlayer").invoke(event);
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
                ButtonAction.remove(token);
                return;
            }

            boolean handled = teleportService.handleButton(player, token);
            if (!handled) return;

            ButtonResult result = switch (action.type()) {
                case ACCEPT -> ButtonResult.ACCEPTED;
                case DENY -> ButtonResult.DENIED;
                case CANCEL -> ButtonResult.CANCELLED;
            };
            results.put(token, result);
            ButtonAction.remove(token);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Ignora representações incompatíveis sem interromper o processamento do servidor.
        }
    }

    private void sendResultMessage(Player player, ButtonResult result, ButtonAction action) {
        String key = switch (result) {
            case ACCEPTED -> "request-already-accepted";
            case DENIED -> "request-already-denied";
            case EXPIRED -> "request-expired-cannot-respond";
            case CANCELLED -> "request-already-cancelled";
            case CANCEL_BLOCKED_EXPIRED -> "request-cannot-cancel-expired";
        };
        String raw = plugin.getConfig().getString("messages." + key, "");
        Player target = org.bukkit.Bukkit.getPlayer(action.targetId());
        if (target != null) raw = raw.replace("{player}", target.getName());
        String prefix = plugin.getConfig().getString("messages.tpa-prefix", plugin.getConfig().getString("messages.prefix", ""));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + raw));
    }

    public void clear() {
        results.clear();
    }

    private enum ButtonResult {
        ACCEPTED,
        DENIED,
        EXPIRED,
        CANCELLED,
        CANCEL_BLOCKED_EXPIRED
    }
}
