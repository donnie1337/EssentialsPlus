package com.donnie1337.essentialsplus.vanish;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VanishService {
    private static final String VANISH_PERMISSION = "essentialsplus.vanish";

    private final Plugin plugin;
    private final Set<UUID> vanished = ConcurrentHashMap.newKeySet();

    public VanishService(Plugin plugin) {
        this.plugin = plugin;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public boolean isVanished(Player player) {
        return player != null && vanished.contains(player.getUniqueId());
    }

    public boolean toggle(Player player) {
        if (player == null) return false;
        return setVanished(player, !isVanished(player));
    }

    public boolean setVanished(Player player, boolean value) {
        if (player == null) return false;
        if (value) vanished.add(player.getUniqueId());
        else vanished.remove(player.getUniqueId());

        if (value) {
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (viewer.equals(player)) continue;
                if (canSeeVanished(viewer)) viewer.showPlayer(plugin, player);
                else viewer.hidePlayer(plugin, player);
            }
            setListed(player, false);
        } else {
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (!viewer.equals(player)) viewer.showPlayer(plugin, player);
            }
            setListed(player, true);
        }
        return true;
    }

    public void applyTo(Player player) {
        if (player == null) return;

        if (isVanished(player)) {
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (viewer.equals(player)) continue;
                if (canSeeVanished(viewer)) viewer.showPlayer(plugin, player);
                else viewer.hidePlayer(plugin, player);
            }
            setListed(player, false);
        }

        for (Player vanishedPlayer : Bukkit.getOnlinePlayers()) {
            if (vanishedPlayer.equals(player) || !isVanished(vanishedPlayer)) continue;
            if (canSeeVanished(player)) player.showPlayer(plugin, vanishedPlayer);
            else player.hidePlayer(plugin, vanishedPlayer);
        }
    }

    public void remove(Player player) {
        if (player == null) return;
        vanished.remove(player.getUniqueId());
        setListed(player, true);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(player)) viewer.showPlayer(plugin, player);
        }
    }

    public void clear() {
        for (Player player : Bukkit.getOnlinePlayers()) remove(player);
        vanished.clear();
    }

    private boolean canSeeVanished(Player viewer) {
        return viewer != null && viewer.hasPermission(VANISH_PERMISSION);
    }

    private void setListed(Player player, boolean listed) {
        try {
            Method method = player.getClass().getMethod("setListed", boolean.class);
            method.invoke(player, listed);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Older Bukkit/Paper APIs may not expose setListed; hidePlayer still provides vanish behavior.
        }
    }
}
