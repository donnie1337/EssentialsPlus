package com.donnie1337.essentialsplus.teleport;

import com.donnie1337.essentialsplus.bridge.AuthSystemBridge;
import com.donnie1337.essentialsplus.bridge.ChatPlusBridge;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class TeleportService {
    private final Plugin plugin;
    private final AuthSystemBridge authSystemBridge;
    private final ChatPlusBridge chatPlusBridge;
    private final ConcurrentMap<UUID, TpaRequest> incoming = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, ConcurrentMap<Integer, ButtonAction>> buttonActions = new ConcurrentHashMap<>();
    private final AtomicInteger buttonToken = new AtomicInteger(1000);

    private volatile Plugin cargoPlugin;
    private volatile Method cargoApiMethod;
    private volatile Method cargoNicknameColorMethod;

    public TeleportService(Plugin plugin, AuthSystemBridge authSystemBridge, ChatPlusBridge chatPlusBridge) {
        this.plugin = plugin;
        this.authSystemBridge = authSystemBridge;
        this.chatPlusBridge = chatPlusBridge;
    }

    public void request(Player requester, Player recipient, boolean here) {
        if (!authSystemBridge.isAuthenticated(requester) || !authSystemBridge.isAuthenticated(recipient)) {
            return;
        }

        incoming.put(recipient.getUniqueId(), new TpaRequest(
                requester.getUniqueId(), requester.getName(),
                recipient.getUniqueId(), recipient.getName(), here,
                System.currentTimeMillis()
        ));

        message(requester, "request-sent", "player", recipient.getName());
        sendCancelButton(requester, recipient);
        sendRequestMessage(recipient, requester, here);
    }

    public void accept(Player recipient) {
        TpaRequest request = incoming.remove(recipient.getUniqueId());
        if (request == null) {
            message(recipient, "no-request");
            return;
        }

        Player requester = Bukkit.getPlayer(request.requesterId());
        if (requester == null || !requester.isOnline()) {
            message(recipient, "player-offline", "player", request.requesterName());
            removeButtonsForRequest(request);
            return;
        }

        if (!authSystemBridge.isAuthenticated(requester) || !authSystemBridge.isAuthenticated(recipient)) {
            removeButtonsForRequest(request);
            return;
        }

        removeButtonsForRequest(request);
        if (request.here()) {
            requester.teleport(recipient.getLocation());
        } else {
            recipient.teleport(requester.getLocation());
        }

        message(recipient, "request-accepted", "player", requester.getName());
        message(requester, "request-accepted-target", "player", recipient.getName());
    }

    public void deny(Player recipient) {
        TpaRequest request = incoming.remove(recipient.getUniqueId());
        if (request == null) {
            message(recipient, "no-request");
            return;
        }

        removeButtonsForRequest(request);
        Player requester = Bukkit.getPlayer(request.requesterId());
        if (requester != null && requester.isOnline()) {
            message(requester, "request-denied", "player", recipient.getName());
        }
        message(recipient, "request-denied-target", "player", request.requesterName());
    }

    public void cancel(Player requester) {
        TpaRequest found = null;
        for (TpaRequest request : incoming.values()) {
            if (request.requesterId().equals(requester.getUniqueId())) {
                found = request;
                break;
            }
        }

        if (found == null) {
            message(requester, "no-request");
            return;
        }

        incoming.remove(found.recipientId(), found);
        removeButtonsForRequest(found);

        Player recipient = Bukkit.getPlayer(found.recipientId());
        if (recipient != null && recipient.isOnline()) {
            message(recipient, "request-cancelled", "player", requester.getName());
        }
        message(requester, "request-cancelled-target", "player", found.recipientName());
    }

    private void sendRequestMessage(Player recipient, Player requester, boolean here) {
        String raw = plugin.getConfig().getString(
                here ? "messages.request-here-received" : "messages.request-received", "");
        raw = raw.replace("{player}", coloredPlayer(requester));
        raw = color(raw);

        List<BaseComponent> components = new ArrayList<>();
        addLegacy(components, raw);

        if (here) {
            components.add(new TextComponent(" "));
            components.add(buttonComponent("[ACEITAR]", "§a", registerButton(recipient, ButtonActionType.ACCEPT, requester.getUniqueId())));
            components.add(new TextComponent(" "));
            components.add(buttonComponent("[RECUSAR]", "§c", registerButton(recipient, ButtonActionType.DENY, requester.getUniqueId())));
        } else {
            components.add(new TextComponent(" "));
            components.add(buttonComponent("[ACEITAR]", "§a", registerButton(recipient, ButtonActionType.ACCEPT, requester.getUniqueId())));
            components.add(new TextComponent(" "));
            components.add(buttonComponent("[RECUSAR]", "§c", registerButton(recipient, ButtonActionType.DENY, requester.getUniqueId())));
        }

        recipient.spigot().sendMessage(components.toArray(new BaseComponent[0]));
    }

    private void sendCancelButton(Player requester, Player recipient) {
        int token = registerButton(requester, ButtonActionType.CANCEL, recipient.getUniqueId());
        requester.spigot().sendMessage(buttonComponent("§c[CANCELAR]", "§c", token));
    }

    private int registerButton(Player player, ButtonActionType type, UUID targetId) {
        int token = buttonToken.incrementAndGet();
        buttonActions.computeIfAbsent(player.getUniqueId(), ignored -> new ConcurrentHashMap<>())
                .put(token, new ButtonAction(token, type, targetId));
        return token;
    }

    private TextComponent buttonComponent(String label, String color, int token) {
        TextComponent component = new TextComponent(TextComponent.fromLegacyText(color(label))[0]);
        component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/essentialsplus:tpaaction " + token));
        return component;
    }

    public boolean handleButton(Player player, int token) {
        ConcurrentMap<Integer, ButtonAction> actions = buttonActions.get(player.getUniqueId());
        if (actions == null) return false;

        ButtonAction action = actions.remove(token);
        if (action == null) return false;

        switch (action.type()) {
            case ACCEPT -> accept(player);
            case DENY -> deny(player);
            case CANCEL -> cancel(player);
        }
        return true;
    }

    private void removeButtonsForRequest(TpaRequest request) {
        removeButton(request.recipientId(), request.requesterId(), ButtonActionType.ACCEPT);
        removeButton(request.recipientId(), request.requesterId(), ButtonActionType.DENY);
        removeButton(request.requesterId(), request.recipientId(), ButtonActionType.CANCEL);
    }

    private void removeButton(UUID ownerId, UUID targetId, ButtonActionType type) {
        ConcurrentMap<Integer, ButtonAction> actions = buttonActions.get(ownerId);
        if (actions == null) return;
        actions.values().removeIf(action -> action.type() == type && action.targetId().equals(targetId));
        if (actions.isEmpty()) buttonActions.remove(ownerId, actions);
    }

    private void message(Player player, String path, String... replacements) {
        if (player == null || !player.isOnline()) return;
        String raw = plugin.getConfig().getString("messages." + path, "");

        for (int i = 0; i + 1 < replacements.length; i += 2) {
            String key = replacements[i];
            String replacement = replacements[i + 1];
            if ("player".equals(key)) {
                Player target = Bukkit.getPlayerExact(replacement);
                if (target != null) replacement = coloredPlayer(target);
            }
            raw = raw.replace("{" + key + "}", replacement);
        }

        String colored = color(raw);
        if (!chatPlusBridge.sendSystemMessage(player, colored)) {
            player.sendMessage(colored);
        }
    }

    /**
     * Retorna o nome do jogador com a MESMA cor que ele já usa no nickname.
     * Não define uma cor própria para o TPA: apenas reaproveita a cor visível
     * no display name/list name do jogador.
     */
    private String coloredPlayer(Player player) {
        if (player == null) return "";

        String color = extractLegacyColor(player.getDisplayName());
        if (color == null) {
            color = extractLegacyColor(player.getPlayerListName());
        }
        if (color == null) {
            color = cargoNicknameColor(player);
        }
        if (color == null) {
            color = "§f";
        }

        return color + player.getName();
    }

    private String extractLegacyColor(String value) {
        if (value == null || value.isEmpty()) return null;
        value = color(value);

        for (int i = 0; i < value.length() - 1; i++) {
            if (value.charAt(i) != ChatColor.COLOR_CHAR) continue;

            char code = Character.toLowerCase(value.charAt(i + 1));
            if ("0123456789abcdef".indexOf(code) >= 0) {
                return "§" + code;
            }

            if (code == 'x' && i + 13 < value.length()) {
                return value.substring(i, i + 14);
            }
        }
        return null;
    }

    /** Fallback opcional para servidores que expõem a cor do CargoPlus via API. */
    private String cargoNicknameColor(Player player) {
        if (player == null || !player.isOnline()) return null;
        Plugin current = Bukkit.getPluginManager().getPlugin("CargoPlus");
        if (current == null || !current.isEnabled()) return null;

        if (cargoPlugin != current || cargoApiMethod == null || cargoNicknameColorMethod == null) {
            synchronized (this) {
                if (cargoPlugin != current || cargoApiMethod == null || cargoNicknameColorMethod == null) {
                    try {
                        cargoPlugin = current;
                        cargoApiMethod = current.getClass().getMethod("api");
                        Object api = cargoApiMethod.invoke(current);
                        cargoNicknameColorMethod = api.getClass().getMethod("getNicknameColor", UUID.class);
                    } catch (ReflectiveOperationException | LinkageError ex) {
                        cargoPlugin = current;
                        cargoApiMethod = null;
                        cargoNicknameColorMethod = null;
                        return null;
                    }
                }
            }
        }

        try {
            Object api = cargoApiMethod.invoke(cargoPlugin);
            Object value = cargoNicknameColorMethod.invoke(api, player.getUniqueId());
            return value == null ? null : color(value.toString());
        } catch (ReflectiveOperationException | LinkageError ex) {
            return null;
        }
    }

    private String color(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private void addLegacy(List<BaseComponent> components, String text) {
        for (BaseComponent component : TextComponent.fromLegacyText(text)) {
            components.add(component);
        }
    }

    public void clearPlayer(Player player) {
        if (player == null) return;
        TpaRequest request = incoming.remove(player.getUniqueId());
        if (request != null) removeButtonsForRequest(request);
        buttonActions.remove(player.getUniqueId());
    }
}
