package com.donnie1337.essentialsplus.teleport;

import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TpaCustomClickListener implements Listener {
    private static final String PERMISSION = "essentialsplus.tpa";
    private static final Pattern TOKEN_PATTERN = Pattern.compile("(?:^|[\\s{])token\\s*:\\s*(\\d+)");

    private final Plugin plugin;
    private final TeleportService teleportService;

    public TpaCustomClickListener(Plugin plugin, TeleportService teleportService) {
        this.plugin = plugin;
        this.teleportService = teleportService;
    }

    @EventHandler
    public void onCustomClick(PlayerCustomClickEvent event) {
        if (!event.getIdentifier().equals(Key.key("essentialsplus:tpa_action"))) return;
        if (!(event.getCommonConnection() instanceof PlayerGameConnection connection)) return;

        Player player = connection.getPlayer();
        if (!player.hasPermission(PERMISSION)) return;

        BinaryTagHolder tag = event.getTag();
        if (tag == null) return;

        Matcher matcher = TOKEN_PATTERN.matcher(tag.string());
        if (!matcher.find()) return;

        try {
            int token = Integer.parseInt(matcher.group(1));
            teleportService.handleButton(player, token);
        } catch (NumberFormatException ignored) {
            plugin.getLogger().fine("Token de botão TPA inválido recebido de " + player.getName() + ".");
        }
    }
}
