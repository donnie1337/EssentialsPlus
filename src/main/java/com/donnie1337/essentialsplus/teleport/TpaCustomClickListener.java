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
        try {
            Class<?> rawType = Class.forName("org.bukkit.event.player.PlayerCustomClickEvent");
            if (!Event.class.isAssignableFrom(rawType)) return;
            @SuppressWarnings("unchecked") Class<? extends Event> eventType = (Class<? extends Event>) rawType;
            EventExecutor executor = (listener, event) -> handleCustomClick(event);
            pluginManager.registerEvent(eventType, this, EventPriority.NORMAL, executor, plugin, true);
            plugin.getLogger().info("Botões de TPA registrados usando custom click do Spigot.");
        } catch (ReflectiveOperationException | LinkageError ignored) {
            plugin.getLogger().warning("A API PlayerCustomClickEvent não está disponível neste servidor; botões de TPA ficarão sem ação.");
        }
    }

    private void handleCustomClick(Event event) {
        try {
            Object identifier = event.getClass().getMethod("getId").invoke(event);
            if (identifier == null || !TeleportService.TPA_BUTTON_KEY.asString().equalsIgnoreCase(identifier.toString())) return;

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

            Object playerObject = event.getClass().getMethod("getPlayer").invoke(event);
            if (!(playerObject instanceof Player player) || !player.hasPermission(PERMISSION)) return;

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
                rememberResult(action, expiredResult);
                sendResultMessage(player, expiredResult, action);
                return;
            }

            boolean handled = teleportService.handleButton(player, token);
            if (!handled) {
                return;
            }

            ButtonResult result = switch (action.type()) {
                case ACCEPT -> ButtonResult.ACCEPTED;
                case DENY -> ButtonResult.DENIED;
                case CANCEL -> ButtonResult.CANCELLED;
            };
            rememberResult(action, result);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    private void rememberResult(ButtonAction source, ButtonResult result) {
        results.put(source.token(), result);

        for (ButtonAction related : ButtonAction.findRelated(source)) {
            ButtonResult relatedResult = switch (result) {
                case ACCEPTED -> related.type() == ButtonActionType.CANCEL
                        ? ButtonResult.CANCEL_BLOCKED_ACCEPTED : ButtonResult.ACCEPTED;
                case DENIED -> related.type() == ButtonActionType.CANCEL
                        ? ButtonResult.CANCEL_BLOCKED_DENIED : ButtonResult.DENIED;
                case CANCELLED -> ButtonResult.CANCELLED;
                case EXPIRED -> related.type() == ButtonActionType.CANCEL
                        ? ButtonResult.CANCEL_BLOCKED_EXPIRED : ButtonResult.EXPIRED;
                case CANCEL_BLOCKED_ACCEPTED, CANCEL_BLOCKED_DENIED, CANCEL_BLOCKED_EXPIRED -> result;
            };
            results.put(related.token(), relatedResult);
        }
    }

    private void sendResultMessage(Player player, ButtonResult result, ButtonAction action) {
        String key = switch (result) {
            case ACCEPTED -> "messages.tpa.resposta.ja-aceita";
            case DENIED -> "messages.tpa.resposta.ja-recusada";
            case EXPIRED -> "messages.tpa.expiracao.nao-pode-responder";
            case CANCELLED -> "messages.tpa.cancelamento.ja-cancelada";
            case CANCEL_BLOCKED_ACCEPTED -> "messages.tpa.cancelamento.jogador-ja-aceitou";
            case CANCEL_BLOCKED_DENIED -> "messages.tpa.cancelamento.jogador-recusou";
            case CANCEL_BLOCKED_EXPIRED -> "messages.tpa.cancelamento.solicitacao-expirada";
        };

        String fallback = switch (result) {
            case ACCEPTED -> "Você já aceitou esta solicitação de TPA.";
            case DENIED -> "Você já recusou esta solicitação de TPA.";
            case EXPIRED -> "Você não pode aceitar ou recusar porque o convite expirou.";
            case CANCELLED -> "Você já cancelou esta solicitação de TPA.";
            case CANCEL_BLOCKED_ACCEPTED -> "Não é possível cancelar porque o pedido já foi aceito.";
            case CANCEL_BLOCKED_DENIED -> "Não é possível cancelar porque o pedido já foi recusado.";
            case CANCEL_BLOCKED_EXPIRED -> "Não é possível cancelar porque o pedido expirou.";
        };

        String raw = plugin.getConfig().getString(key, fallback);
        Player target = Bukkit.getPlayer(action.targetId());
        if (target != null) raw = raw.replace("{player}", target.getName());
        String prefix = plugin.getConfig().getString("messages.tpa.prefix", plugin.getConfig().getString("messages.tpa-prefix", "&b&lᴛᴘᴀ &8• &r"));
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
        CANCEL_BLOCKED_ACCEPTED,
        CANCEL_BLOCKED_DENIED,
        CANCEL_BLOCKED_EXPIRED
    }
}
