package com.donnie1337.essentialsplus.marriage;

import com.donnie1337.essentialsplus.EssentialsPlus;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class MarriageListener implements Listener {

    private final EssentialsPlus plugin;
    private final MarriageService service;

    public MarriageListener(EssentialsPlus plugin, MarriageService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        for (MarriageService.Proposal proposal : service.cancelRequestsFor(event.getPlayer().getUniqueId())) {
            if (proposal.requesterId().equals(event.getPlayer().getUniqueId())) {
                Player target = Bukkit.getPlayer(proposal.targetId());
                if (target != null) {
                    target.sendMessage(message("pedido-cancelado-desconexao",
                            "&eO pedido de casamento foi cancelado porque {player} saiu do servidor.")
                            .replace("{player}", event.getPlayer().getName()));
                }
            } else {
                Player requester = Bukkit.getPlayer(proposal.requesterId());
                if (requester != null) {
                    requester.sendMessage(message("pedido-cancelado-desconexao",
                            "&eO pedido de casamento foi cancelado porque {player} saiu do servidor.")
                            .replace("{player}", event.getPlayer().getName()));
                }
            }
        }
    }

    private String message(String key, String fallback) {
        String prefix = plugin.getConfig().getString(
                "messages.marry.prefix", "&d[Marry] &r");
        String body = plugin.getConfig().getString("messages.marry." + key, fallback);
        return ChatColor.translateAlternateColorCodes('&', prefix + body);
    }
}
