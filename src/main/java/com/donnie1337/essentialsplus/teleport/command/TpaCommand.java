package com.donnie1337.essentialsplus.teleport.command;

import com.donnie1337.essentialsplus.auth.AuthSystemBridge;
import com.donnie1337.essentialsplus.teleport.TeleportService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public final class TpaCommand implements CommandExecutor, TabCompleter {
    private final TeleportService service;
    private final AuthSystemBridge auth = new AuthSystemBridge();
    public TpaCommand(TeleportService service) { this.service = service; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Este comando só pode ser usado por jogadores."); return true; }
        if (args.length != 1) { player.sendMessage("§cUso: /tpa <jogador>"); return true; }
        if (!player.hasPermission("essentialsplus.tpa")) { player.sendMessage("§cVocê não tem permissão para isso."); return true; }
        final Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) { player.sendMessage("§cJogador não encontrado."); return true; }
        if (!auth.isAuthenticated(target)) { player.sendMessage("§cEsse jogador ainda não está autenticado."); return true; }
        service.request(player, target, false);
        return true;
    }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player) || !player.isOnline()) return List.of();
        if (args.length != 1) return List.of();
        if (!player.hasPermission("essentialsplus.tpa")) return List.of();
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).sorted().toList();
    }
}
