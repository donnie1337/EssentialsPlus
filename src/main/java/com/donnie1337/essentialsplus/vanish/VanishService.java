package com.donnie1337.essentialsplus.vanish;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VanishService {
    private static final String VANISH_PERMISSION = "essentialsplus.vanish";
    private static final String VANISH_SUFFIX = "§7[ɪɴᴠɪsɪᴠᴇʟ]";

    private final Plugin plugin;
    private final Set<UUID> vanished = ConcurrentHashMap.newKeySet();
    private final Map<UUID, TeamSuffixState> suffixStates = new ConcurrentHashMap<>();

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

        if (value) {
            vanished.add(player.getUniqueId());
            applyVanishSuffix(player);

            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (viewer.equals(player)) continue;
                if (canSeeVanished(viewer)) viewer.showPlayer(plugin, player);
                else viewer.hidePlayer(plugin, player);
            }
        } else {
            vanished.remove(player.getUniqueId());
            removeVanishSuffix(player);

            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (!viewer.equals(player)) viewer.showPlayer(plugin, player);
            }
        }

        return true;
    }

    public void applyTo(Player player) {
        if (player == null) return;

        if (isVanished(player)) {
            applyVanishSuffix(player);
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (viewer.equals(player)) continue;
                if (canSeeVanished(viewer)) viewer.showPlayer(plugin, player);
                else viewer.hidePlayer(plugin, player);
            }
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
        removeVanishSuffix(player);

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(player)) viewer.showPlayer(plugin, player);
        }
    }

    public void clear() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isVanished(player)) remove(player);
        }
        suffixStates.clear();
        vanished.clear();
    }

    /**
     * Uses the player's existing scoreboard team instead of creating another
     * TextDisplay nametag. CargoPlus/another nametag system therefore keeps
     * full control over the prefix and nickname, while EssentialsPlus only
     * contributes the vanish suffix.
     */
    private void applyVanishSuffix(Player player) {
        Team team = findTeam(player);
        if (team == null) return;

        UUID uuid = player.getUniqueId();
        TeamSuffixState state = suffixStates.get(uuid);
        if (state == null) {
            String originalSuffix = team.getSuffix();
            suffixStates.put(uuid, new TeamSuffixState(team.getName(), originalSuffix));
            state = suffixStates.get(uuid);
        }

        String originalSuffix = state.originalSuffix();
        String expectedSuffix = originalSuffix + " " + VANISH_SUFFIX;
        if (!team.getSuffix().equals(expectedSuffix)) {
            team.setSuffix(expectedSuffix);
        }
    }

    private void removeVanishSuffix(Player player) {
        UUID uuid = player.getUniqueId();
        TeamSuffixState state = suffixStates.remove(uuid);
        if (state == null) return;

        Scoreboard scoreboard = getMainScoreboard();
        Team team = scoreboard.getTeam(state.teamName());
        if (team == null) return;

        team.setSuffix(state.originalSuffix());
    }

    private Team findTeam(Player player) {
        Scoreboard scoreboard = getMainScoreboard();
        for (Team team : scoreboard.getTeams()) {
            if (team.hasEntry(player.getName())) return team;
        }
        return null;
    }

    private Scoreboard getMainScoreboard() {
        return Bukkit.getScoreboardManager().getMainScoreboard();
    }

    private boolean canSeeVanished(Player viewer) {
        return viewer != null && viewer.hasPermission(VANISH_PERMISSION);
    }

    private record TeamSuffixState(String teamName, String originalSuffix) {
    }
}
