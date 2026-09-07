package com.donnie1337.essentialsplus.teleport;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public final class TeleportService implements Listener {

    private final JavaPlugin plugin;
    private final ConcurrentMap<UUID, LinkedHashMap<UUID, TpaRequest>> incoming = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, BukkitTask> pendingTeleports = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Location> pendingLocations = new ConcurrentHashMap<>();
    private BukkitTask expirationTask;

    public TeleportService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        expirationTask = Bukkit.getScheduler().runTaskTimer(plugin, this::expireRequests, 20L, 20L);
    }

    public void shutdown() {
        if (expirationTask != null) {
            expirationTask.cancel();
            expirationTask = null;
        }
        pendingTeleports.values().forEach(BukkitTask::cancel);
        pendingTeleports.clear();
        pendingLocations.clear();
        incoming.clear();
    }

    public boolean enabled() {
        return plugin.getConfig().getBoolean("tpa.enabled", true);
    }

    public boolean request(Player requester, Player recipient, boolean here) {
        if (!enabled()) {
            message(requester, "disabled");
            return false;
        }
        if (requester.getUniqueId().equals(recipient.getUniqueId())) {
            message(requester, "cannot-self");
            return false;
        }

        final long now = System.currentTimeMillis();
        if (hasPendingOutgoingRequest(requester.getUniqueId(), now)) {
            message(requester, "pending-request");
            return false;
        }

        final int max = Math.max(1, plugin.getConfig().getInt("tpa.max-pending-requests", 5));
        final LinkedHashMap<UUID, TpaRequest> requests = incoming.computeIfAbsent(recipient.getUniqueId(), ignored -> new LinkedHashMap<>());
        synchronized (requests) {
            removeExpired(requests, now);
            requests.remove(requester.getUniqueId());
            while (requests.size() >= max) {
                final UUID oldest = requests.keySet().iterator().next();
                requests.remove(oldest);
            }
            if (requests.size() >= max) {
                message(requester, "too-many-requests");
                return false;
            }

            final Location destination = (here ? requester.getLocation() : recipient.getLocation()).clone();
            requests.put(requester.getUniqueId(), new TpaRequest(
                    requester.getUniqueId(), requester.getName(), recipient.getUniqueId(), recipient.getName(), here, destination, now));
        }

        message(requester, "request-sent", "player", recipient.getName());
        sendRequestMessage(recipient, requester.getName(), here);
        return true;
    }

    public boolean accept(Player recipient, String requesterName) {
        final TpaRequest request = findRequest(recipient.getUniqueId(), requesterName);
        if (request == null) {
            message(recipient, "no-request");
            return false;
        }
        removeRequest(request);

        final Player requester = Bukkit.getPlayer(request.requesterId());
        if (requester == null || !requester.isOnline()) {
            message(recipient, "target-offline");
            return false;
        }

        final Player teleported = request.here() ? recipient : requester;
        final Location destination = request.destination().clone();
        scheduleTeleport(teleported, destination);
        message(recipient, "request-accepted", "player", request.requesterName());
        message(requester, "request-accepted-sender", "player", recipient.getName());
        return true;
    }

    public boolean deny(Player recipient, String requesterName) {
        final TpaRequest request = findRequest(recipient.getUniqueId(), requesterName);
        if (request == null) {
            message(recipient, "no-request");
            return false;
        }
        removeRequest(request);
        message(recipient, "request-denied", "player", request.requesterName());
        final Player requester = Bukkit.getPlayer(request.requesterId());
        if (requester != null) {
            message(requester, "request-denied-sender", "player", recipient.getName());
        }
        return true;
    }

    public boolean cancel(Player requester, String recipientName) {
        boolean removed = false;
        for (Map.Entry<UUID, LinkedHashMap<UUID, TpaRequest>> entry : incoming.entrySet()) {
            final LinkedHashMap<UUID, TpaRequest> requests = entry.getValue();
            synchronized (requests) {
                final TpaRequest request = requests.get(requester.getUniqueId());
                if (request != null && (recipientName == null || request.recipientName().equalsIgnoreCase(recipientName))) {
                    requests.remove(requester.getUniqueId());
                    removed = true;
                    final Player recipient = Bukkit.getPlayer(entry.getKey());
                    if (recipient != null) {
                        message(recipient, "request-cancelled");
                    }
                    if (recipientName != null) {
                        break;
                    }
                }
            }
        }
        if (removed) {
            message(requester, "request-cancelled");
        }
        return removed;
    }

    public List<String> pendingRequesterNames(Player recipient) {
        final LinkedHashMap<UUID, TpaRequest> requests = incoming.get(recipient.getUniqueId());
        if (requests == null) return List.of();
        synchronized (requests) {
            removeExpired(requests, System.currentTimeMillis());
            return Collections.unmodifiableList(requests.values().stream().map(TpaRequest::requesterName).toList());
        }
    }

    private boolean hasPendingOutgoingRequest(UUID requesterId, long now) {
        for (Map.Entry<UUID, LinkedHashMap<UUID, TpaRequest>> entry : incoming.entrySet()) {
            final LinkedHashMap<UUID, TpaRequest> requests = entry.getValue();
            synchronized (requests) {
                final TpaRequest request = requests.get(requesterId);
                if (request == null) continue;
                if (request.isExpired(now, timeoutMillis())) {
                    requests.remove(requesterId);
                    if (requests.isEmpty()) incoming.remove(entry.getKey(), requests);
                    continue;
                }
                return true;
            }
        }
        return false;
    }

    private TpaRequest findRequest(UUID recipientId, String requesterName) {
        final LinkedHashMap<UUID, TpaRequest> requests = incoming.get(recipientId);
        if (requests == null) return null;
        synchronized (requests) {
            removeExpired(requests, System.currentTimeMillis());
            if (requesterName == null || requesterName.isBlank()) {
                final List<TpaRequest> values = new ArrayList<>(requests.values());
                return values.isEmpty() ? null : values.get(values.size() - 1);
            }
            for (TpaRequest request : requests.values()) {
                if (request.requesterName().equalsIgnoreCase(requesterName)) return request;
            }
        }
        return null;
    }

    private void removeRequest(TpaRequest request) {
        final LinkedHashMap<UUID, TpaRequest> requests = incoming.get(request.recipientId());
        if (requests == null) return;
        synchronized (requests) {
            requests.remove(request.requesterId());
            if (requests.isEmpty()) incoming.remove(request.recipientId(), requests);
        }
    }

    private void expireRequests() {
        final long now = System.currentTimeMillis();
        for (Map.Entry<UUID, LinkedHashMap<UUID, TpaRequest>> entry : incoming.entrySet()) {
            final LinkedHashMap<UUID, TpaRequest> requests = entry.getValue();
            synchronized (requests) {
                final List<TpaRequest> expired = requests.values().stream()
                        .filter(request -> request.isExpired(now, timeoutMillis()))
                        .toList();
                expired.forEach(request -> {
                    requests.remove(request.requesterId());
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
        requests.values().removeIf(request -> request.isExpired(now, timeoutMillis()));
    }

    private long timeoutMillis() {
        return TimeUnit.SECONDS.toMillis(Math.max(0, plugin.getConfig().getLong("tpa.request-timeout-seconds", 20)));
    }

    private void scheduleTeleport(Player player, Location destination) {
        cancelPendingTeleport(player.getUniqueId());
        final long delaySeconds = Math.max(0, plugin.getConfig().getLong("tpa.teleport-delay-seconds", 5));
        if (delaySeconds == 0 || player.hasPermission("essentialsplus.tpa.bypass.delay")) {
            performTeleport(player, destination);
            return;
        }

        final Location start = player.getLocation().clone();
        pendingLocations.put(player.getUniqueId(), start);
        message(player, "teleporting", "seconds", String.valueOf(delaySeconds));
        final BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingTeleports.remove(player.getUniqueId());
            pendingLocations.remove(player.getUniqueId());
            if (!player.isOnline() || !sameBlock(start, player.getLocation())) {
                message(player, "teleport-cancelled");
                return;
            }
            performTeleport(player, destination);
        }, delaySeconds * 20L);
        pendingTeleports.put(player.getUniqueId(), task);
    }

    private void performTeleport(Player player, Location destination) {
        if (!player.isOnline() || destination.getWorld() == null) {
            return;
        }
        player.teleportAsync(destination).thenAccept(success -> {
            if (!success) {
                Bukkit.getScheduler().runTask(plugin, () -> message(player, "teleport-failed"));
            }
        });
    }

    private void cancelPendingTeleport(UUID playerId) {
        final BukkitTask task = pendingTeleports.remove(playerId);
        if (task != null) task.cancel();
        pendingLocations.remove(playerId);
    }

    private boolean sameBlock(Location a, Location b) {
        return b != null
                && a.getWorld() == b.getWorld()
                && a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }

    private void sendRequestMessage(Player recipient, String requesterName, boolean here) {
        final String key = here ? "request-here-received" : "request-received";
        message(recipient, key, "player", requesterName);

        final Component accept = button(
                "buttons.accept.text",
                "buttons.accept.hover",
                recipient.getUniqueId(),
                requesterName,
                true
        );
        final Component deny = button(
                "buttons.deny.text",
                "buttons.deny.hover",
                recipient.getUniqueId(),
                requesterName,
                false
        );
        recipient.sendMessage(Component.text("  ").append(accept).append(Component.text("  ")).append(deny));
    }

    private Component button(String textPath, String hoverPath, UUID recipientId, String requesterName, boolean accept) {
        final String text = color(plugin.getConfig().getString(textPath, ""));
        final String hover = color(plugin.getConfig().getString(hoverPath, ""));
        final long lifetimeSeconds = Math.max(1, plugin.getConfig().getLong("tpa.request-timeout-seconds", 20));
        final ClickCallback<Audience> callback = audience -> {
            if (!(audience instanceof Player player) || !player.getUniqueId().equals(recipientId)) {
                return;
            }
            if (accept) {
                accept(player, requesterName);
            } else {
                deny(player, requesterName);
            }
        };
        final ClickCallback.Options options = ClickCallback.Options.builder()
                .lifetime(Duration.ofSeconds(lifetimeSeconds))
                .uses(1)
                .build();
        return Component.text(text)
                .clickEvent(ClickEvent.callback(callback, options))
                .hoverEvent(HoverEvent.showText(Component.text(hover)));
    }

    private void message(Player player, String path, String... replacements) {
        String raw = plugin.getConfig().getString("messages." + path, "");
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            raw = raw.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        final String prefix = plugin.getConfig().getString("messages.prefix", "");
        player.sendMessage(Component.text(color(prefix + raw)));
    }

    private String color(String value) {
        return value == null ? "" : value.replace('&', '§');
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!plugin.getConfig().getBoolean("tpa.cancel-on-move", true)) return;
        final Location start = pendingLocations.get(event.getPlayer().getUniqueId());
        if (start == null) return;
        if (!sameBlock(start, event.getTo())) {
            cancelPendingTeleport(event.getPlayer().getUniqueId());
            message(event.getPlayer(), "teleport-cancelled");
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!plugin.getConfig().getBoolean("tpa.cancel-on-damage", false)) return;
        if (event.getEntity() instanceof Player player && pendingTeleports.containsKey(player.getUniqueId())) {
            cancelPendingTeleport(player.getUniqueId());
            message(player, "teleport-cancelled");
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (plugin.getConfig().getBoolean("tpa.cancel-on-death", true)) {
            cancelPendingTeleport(event.getPlayer().getUniqueId());
        }
        removeAllRequestsFor(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (plugin.getConfig().getBoolean("tpa.cancel-on-disconnect", true)) {
            cancelPendingTeleport(event.getPlayer().getUniqueId());
            removeAllRequestsFor(event.getPlayer().getUniqueId());
        }
    }

    private void removeAllRequestsFor(UUID playerId) {
        incoming.remove(playerId);
        for (LinkedHashMap<UUID, TpaRequest> requests : incoming.values()) {
            synchronized (requests) {
                requests.remove(playerId);
            }
        }
    }
}
