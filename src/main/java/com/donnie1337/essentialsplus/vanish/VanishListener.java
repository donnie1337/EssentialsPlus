package com.donnie1337.essentialsplus.vanish;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.entity.Player;

import java.util.Locale;

public final class VanishListener implements Listener {
    private final VanishService service;

    public VanishListener(VanishService service) {
        this.service = service;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        for (Player vanished : Bukkit.getOnlinePlayers()) {
            if (!vanished.equals(event.getPlayer()) && service.isVanished(vanished)) {
                event.getPlayer().hidePlayer(servicePlugin(), vanished);
            }
        }
        service.applyTo(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        service.remove(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onTpaCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (message == null || message.length() < 2 || message.charAt(0) != '/') return;
        String[] parts = message.substring(1).trim().split("\\s+");
        if (parts.length == 0) return;
        String command = parts[0].toLowerCase(Locale.ROOT);
        if (command.contains(":")) command = command.substring(command.lastIndexOf(':') + 1);
        if (!command.equals("tpa") && !command.equals("tpaqui") && !command.equals("tpaccept") && !command.equals("tpdeny") && !command.equals("tpacancel") && !command.equals("tpaceitar") && !command.equals("tpnegar") && !command.equals("tpcancelar")) return;

        Player sender = event.getPlayer();
        if (service.isVanished(sender)) {
            event.setCancelled(true);
            sender.sendMessage("§b§lᴛᴘᴀ §8• §cVocê não pode usar TPA enquanto estiver invisível.");
            return;
        }

        // A regra de alvo invisível só se aplica ao envio de /tpa e /tpaqui.
        if (!command.equals("tpa") && !command.equals("tpaqui")) return;
        if (parts.length < 2) return;

        Player target = Bukkit.getPlayerExact(parts[1]);
        if (target == null || !service.isVanished(target)) return;

        if (!sender.hasPermission("essentialsplus.vanish")) {
            event.setCancelled(true);
            sender.sendMessage("§b§lᴛᴘᴀ §8• §cJogador não encontrado.");
            return;
        }

        event.setCancelled(true);
        sender.sendMessage("§b§lᴛᴘᴀ §8• §cNão foi possível mandar TPA pois " + target.getName() + " está invisível.");
    }

    private org.bukkit.plugin.Plugin servicePlugin() {
        return service.getPlugin();
    }
}
