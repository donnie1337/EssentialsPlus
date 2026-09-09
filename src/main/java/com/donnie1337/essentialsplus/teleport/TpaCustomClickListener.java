package com.donnie1337.essentialsplus.teleport;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TpaCustomClickListener implements Listener {
    private static final String PERMISSION = "essentialsplus.tpa";
    private static final String ACTION_ID = "essentialsplus:tpa_action";
    private static final Pattern TOKEN_PATTERN = Pattern.compile("(?:^|[\\s{\"])(?:token)\\s*[:=]\\s*(\\d+)");

    private final Plugin plugin;
    private final TeleportService teleportService;

    public TpaCustomClickListener(Plugin plugin, TeleportService teleportService) {
        this.plugin = plugin;
        this.teleportService = teleportService;
    }

    /**
     * Registers the custom-click listener without linking the plugin class directly to
     * one specific Paper/Bukkit PlayerCustomClickEvent class. Paper 26.2 exposes the
     * event in io.papermc.paper.event.player, while some 26.2 server implementations
     * expose the Bukkit-compatible org.bukkit.event.player variant.
     */
    public void register(PluginManager pluginManager) {
        Class<? extends Event> eventClass = findEventClass();
        if (eventClass == null) {
            plugin.getLogger().warning("PlayerCustomClickEvent não está disponível neste servidor; botões de TPA ficarão desativados.");
            return;
        }

        pluginManager.registerEvent(
            eventClass,
            this,
            EventPriority.NORMAL,
            (listener, event) -> handle(event),
            plugin,
            false
        );
        plugin.getLogger().info("Botões de TPA clicáveis registrados usando " + eventClass.getName() + ".");
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Event> findEventClass() {
        String[] candidates = {
            "io.papermc.paper.event.player.PlayerCustomClickEvent",
            "org.bukkit.event.player.PlayerCustomClickEvent"
        };
        for (String name : candidates) {
            try {
                Class<?> type = Class.forName(name, false, plugin.getClass().getClassLoader());
                if (Event.class.isAssignableFrom(type)) return (Class<? extends Event>) type;
            } catch (ClassNotFoundException ignored) {
                // Try the other API variant.
            }
        }
        return null;
    }

    private void handle(Event event) {
        try {
            String identifier = stringValue(invoke(event, "getIdentifier"));
            if (identifier == null) identifier = stringValue(invoke(event, "getId"));
            if (!ACTION_ID.equals(identifier)) return;

            Player player = extractPlayer(event);
            if (player == null || !player.hasPermission(PERMISSION)) return;

            Object payload = invoke(event, "getTag");
            if (payload == null) payload = invoke(event, "getData");
            String payloadText = stringValue(payload);
            if (payloadText == null) return;

            Matcher matcher = TOKEN_PATTERN.matcher(payloadText);
            if (!matcher.find()) return;

            int token = Integer.parseInt(matcher.group(1));
            teleportService.handleButton(player, token);
        } catch (NumberFormatException ignored) {
            Player player = extractPlayer(event);
            String name = player == null ? "desconhecido" : player.getName();
            plugin.getLogger().fine("Token de botão TPA inválido recebido de " + name + ".");
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            plugin.getLogger().fine("Não foi possível processar o clique personalizado de TPA.");
        }
    }

    private Player extractPlayer(Event event) throws ReflectiveOperationException {
        Object direct = invoke(event, "getPlayer");
        if (direct instanceof Player player) return player;

        Object connection = invoke(event, "getCommonConnection");
        Object player = connection == null ? null : invoke(connection, "getPlayer");
        return player instanceof Player p ? p : null;
    }

    private Object invoke(Object target, String methodName) throws ReflectiveOperationException {
        if (target == null) return null;
        Method method = target.getClass().getMethod(methodName);
        return method.invoke(target);
    }

    private String stringValue(Object value) {
        if (value == null) return null;
        try {
            Method stringMethod = value.getClass().getMethod("string");
            Object result = stringMethod.invoke(value);
            if (result != null) return result.toString();
        } catch (ReflectiveOperationException ignored) {
            // Fall back to toString for Bukkit JsonElement and similar payloads.
        }
        return value.toString();
    }
}
