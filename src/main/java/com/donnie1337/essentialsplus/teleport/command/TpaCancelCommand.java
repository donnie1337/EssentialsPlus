package com.donnie1337.essentialsplus.teleport.command;

import com.donnie1337.essentialsplus.teleport.TeleportService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public final class TpaCancelCommand implements CommandExecutor, TabCompleter {
    private final TeleportService service;
    public TpaCancelCommand(TeleportService service) { this.service = service; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Este comando só pode ser usado por jogadores."); return true; }
        if (!player.hasPermission("essentialsplus.tpacancel")) { player.sendMessage("§cVocê não tem permissão para isso."); return true; }
        if (args.length > 1) { player.sendMessage("§cUso: /tpacancel [jogador]"); return true; }
        service.cancel(player, args.length == 1 ? args[0] : null);
        return true;
    }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player) || args.length != 1) return List.of();
        final String prefix = args[0].toLowerCase();
        return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                .filter(n -> !n.equalsIgnoreCase(player.getName()) && n.toLowerCase().startsWith(prefix)).sorted().toList();
    }
}
