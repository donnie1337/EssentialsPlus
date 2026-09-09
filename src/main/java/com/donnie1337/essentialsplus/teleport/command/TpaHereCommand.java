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

public final class TpaHereCommand implements CommandExecutor, TabCompleter {
    private static final String PERMISSION = "essentialsplus.tpa";
    private final TeleportService service;
    private final AuthSystemBridge auth = new AuthSystemBridge();
    public TpaHereCommand(TeleportService service) { this.service = service; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Este comando só pode ser usado por jogadores."); return true; }
        if (args.length != 1) { player.sendMessage("§cUso: /tpaqui <jogador>"); return true; }
        if (!player.hasPermission(PERMISSION)) { player.sendMessage("§cVocê não tem permissão para isso."); return true; }
        final Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) { player.sendMessage("§cJogador não encontrado."); return true; }
        if (!auth.isAuthenticated(target)) { player.sendMessage("§cEsse jogador ainda não está autenticado."); return true; }
        service.request(player, target, true);
        return true;
    }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player) || !player.isOnline() || !player.hasPermission(PERMISSION) || args.length != 1) return List.of();
        final String prefix = args[0].toLowerCase(java.util.Locale.ROOT);
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(n -> n.toLowerCase(java.util.Locale.ROOT).startsWith(prefix))
                .sorted()
                .limit(20)
                .toList();
    }
}
