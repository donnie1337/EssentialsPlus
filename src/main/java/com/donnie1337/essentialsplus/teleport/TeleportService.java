package com.donnie1337.essentialsplus.teleport;

import com.donnie1337.essentialsplus.auth.AuthSystemBridge;
import com.donnie1337.essentialsplus.chat.ChatPlusBridge;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class TeleportService implements Listener {
    private static final String BUTTON_IDENTIFIER_PREFIX = "essentialsplus:tpa/";
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

    private final JavaPlugin plugin;
    private final ChatPlusBridge chatPlusBridge;
    private final AuthSystemBridge authSystemBridge;
    private final ConcurrentMap<UUID, LinkedHashMap<UUID, TpaRequest>> incoming = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, ConcurrentMap<Integer, ButtonAction>> buttonActions = new ConcurrentHashMap<>();
    private final AtomicInteger buttonToken = new AtomicInteger(1000);
    private BukkitTask expirationTask;
    private CustomTpaClickListener customTpaClickListener;
    private volatile Plugin cargoPlugin;
    private volatile Method cargoApiMethod;
    private volatile Method cargoNicknameColorMethod;

    public TeleportService(JavaPlugin plugin, ChatPlusBridge chatPlusBridge, AuthSystemBridge authSystemBridge) {
        this.plugin = plugin; this.chatPlusBridge = chatPlusBridge; this.authSystemBridge = authSystemBridge;
    }

    public JavaPlugin plugin() { return plugin; }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        customTpaClickListener = new CustomTpaClickListener(this);
        com.github.retrooper.packetevents.PacketEvents.getAPI().getEventManager().registerListener(customTpaClickListener);
        expirationTask = Bukkit.getScheduler().runTaskTimer(plugin, this::expireRequests, 20L, 20L);
    }

    public void shutdown() {
        if (expirationTask != null) { expirationTask.cancel(); expirationTask = null; }
        if (customTpaClickListener != null) {
            com.github.retrooper.packetevents.PacketEvents.getAPI().getEventManager().unregisterListener(customTpaClickListener);
            customTpaClickListener = null;
        }
        incoming.clear();
        buttonActions.clear();
    }

    public boolean enabled() { return plugin.getConfig().getBoolean("tpa.enabled", true); }

    public boolean request(Player requester, Player recipient, boolean here) {
        if (!authenticated(requester)) return false;
        if (!enabled()) { message(requester, "disabled"); return false; }
        if (requester.getUniqueId().equals(recipient.getUniqueId())) { message(requester, "cannot-self"); return false; }
        final long now = System.currentTimeMillis();
        if (hasPendingOutgoingRequest(requester.getUniqueId(), now)) { message(requester, "pending-request"); return false; }
        final int max = Math.max(1, plugin.getConfig().getInt("tpa.max-pending-requests", 5));
        final UUID recipientId = recipient.getUniqueId();
        final LinkedHashMap<UUID, TpaRequest> requests = incoming.computeIfAbsent(recipientId, ignored -> new LinkedHashMap<>());
        synchronized (requests) {
            removeExpired(requests, now);
            if (requests.size() >= max) { if (requests.isEmpty()) incoming.remove(recipientId, requests); message(requester, "too-many-requests"); return false; }
            requests.put(requester.getUniqueId(), new TpaRequest(requester.getUniqueId(), requester.getName(), recipientId, recipient.getName(), here, now));
        }
        message(requester, "request-sent", "player", recipient.getName());
        sendCancelButton(requester, recipient);
        sendRequestMessage(recipient, requester, here);
        return true;
    }

    public boolean accept(Player recipient, String requesterName) {
        if (!authenticated(recipient)) return false;
        final TpaRequest request = findRequest(recipient.getUniqueId(), requesterName);
        if (request == null) { message(recipient, "no-request"); return false; }
        return accept(recipient, request.requesterId());
    }

    private boolean accept(Player recipient, UUID requesterId) {
        if (!authenticated(recipient)) return false;
        final TpaRequest request = findRequest(recipient.getUniqueId(), requesterId);
        if (request == null) { message(recipient, "no-request"); return false; }
        removeRequest(request);
        final Player requester = Bukkit.getPlayer(request.requesterId());
        if (requester == null || !requester.isOnline()) { message(recipient, "target-offline"); return false; }
        if (!authenticated(requester)) { message(recipient, "target-not-authenticated"); return false; }
        final Player teleported; final Location destination;
        if (request.here()) { teleported = recipient; destination = requester.getLocation().clone(); }
        else { teleported = requester; destination = recipient.getLocation().clone(); }
        performTeleport(teleported, destination);
        message(recipient, "request-accepted", "player", request.requesterName());
        message(requester, "request-accepted-sender", "player", recipient.getName());
        return true;
    }

    public boolean deny(Player recipient, String requesterName) {
        if (!authenticated(recipient)) return false;
        final TpaRequest request = findRequest(recipient.getUniqueId(), requesterName);
        if (request == null) { message(recipient, "no-request"); return false; }
        return deny(recipient, request.requesterId());
    }

    private boolean deny(Player recipient, UUID requesterId) {
        if (!authenticated(recipient)) return false;
        final TpaRequest request = findRequest(recipient.getUniqueId(), requesterId);
        if (request == null) { message(recipient, "no-request"); return false; }
        removeRequest(request);
        message(recipient, "request-denied", "player", request.requesterName());
        final Player requester = Bukkit.getPlayer(request.requesterId());
        if (requester != null) message(requester, "request-denied-sender", "player", recipient.getName());
        return true;
    }

    public boolean cancel(Player requester, String recipientName) {
        if (!authenticated(requester)) return false; boolean removed = false;
        for (Map.Entry<UUID, LinkedHashMap<UUID, TpaRequest>> entry : incoming.entrySet()) {
            final LinkedHashMap<UUID, TpaRequest> requests = entry.getValue();
            synchronized (requests) {
                final TpaRequest request = requests.get(requester.getUniqueId());
                if (request != null && (recipientName == null || request.recipientName().equalsIgnoreCase(recipientName))) {
                    requests.remove(requester.getUniqueId());
                    removeButtonsForRequest(request);
                    removed = true;
                    final Player recipient = Bukkit.getPlayer(entry.getKey());
                    if (recipient != null) message(recipient, "request-cancelled");
                    if (requests.isEmpty()) incoming.remove(entry.getKey(), requests);
                    if (recipientName != null) break;
                }
            }
        }
        if (removed) message(requester, "request-cancelled");
        return removed;
    }

    private boolean cancel(Player requester, UUID recipientId) {
        if (!authenticated(requester)) return false;
        final LinkedHashMap<UUID, TpaRequest> requests = incoming.get(recipientId);
        if (requests == null) { message(requester, "no-request"); return false; }
        synchronized (requests) {
            final TpaRequest request = requests.get(requester.getUniqueId());
            if (request == null) { message(requester, "no-request"); return false; }
            requests.remove(requester.getUniqueId());
            removeButtonsForRequest(request);
            if (requests.isEmpty()) incoming.remove(recipientId, requests);
            final Player recipient = Bukkit.getPlayer(recipientId);
            if (recipient != null) message(recipient, "request-cancelled");
        }
        message(requester, "request-cancelled");
        return true;
    }

    public List<String> pendingRequesterNames(Player recipient) {
        if (!authenticated(recipient)) return List.of();
        final LinkedHashMap<UUID, TpaRequest> requests = incoming.get(recipient.getUniqueId());
        if (requests == null) return List.of();
        synchronized (requests) {
            removeExpired(requests, System.currentTimeMillis());
            if (requests.isEmpty()) { incoming.remove(recipient.getUniqueId(), requests); return List.of(); }
            return Collections.unmodifiableList(requests.values().stream().map(TpaRequest::requesterName).toList());
        }
    }

    public List<String> pendingRecipientNames(Player requester) {
        if (!authenticated(requester)) return List.of();
        final List<String> names = new ArrayList<>(); final long now = System.currentTimeMillis();
        for (Map.Entry<UUID, LinkedHashMap<UUID, TpaRequest>> entry : incoming.entrySet()) {
            final LinkedHashMap<UUID, TpaRequest> requests = entry.getValue();
            synchronized (requests) {
                final TpaRequest request = requests.get(requester.getUniqueId()); if (request == null) continue;
                if (request.isExpired(now, timeoutMillis())) { requests.remove(requester.getUniqueId()); removeButtonsForRequest(request); if (requests.isEmpty()) incoming.remove(entry.getKey(), requests); continue; }
                names.add(request.recipientName()); break;
            }
        }
        return Collections.unmodifiableList(names);
    }

    private boolean authenticated(Player player) { if (authSystemBridge.isAuthenticated(player)) return true; message(player, "auth-required"); return false; }

    private boolean hasPendingOutgoingRequest(UUID requesterId, long now) {
        for (Map.Entry<UUID, LinkedHashMap<UUID, TpaRequest>> entry : incoming.entrySet()) {
            final LinkedHashMap<UUID, TpaRequest> requests = entry.getValue(); synchronized (requests) {
                final TpaRequest request = requests.get(requesterId); if (request == null) continue;
                if (request.isExpired(now, timeoutMillis())) { requests.remove(requesterId); removeButtonsForRequest(request); if (requests.isEmpty()) incoming.remove(entry.getKey(), requests); continue; }
                return true;
            }
        }
        return false;
    }

    private TpaRequest findRequest(UUID recipientId, String requesterName) {
        final LinkedHashMap<UUID, TpaRequest> requests = incoming.get(recipientId); if (requests == null) return null;
        synchronized (requests) {
            removeExpired(requests, System.currentTimeMillis());
            if (requesterName == null || requesterName.isBlank()) { final List<TpaRequest> values = new ArrayList<>(requests.values()); return values.isEmpty() ? null : values.get(values.size() - 1); }
            for (TpaRequest request : requests.values()) if (request.requesterName().equalsIgnoreCase(requesterName)) return request;
        }
        return null;
    }

    private TpaRequest findRequest(UUID recipientId, UUID requesterId) {
        final LinkedHashMap<UUID, TpaRequest> requests = incoming.get(recipientId); if (requests == null) return null;
        synchronized (requests) { removeExpired(requests, System.currentTimeMillis()); return requests.get(requesterId); }
    }

    private void removeRequest(TpaRequest request) {
        final LinkedHashMap<UUID, TpaRequest> requests = incoming.get(request.recipientId());
        if (requests == null) return;
        synchronized (requests) {
            requests.remove(request.requesterId());
            removeButtonsForRequest(request);
            if (requests.isEmpty()) incoming.remove(request.recipientId(), requests);
        }
    }

    private void expireRequests() {
        final long now = System.currentTimeMillis();
        for (Map.Entry<UUID, LinkedHashMap<UUID, TpaRequest>> entry : incoming.entrySet()) {
            final LinkedHashMap<UUID, TpaRequest> requests = entry.getValue();
            synchronized (requests) {
                final List<TpaRequest> expired = requests.values().stream().filter(request -> request.isExpired(now, timeoutMillis())).toList();
                expired.forEach(request -> {
                    requests.remove(request.requesterId());
                    removeButtonsForRequest(request);
                    final Player requester = Bukkit.getPlayer(request.requesterId());
                    if (requester != null) message(requester, "request-expired", "player", request.recipientName());
                    final Player recipient = Bukkit.getPlayer(request.recipientId());
                    if (recipient != null) message(recipient, "request-expired", "player", request.requesterName());
                });
                if (requests.isEmpty()) incoming.remove(entry.getKey(), requests);
            }
        }
    }

    private void removeExpired(LinkedHashMap<UUID, TpaRequest> requests, long now) {
        final List<TpaRequest> expired = requests.values().stream().filter(request -> request.isExpired(now, timeoutMillis())).toList();
        for (TpaRequest request : expired) {
            requests.remove(request.requesterId());
            removeButtonsForRequest(request);
        }
    }

    private long timeoutMillis() { return TimeUnit.SECONDS.toMillis(Math.max(0, plugin.getConfig().getLong("tpa.request-timeout-seconds", 20))); }
    private void performTeleport(Player player, Location destination) { if (!player.isOnline() || destination.getWorld() == null || !authenticated(player)) return; player.teleport(destination); }

    private int registerButton(Player player, ButtonActionType type, UUID targetId) {
        if (player == null) return -1;
        final int token = nextButtonToken();
        buttonActions.computeIfAbsent(player.getUniqueId(), ignored -> new ConcurrentHashMap<>())
                .put(token, new ButtonAction(token, type, targetId));
        return token;
    }

    private int nextButtonToken() {
        int token = buttonToken.updateAndGet(value -> value >= 2_000_000_000 ? 1000 : value + 1);
        boolean alreadyUsed;
        do {
            alreadyUsed = false;
            for (ConcurrentMap<Integer, ButtonAction> actions : buttonActions.values()) {
                if (actions.containsKey(token)) {
                    alreadyUsed = true;
                    token = buttonToken.incrementAndGet();
                    break;
                }
            }
        } while (alreadyUsed);
        return token;
    }

    private void removeButtonsForRequest(TpaRequest request) {
        removeButton(request.recipientId(), request.requesterId(), ButtonActionType.ACCEPT);
        removeButton(request.recipientId(), request.requesterId(), ButtonActionType.DENY);
        removeButton(request.requesterId(), request.recipientId(), ButtonActionType.CANCEL);
    }

    private void removeButton(UUID ownerId, UUID targetId, ButtonActionType type) {
        final ConcurrentMap<Integer, ButtonAction> actions = buttonActions.get(ownerId);
        if (actions == null) return;
        actions.entrySet().removeIf(entry -> entry.getValue().type() == type && entry.getValue().targetId().equals(targetId));
        if (actions.isEmpty()) buttonActions.remove(ownerId, actions);
    }

    void handleCustomButton(Player player, String identifier) {
        if (player == null || !player.isOnline()) return;
        if (!identifier.startsWith(BUTTON_IDENTIFIER_PREFIX)) return;

        final String[] parts = identifier.substring(BUTTON_IDENTIFIER_PREFIX.length()).split("/");
        if (parts.length != 2) return;

        final ButtonActionType type;
        try { type = ButtonActionType.valueOf(parts[0].toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ignored) { return; }

        final int token;
        try { token = Integer.parseInt(parts[1]); }
        catch (NumberFormatException ignored) { return; }

        final ConcurrentMap<Integer, ButtonAction> actions = buttonActions.get(player.getUniqueId());
        final ButtonAction action = actions == null ? null : actions.get(token);
        if (action == null || action.type() != type) return;

        actions.remove(token, action);
        if (actions.isEmpty()) buttonActions.remove(player.getUniqueId(), actions);

        switch (action.type()) {
            case ACCEPT -> accept(player, action.targetId());
            case DENY -> deny(player, action.targetId());
            case CANCEL -> cancel(player, action.targetId());
        }
    }

    private void sendRequestMessage(Player recipient, Player requester, boolean here) {
        String raw = plugin.getConfig().getString(here ? "messages.request-here-received" : "messages.request-received", "");
        raw = raw.replace("{player}", coloredPlayer(requester));

        Component row = legacy(raw).append(Component.newline());
        row = row.append(legacy("§fClique "));
        if (plugin.getConfig().getBoolean("buttons.accept.enabled", true)) row = row.append(buttonComponent("buttons.accept.text", recipient, ButtonActionType.ACCEPT, requester.getUniqueId()));
        row = row.append(legacy("§f para aceitar ou Clique "));
        if (plugin.getConfig().getBoolean("buttons.deny.enabled", true)) row = row.append(buttonComponent("buttons.deny.text", recipient, ButtonActionType.DENY, requester.getUniqueId()));
        row = row.append(legacy("§f!"));
        recipient.sendMessage(row);
    }

    private void sendCancelButton(Player requester, Player recipient) {
        if (!plugin.getConfig().getBoolean("buttons.cancel.enabled", true)) return;
        requester.sendMessage(buttonComponent("buttons.cancel.text", requester, ButtonActionType.CANCEL, recipient.getUniqueId()));
    }

    private Component buttonComponent(String textPath, Player buttonOwner, ButtonActionType type, UUID targetId) {
        final String text = color(plugin.getConfig().getString(textPath, "AQUI"));
        final int token = registerButton(buttonOwner, type, targetId);
        if (token < 0) return Component.empty();

        final Key key = Key.key("essentialsplus", "tpa/" + type.name().toLowerCase(Locale.ROOT) + "/" + token);
        return legacy(text).clickEvent(ClickEvent.custom(key, BinaryTagHolder.binaryTagHolder("{}")));
    }

    private Component legacy(String text) { return LEGACY_SERIALIZER.deserialize(text == null ? "" : text); }

    private void message(Player player, String path, String... replacements) {
        if (player == null || !player.isOnline()) return;
        String raw = plugin.getConfig().getString("messages." + path, "");
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            String replacement = replacements[i + 1];
            if ("player".equals(replacements[i])) { Player target = Bukkit.getPlayerExact(replacement); if (target != null) replacement = coloredPlayer(target); }
            raw = raw.replace("{" + replacements[i] + "}", replacement);
        }
        final String colored = color(raw);
        if (!chatPlusBridge.sendSystemMessage(player, colored)) { final String prefix = plugin.getConfig().getString("messages.prefix", ""); player.sendMessage(color(prefix) + colored); }
    }

    private String coloredPlayer(Player player) { if (player == null) return ""; String color = cargoNicknameColor(player); return color + player.getName(); }

    private String cargoNicknameColor(Player player) {
        if (player == null || !player.isOnline()) return "§f";
        Plugin current = Bukkit.getPluginManager().getPlugin("CargoPlus");
        if (current == null || !current.isEnabled()) return "§f";
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
                        return "§f";
                    }
                }
            }
        }
        try {
            Object api = cargoApiMethod.invoke(current);
            Object result = cargoNicknameColorMethod.invoke(api, player.getUniqueId());
            return result instanceof String value && !value.isBlank() ? value : "§f";
        } catch (ReflectiveOperationException | LinkageError ex) { return "§f"; }
    }

    private String color(String value) { return value == null ? "" : value.replace('&', '§'); }

    @EventHandler public void onDeath(PlayerDeathEvent event) { removeAllRequestsFor(event.getPlayer().getUniqueId()); }
    @EventHandler public void onQuit(PlayerQuitEvent event) { removeAllRequestsFor(event.getPlayer().getUniqueId()); }
    private void removeAllRequestsFor(UUID playerId) { incoming.remove(playerId); buttonActions.remove(playerId); }

    private enum ButtonActionType { ACCEPT, DENY, CANCEL }
    private record ButtonAction(int token, ButtonActionType type, UUID targetId) { }
}
