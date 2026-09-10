package com.donnie1337.essentialsplus.vanish;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VanishService {
    private static final String VANISH_PERMISSION = "essentialsplus.vanish";
    private static final String VANISH_SUFFIX_TEXT = "[ɪɴᴠɪsɪᴠᴇʟ]";
    private static final Component VANISH_HOVER = Component.text("Este jogador está invisível para jogadores.");

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
            scheduleSuffixRefresh(player);

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
            scheduleSuffixRefresh(player);
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

        // CargoPlus may recreate the team after /v. If the active team changed,
        // bind the vanish state to the new team instead of keeping a stale name.
        if (state == null || !state.teamName().equals(team.getName())) {
            Component originalSuffix = readCurrentSuffix(team);
            state = new TeamSuffixState(team.getName(), originalSuffix);
            suffixStates.put(uuid, state);
        }

        Component expectedSuffix = state.originalSuffix()
                .append(Component.text(" "))
                .append(Component.text(VANISH_SUFFIX_TEXT)
                        .color(NamedTextColor.GRAY)
                        .hoverEvent(HoverEvent.showText(VANISH_HOVER)));

        Component currentSuffix = readCurrentSuffix(team);
        if (!expectedSuffix.equals(currentSuffix)) {
            writeSuffix(team, expectedSuffix);
        }
    }

    /**
     * CargoPlus creates/recreates the player's scoreboard team. A short delayed
     * refresh makes vanish resilient to that ordering race during /v and login.
     */
    private void scheduleSuffixRefresh(Player player) {
        UUID uuid = player.getUniqueId();
        for (long delay : new long[]{1L, 3L, 6L}) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline() || !vanished.contains(uuid)) return;
                applyVanishSuffix(player);
            }, delay);
        }
    }

    private void removeVanishSuffix(Player player) {
        UUID uuid = player.getUniqueId();
        TeamSuffixState state = suffixStates.remove(uuid);
        if (state == null) return;

        Team team = findTeam(player);
        if (team == null) {
            Scoreboard scoreboard = getMainScoreboard();
            team = scoreboard.getTeam(state.teamName());
        }
        if (team == null) return;

        writeSuffix(team, state.originalSuffix());
    }

    private Component readCurrentSuffix(Team team) {
        Component paperSuffix = readPaperSuffix(team);
        if (paperSuffix != null) return paperSuffix;

        String legacySuffix = team.getSuffix();
        if (legacySuffix == null || legacySuffix.isEmpty()) return Component.empty();
        return LegacyComponentSerializer.legacySection().deserialize(legacySuffix);
    }

    private Component readPaperSuffix(Team team) {
        try {
            Method method = team.getClass().getMethod("suffix");
            Object result = method.invoke(team);
            return result instanceof Component component ? component : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private void writeSuffix(Team team, Component suffix) {
        if (writePaperSuffix(team, suffix)) return;
        team.setSuffix(LegacyComponentSerializer.legacySection().serialize(suffix));
    }

    private boolean writePaperSuffix(Team team, Component suffix) {
        try {
            for (Method method : team.getClass().getMethods()) {
                if (!method.getName().equals("suffix") || method.getParameterCount() != 1) continue;
                if (!method.getParameterTypes()[0].isInstance(suffix)) continue;
                method.invoke(team, suffix);
                return true;
            }
        } catch (ReflectiveOperationException ignored) {
            // Fall back to the legacy String suffix API below.
        }
        return false;
    }

    private Team findTeam(Player player) {
        // Prefer the scoreboard actually assigned to the player. This is the
        // scoreboard CargoPlus normally mutates for the player's nametag.
        Scoreboard playerScoreboard = player.getScoreboard();
        Team team = findTeam(playerScoreboard, player);
        if (team != null) return team;

        Scoreboard main = getMainScoreboard();
        if (main != playerScoreboard) {
            team = findTeam(main, player);
            if (team != null) return team;
        }
        return null;
    }

    private Team findTeam(Scoreboard scoreboard, Player player) {
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

    private record TeamSuffixState(String teamName, Component originalSuffix) {
    }
}
