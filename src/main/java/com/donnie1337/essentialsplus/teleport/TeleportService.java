package com.donnie1337.essentialsplus.teleport;

import com.donnie1337.essentialsplus.auth.AuthSystemBridge;
import com.donnie1337.essentialsplus.chat.ChatPlusBridge;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class TeleportService implements Listener {
    private static final String BUTTON_OBJECTIVE = "ep_tpa";

    private final JavaPlugin plugin;
    private final ChatPlusBridge chatPlusBridge;
    private final AuthSystemBridge authSystemBridge;
    private final ConcurrentMap<UUID, LinkedHashMap<UUID, TpaRequest>> incoming = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, ButtonAction> buttonActions = new ConcurrentHashMap<>();
    private final AtomicInteger buttonToken = new AtomicInteger(1000);
    private BukkitTask expirationTask;
    private volatile Plugin cargoPlugin;
    private volatile Method cargoApiMethod;
    private volatile Method cargoNicknameColorMethod;

    public TeleportService(JavaPlugin plugin, ChatPlusBridge chatPlusBridge, AuthSystemBridge authSystemBridge) {
        this.plugin = plugin; this.chatPlusBridge = chatPlusBridge; this.authSystemBridge = authSystemBridge;
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        ensureButtonObjective();
        expirationTask = Bukkit.getScheduler().runTaskTimer(plugin, this::expireRequests, 20L, 20L);
    }

    public void shutdown() {
        if (expirationTask != null) { expirationTask.cancel(); expirationTask = null; }
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
                    requests.remove(requester.getUniqueId()); removed = true;
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
                if (request.isExpired(now, timeoutMillis())) { requests.remove(requester.getUniqueId()); if (requests.isEmpty()) incoming.remove(entry.getKey(), requests); continue; }
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
                if (request.isExpired(now, timeoutMillis())) { requests.remove(requesterId); if (requests.isEmpty()) incoming.remove(entry.getKey(), requests); continue; }
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
        final LinkedHashMap<UUID, TpaRequest> requests = incoming.get(request.recipientId()); if (requests == null) return;
        synchronized (requests) { requests.remove(request.requesterId()); if (requests.isEmpty()) incoming.remove(request.recipientId(), requests); }
    }

    private void expireRequests() {
        final long now = System.currentTimeMillis();
        for (Map.Entry<UUID, LinkedHashMap<UUID, TpaRequest>> entry : incoming.entrySet()) {
            final LinkedHashMap<UUID, TpaRequest> requests = entry.getValue();
            synchronized (requests) {
                final List<TpaRequest> expired = requests.values().stream().filter(request -> request.isExpired(now, timeoutMillis())).toList();
                expired.forEach(request -> { requests.remove(request.requesterId()); final Player requester = Bukkit.getPlayer(request.requesterId()); if (requester != null) message(requester, "request-expired", "player", request.recipientName()); final Player recipient = Bukkit.getPlayer(request.recipientId()); if (recipient != null) message(recipient, "request-expired", "player", request.requesterName()); });
                if (requests.isEmpty()) incoming.remove(entry.getKey(), requests);
            }
        }
    }

    private void removeExpired(LinkedHashMap<UUID, TpaRequest> requests, long now) { requests.values().removeIf(request -> request.isExpired(now, timeoutMillis())); }
    private long timeoutMillis() { return TimeUnit.SECONDS.toMillis(Math.max(0, plugin.getConfig().getLong("tpa.request-timeout-seconds", 20))); }
    private void performTeleport(Player player, Location destination) { if (!player.isOnline() || destination.getWorld() == null || !authenticated(player)) return; player.teleport(destination); }

    private void ensureButtonObjective() {
        if (Bukkit.getScoreboardManager() == null) return;
        Objective objective = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(BUTTON_OBJECTIVE);
        if (objective == null) objective = Bukkit.getScoreboardManager().getMainScoreboard().registerNewObjective(BUTTON_OBJECTIVE, Criteria.TRIGGER, "EssentialsPlus TPA");
        objective.setDisplayName("EssentialsPlus TPA");
    }

    private int registerButton(Player player, ButtonActionType type, UUID targetId) {
        if (player == null) return -1;
        ensureButtonObjective();
        int token = nextButtonToken();
        buttonActions.put(player.getUniqueId(), new ButtonAction(token, type, targetId));
        if (Bukkit.getScoreboardManager() != null) {
            final Objective objective = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(BUTTON_OBJECTIVE);
            if (objective != null) {
                Score score = objective.getScore(player.getName());
                score.setScore(token);
                score.setTriggerable(true);
            }
        }
        return token;
    }

    private int nextButtonToken() {
        int token = buttonToken.updateAndGet(value -> value >= 2_000_000_000 ? 1000 : value + 1);
        while (buttonActions.values().stream().anyMatch(action -> action.token() == token)) token = buttonToken.incrementAndGet();
        return token;
    }

    @EventHandler public void onButtonCommand(PlayerCommandPreprocessEvent event) {
        final Player player = event.getPlayer();
        final String command = event.getMessage();
        final String prefix = "/trigger " + BUTTON_OBJECTIVE + " set ";
        if (!command.regionMatches(true, 0, prefix, 0, prefix.length())) return;
        final String value = command.substring(prefix.length()).trim();
        if (!value.matches("\\d+")) return;
        final ButtonAction action = buttonActions.get(player.getUniqueId());
        if (action == null || !Integer.toString(action.token()).equals(value)) return;
        event.setCancelled(true);
        buttonActions.remove(player.getUniqueId(), action);
        switch (action.type()) {
            case ACCEPT -> accept(player, action.targetId());
            case DENY -> deny(player, action.targetId());
            case CANCEL -> cancel(player, action.targetId());
        }
    }

    private void sendRequestMessage(Player recipient, Player requester, boolean here) {
        String raw = plugin.getConfig().getString(here ? "messages.request-here-received" : "messages.request-received", "");
        raw = raw.replace("{player}", coloredPlayer(requester));
        List<BaseComponent> row = new ArrayList<>();
        Collections.addAll(row, TextComponent.fromLegacyText(color(raw)));
        row.add(new TextComponent("\n"));
        appendLegacy(row, "§fClique ");
        if (plugin.getConfig().getBoolean("buttons.accept.enabled", true)) appendButton(row, "buttons.accept.text", recipient, ButtonActionType.ACCEPT, requester.getUniqueId());
        appendLegacy(row, "§f para aceitar ou Clique ");
        if (plugin.getConfig().getBoolean("buttons.deny.enabled", true)) appendButton(row, "buttons.deny.text", recipient, ButtonActionType.DENY, requester.getUniqueId());
        appendLegacy(row, "§f!");
        recipient.spigot().sendMessage(row.toArray(new BaseComponent[0]));
    }

    private void sendCancelButton(Player requester, Player recipient) {
        if (!plugin.getConfig().getBoolean("buttons.cancel.enabled", true)) return;
        final List<BaseComponent> row = new ArrayList<>();
        appendButton(row, "buttons.cancel.text", requester, ButtonActionType.CANCEL, recipient.getUniqueId());
        requester.spigot().sendMessage(row.toArray(new BaseComponent[0]));
    }

    private void appendButton(List<BaseComponent> row, String textPath, Player buttonOwner, ButtonActionType type, UUID targetId) {
        final BaseComponent[] components = TextComponent.fromLegacyText(color(plugin.getConfig().getString(textPath, "AQUI")));
        final int token = registerButton(buttonOwner, type, targetId);
        if (token < 0) return;
        final ClickEvent click = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/trigger " + BUTTON_OBJECTIVE + " set " + token);
        for (BaseComponent component : components) component.setClickEvent(click);
        Collections.addAll(row, components);
    }

    private void appendLegacy(List<BaseComponent> row, String text) { Collections.addAll(row, TextComponent.fromLegacyText(color(text))); }

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
