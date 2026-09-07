package com.donnie1337.essentialsplus.teleport.command;

import com.donnie1337.essentialsplus.teleport.TeleportService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public final class TpaAcceptCommand implements CommandExecutor, TabCompleter {
    private static final String PERMISSION = "essentialsplus.tpaccept";
    private final TeleportService service;
    public TpaAcceptCommand(TeleportService service) { this.service = service; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Este comando só pode ser usado por jogadores."); return true; }
        if (!player.hasPermission(PERMISSION)) { player.sendMessage("§cVocê não tem permissão para isso."); return true; }
        if (args.length > 1) { player.sendMessage("§cUso: /tpaccept [jogador]"); return true; }
        service.accept(player, args.length == 1 ? args[0] : null);
        return true;
    }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player) || !player.isOnline() || !player.hasPermission(PERMISSION) || args.length != 1) return List.of();
        final String prefix = args[0].toLowerCase(java.util.Locale.ROOT);
        return service.pendingRequesterNames(player).stream().filter(n -> n.toLowerCase(java.util.Locale.ROOT).startsWith(prefix)).sorted().toList();
    }
}
