package com.donnie1337.essentialsplus.teleport;

import com.donnie1337.essentialsplus.auth.AuthSystemBridge;
import com.donnie1337.essentialsplus.chat.ChatPlusBridge;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class TeleportService {
    private final Plugin plugin;
    private final ChatPlusBridge chatPlusBridge;
    private final AuthSystemBridge authSystemBridge;
    private final ConcurrentMap<UUID, List<TpaRequest>> incoming = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, ConcurrentMap<Integer, ButtonAction>> buttonActions = new ConcurrentHashMap<>();
    private final AtomicInteger buttonToken = new AtomicInteger(1000);
    private BukkitTask expirationTask;

    public TeleportService(Plugin plugin, ChatPlusBridge chatPlusBridge, AuthSystemBridge authSystemBridge) {
        this.plugin = plugin;
        this.chatPlusBridge = chatPlusBridge;
        this.authSystemBridge = authSystemBridge;
    }

    public void start() {
        if (expirationTask != null) expirationTask.cancel();
        expirationTask = Bukkit.getScheduler().runTaskTimer(plugin, this::expireRequests, 20L, 20L);
    }

    public void shutdown() {
        if (expirationTask != null) {
            expirationTask.cancel();
            expirationTask = null;
        }
        incoming.clear();
        buttonActions.clear();
    }

    public void request(Player requester, Player recipient, boolean here) {
        if (requester == null || recipient == null || !requester.isOnline() || !recipient.isOnline()) return;
        if (requester.getUniqueId().equals(recipient.getUniqueId())) {
            message(requester, "cannot-self");
            return;
        }
        if (!authSystemBridge.isAuthenticated(requester)) {
            message(requester, "auth-required");
            return;
        }
        if (!authSystemBridge.isAuthenticated(recipient)) {
            message(requester, "target-not-authenticated");
            return;
        }

        long now = System.currentTimeMillis();
        long timeout = timeoutMillis();
        List<TpaRequest> requests = incoming.computeIfAbsent(recipient.getUniqueId(), ignored -> new ArrayList<>());
        synchronized (requests) {
            removeExpiredLocked(recipient, requests, now, timeout);

            TpaRequest replacement = null;
            for (TpaRequest existing : requests) {
                if (existing.requesterId().equals(requester.getUniqueId())) {
                    replacement = existing;
                    break;
                }
            }
            if (replacement != null) {
                requests.remove(replacement);
                removeButtonsForRequest(replacement);
            }

            int max = Math.max(1, plugin.getConfig().getInt("tpa.max-pending-requests", 5));
            while (requests.size() >= max) {
                TpaRequest oldest = requests.stream().min(Comparator.comparingLong(TpaRequest::createdAt)).orElse(null);
                if (oldest == null) break;
                requests.remove(oldest);
                removeButtonsForRequest(oldest);
                Player oldRequester = Bukkit.getPlayer(oldest.requesterId());
                if (oldRequester != null && oldRequester.isOnline()) message(oldRequester, "too-many-requests", "player", recipient.getName());
            }

            TpaRequest request = new TpaRequest(requester.getUniqueId(), requester.getName(), recipient.getUniqueId(), recipient.getName(), here, now);
            requests.add(request);
            message(requester, "request-sent", "player", recipient.getName());
            sendCancelButton(requester, recipient);
            sendRequestMessage(recipient, requester, here);
        }
    }

    public void accept(Player recipient) { accept(recipient, null); }

    public void accept(Player recipient, String requesterName) {
        TpaRequest request = takeRequest(recipient, requesterName);
        if (request == null) {
            message(recipient, "no-request");
            return;
        }
        completeAccepted(recipient, request);
    }

    public void deny(Player recipient) { deny(recipient, null); }

    public void deny(Player recipient, String requesterName) {
        TpaRequest request = takeRequest(recipient, requesterName);
        if (request == null) {
            message(recipient, "no-request");
            return;
        }
        removeButtonsForRequest(request);
        Player requester = Bukkit.getPlayer(request.requesterId());
        if (requester != null && requester.isOnline()) message(requester, "request-denied-sender", "player", recipient.getName());
        message(recipient, "request-denied", "player", request.requesterName());
    }

    public void cancel(Player requester) { cancel(requester, null); }

    public void cancel(Player requester, String recipientName) {
        if (requester == null) return;
        TpaRequest found = null;
        UUID ownerId = null;
        for (var entry : incoming.entrySet()) {
            List<TpaRequest> requests = entry.getValue();
            synchronized (requests) {
                for (TpaRequest request : requests) {
                    if (request.requesterId().equals(requester.getUniqueId()) && (recipientName == null || request.recipientName().equalsIgnoreCase(recipientName))) {
                        found = request;
                        ownerId = entry.getKey();
                        break;
                    }
                }
            }
            if (found != null) break;
        }
        if (found == null) {
            message(requester, "no-request");
            return;
        }

        List<TpaRequest> requests = incoming.get(ownerId);
        if (requests != null) {
            synchronized (requests) { requests.remove(found); }
            if (requests.isEmpty()) incoming.remove(ownerId, requests);
        }
        removeButtonsForRequest(found);
        Player recipient = Bukkit.getPlayer(found.recipientId());
        if (recipient != null && recipient.isOnline()) message(recipient, "request-cancelled", "player", requester.getName());
        message(requester, "request-cancelled", "player", found.recipientName());
    }

    public List<String> pendingRequesterNames(Player recipient) {
        List<TpaRequest> requests = incoming.get(recipient.getUniqueId());
        if (requests == null) return List.of();
        synchronized (requests) { return requests.stream().map(TpaRequest::requesterName).toList(); }
    }

    public List<String> pendingRecipientNames(Player requester) {
        List<String> names = new ArrayList<>();
        for (List<TpaRequest> requests : incoming.values()) {
            synchronized (requests) {
                for (TpaRequest request : requests) if (request.requesterId().equals(requester.getUniqueId())) names.add(request.recipientName());
            }
        }
        return names;
    }

    private TpaRequest takeRequest(Player recipient, String requesterName) {
        if (recipient == null) return null;
        List<TpaRequest> requests = incoming.get(recipient.getUniqueId());
        if (requests == null) return null;
        synchronized (requests) {
            removeExpiredLocked(recipient, requests, System.currentTimeMillis(), timeoutMillis());
            TpaRequest selected = null;
            for (TpaRequest request : requests) {
                if (requesterName == null || request.requesterName().equalsIgnoreCase(requesterName)) {
                    if (selected == null || request.createdAt() > selected.createdAt()) selected = request;
                }
            }
            if (selected != null) requests.remove(selected);
            if (requests.isEmpty()) incoming.remove(recipient.getUniqueId(), requests);
            return selected;
        }
    }

    private void completeAccepted(Player recipient, TpaRequest request) {
        removeButtonsForRequest(request);
        Player requester = Bukkit.getPlayer(request.requesterId());
        if (requester == null || !requester.isOnline()) {
            message(recipient, "target-offline");
            return;
        }
        if (!authSystemBridge.isAuthenticated(requester) || !authSystemBridge.isAuthenticated(recipient)) {
            message(recipient, "auth-required");
            return;
        }

        Player teleported = request.here() ? recipient : requester;
        Player destination = request.here() ? requester : recipient;
        boolean success = teleported.teleport(destination.getLocation());
        if (!success) {
            message(recipient, "teleport-failed");
            message(requester, "teleport-failed");
            return;
        }
        message(recipient, "request-accepted", "player", requester.getName());
        message(requester, "request-accepted-sender", "player", recipient.getName());
    }

    private void sendRequestMessage(Player recipient, Player requester, boolean here) {
        String raw = plugin.getConfig().getString(here ? "messages.request-here-received" : "messages.request-received", "");
        raw = color(raw.replace("{player}", coloredPlayer(requester)));
        List<BaseComponent> components = new ArrayList<>();
        for (BaseComponent component : TextComponent.fromLegacyText(raw)) components.add(component);

        int accept = registerButton(recipient, ButtonActionType.ACCEPT, requester.getUniqueId());
        int deny = registerButton(recipient, ButtonActionType.DENY, requester.getUniqueId());
        components.add(new TextComponent(" "));
        components.add(buttonComponent("[ACEITAR]", "§a", accept));
        components.add(new TextComponent(" "));
        components.add(buttonComponent("[RECUSAR]", "§c", deny));
        recipient.spigot().sendMessage(components.toArray(new BaseComponent[0]));
    }

    private void sendCancelButton(Player requester, Player recipient) {
        int token = registerButton(requester, ButtonActionType.CANCEL, recipient.getUniqueId());
        requester.spigot().sendMessage(buttonComponent("[CANCELAR]", "§c", token));
    }

    private int registerButton(Player player, ButtonActionType type, UUID targetId) {
        int token = buttonToken.incrementAndGet();
        buttonActions.computeIfAbsent(player.getUniqueId(), ignored -> new ConcurrentHashMap<>()).put(token, new ButtonAction(token, type, targetId));
        return token;
    }

    private TextComponent buttonComponent(String label, String color, int token) {
        BaseComponent[] legacy = TextComponent.fromLegacyText(color(label));
        TextComponent component = legacy.length == 0 ? new TextComponent(label) : new TextComponent(legacy[0]);
        component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/essentialsplus:tpaaction " + token));
        return component;
    }

    public boolean handleButton(Player player, int token) {
        ConcurrentMap<Integer, ButtonAction> actions = buttonActions.get(player.getUniqueId());
        if (actions == null) return false;
        ButtonAction action = actions.remove(token);
        if (action == null) return false;
        switch (action.type()) {
            case ACCEPT -> acceptById(player, action.targetId());
            case DENY -> denyById(player, action.targetId());
            case CANCEL -> cancelById(player, action.targetId());
        }
        return true;
    }

    private void acceptById(Player recipient, UUID requesterId) {
        TpaRequest request = removeRequestById(recipient, requesterId);
        if (request == null) { message(recipient, "no-request"); return; }
        completeAccepted(recipient, request);
    }

    private void denyById(Player recipient, UUID requesterId) {
        TpaRequest request = removeRequestById(recipient, requesterId);
        if (request == null) { message(recipient, "no-request"); return; }
        removeButtonsForRequest(request);
        Player requester = Bukkit.getPlayer(request.requesterId());
        if (requester != null && requester.isOnline()) message(requester, "request-denied-sender", "player", recipient.getName());
        message(recipient, "request-denied", "player", request.requesterName());
    }

    private void cancelById(Player requester, UUID recipientId) {
        List<TpaRequest> requests = incoming.get(recipientId);
        if (requests == null) { message(requester, "no-request"); return; }
        TpaRequest found = null;
        synchronized (requests) {
            for (TpaRequest request : requests) if (request.requesterId().equals(requester.getUniqueId())) { found = request; break; }
            if (found != null) requests.remove(found);
        }
        if (found == null) { message(requester, "no-request"); return; }
        if (requests.isEmpty()) incoming.remove(recipientId, requests);
        removeButtonsForRequest(found);
        Player recipient = Bukkit.getPlayer(recipientId);
        if (recipient != null && recipient.isOnline()) message(recipient, "request-cancelled", "player", requester.getName());
        message(requester, "request-cancelled", "player", found.recipientName());
    }

    private TpaRequest removeRequestById(Player recipient, UUID requesterId) {
        List<TpaRequest> requests = incoming.get(recipient.getUniqueId());
        if (requests == null) return null;
        synchronized (requests) {
            TpaRequest found = null;
            for (TpaRequest request : requests) if (request.requesterId().equals(requesterId)) { found = request; break; }
            if (found != null) requests.remove(found);
            if (requests.isEmpty()) incoming.remove(recipient.getUniqueId(), requests);
            return found;
        }
    }

    private void expireRequests() {
        long now = System.currentTimeMillis();
        long timeout = timeoutMillis();
        if (timeout <= 0) return;
        for (var entry : incoming.entrySet()) {
            List<TpaRequest> requests = entry.getValue();
            List<TpaRequest> expired = new ArrayList<>();
            synchronized (requests) {
                requests.removeIf(request -> {
                    boolean exp = request.isExpired(now, timeout);
                    if (exp) expired.add(request);
                    return exp;
                });
            }
            if (requests.isEmpty()) incoming.remove(entry.getKey(), requests);
            for (TpaRequest request : expired) {
                removeButtonsForRequest(request);
                Player requester = Bukkit.getPlayer(request.requesterId());
                if (requester != null && requester.isOnline()) message(requester, "request-expired", "player", request.recipientName());
                Player target = Bukkit.getPlayer(request.recipientId());
                if (target != null && target.isOnline()) message(target, "request-expired", "player", request.requesterName());
            }
        }
    }

    private void removeExpiredLocked(Player recipient, List<TpaRequest> requests, long now, long timeout) {
        if (timeout <= 0) return;
        List<TpaRequest> expired = new ArrayList<>();
        requests.removeIf(request -> {
            boolean exp = request.isExpired(now, timeout);
            if (exp) expired.add(request);
            return exp;
        });
        for (TpaRequest request : expired) {
            removeButtonsForRequest(request);
            Player requester = Bukkit.getPlayer(request.requesterId());
            if (requester != null && requester.isOnline()) message(requester, "request-expired", "player", recipient.getName());
        }
    }

    private long timeoutMillis() {
        long seconds = plugin.getConfig().getLong("tpa.request-timeout-seconds", 20L);
        return seconds <= 0 ? 0 : seconds * 1000L;
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
        if (!chatPlusBridge.sendSystemMessage(player, colored)) player.sendMessage(colored);
    }

    private String coloredPlayer(Player player) {
        if (player == null) return "";
        String color = extractLegacyColor(player.getDisplayName());
        if (color == null) color = extractLegacyColor(player.getPlayerListName());
        if (color == null) color = cargoNicknameColor(player);
        return (color == null ? "§f" : color) + player.getName();
    }

    private String extractLegacyColor(String value) {
        if (value == null || value.isEmpty()) return null;
        value = color(value);
        for (int i = 0; i < value.length() - 1; i++) {
            if (value.charAt(i) != ChatColor.COLOR_CHAR) continue;
            char code = Character.toLowerCase(value.charAt(i + 1));
            if ("0123456789abcdef".indexOf(code) >= 0) return "§" + code;
            if (code == 'x' && i + 13 < value.length()) return value.substring(i, i + 14);
        }
        return null;
    }

    private String cargoNicknameColor(Player player) {
        try {
            Class<?> apiClass = Class.forName("com.cargoplus.api.CargoPlusAPI");
            RegisteredServiceProvider<?> registration = Bukkit.getServicesManager().getRegistration(apiClass);
            if (registration == null || registration.getProvider() == null) return null;
            Object value = apiClass.getMethod("getNicknameColor", UUID.class).invoke(registration.getProvider(), player.getUniqueId());
            return value == null ? null : color(value.toString());
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return null;
        }
    }

    private String color(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public void clearPlayer(Player player) {
        if (player == null) return;
        UUID id = player.getUniqueId();
        List<TpaRequest> own = incoming.remove(id);
        if (own != null) synchronized (own) { own.forEach(this::removeButtonsForRequest); }
        for (var entry : incoming.entrySet()) {
            List<TpaRequest> requests = entry.getValue();
            List<TpaRequest> removed = new ArrayList<>();
            synchronized (requests) {
                requests.removeIf(request -> {
                    boolean match = request.requesterId().equals(id);
                    if (match) removed.add(request);
                    return match;
                });
            }
            removed.forEach(this::removeButtonsForRequest);
            if (requests.isEmpty()) incoming.remove(entry.getKey(), requests);
        }
        buttonActions.remove(id);
    }
}
