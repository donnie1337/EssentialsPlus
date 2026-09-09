package com.donnie1337.essentialsplus.teleport;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

public final class TpaCustomClickListener implements Listener {
    private static final String PERMISSION = "essentialsplus.tpa";

    private final Plugin plugin;
    private final TeleportService teleportService;

    public TpaCustomClickListener(Plugin plugin, TeleportService teleportService) {
        this.plugin = plugin;
        this.teleportService = teleportService;
    }

    public void register(PluginManager pluginManager) {
        pluginManager.registerEvents(this, plugin);
        plugin.getLogger().info("Botões de TPA clicáveis registrados usando callback interno de chat.");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        String prefix = "/" + TeleportService.TPA_BUTTON_COMMAND;
        if (!message.regionMatches(true, 0, prefix, 0, prefix.length())) return;

        String remainder = message.substring(prefix.length()).trim();
        if (remainder.isEmpty() || remainder.indexOf(' ') >= 0) {
            event.setCancelled(true);
            return;
        }

        int token;
        try {
            token = Integer.parseInt(remainder);
        } catch (NumberFormatException ignored) {
            event.setCancelled(true);
            return;
        }

        Player player = event.getPlayer();
        event.setCancelled(true);
        if (!player.hasPermission(PERMISSION)) return;
        teleportService.handleButton(player, token);
    }
}
