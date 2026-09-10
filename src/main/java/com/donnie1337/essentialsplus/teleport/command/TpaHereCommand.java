package com.donnie1337.essentialsplus.teleport.command;

import com.donnie1337.essentialsplus.EssentialsPlus;
import com.donnie1337.essentialsplus.auth.AuthSystemBridge;
import com.donnie1337.essentialsplus.teleport.TeleportService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Locale;

public final class TpaHereCommand implements CommandExecutor, TabCompleter {
    private static final String PERMISSION = "essentialsplus.tpa";
    private static final String VANISH_PERMISSION = "essentialsplus.vanish";
    private static final String TPA_NOT_FOUND = "§b§lᴛᴘᴀ §8• §cJogador não encontrado.";
    private static final String TPA_VANISHED = "§b§lᴛᴘᴀ §8• §cVocê não pode usar TPA enquanto estiver invisível.";

    private final TeleportService service;
    private final AuthSystemBridge auth = new AuthSystemBridge();
    public TpaHereCommand(TeleportService service) { this.service = service; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Este comando só pode ser usado por jogadores."); return true; }
        if (args.length != 1) { player.sendMessage("§cUso: /tpaqui <jogador>"); return true; }
        if (!player.hasPermission(PERMISSION)) { player.sendMessage("§cVocê não tem permissão para isso."); return true; }
        if (isVanished(player)) { player.sendMessage(TPA_VANISHED); return true; }

        final Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !canTarget(player, target)) { player.sendMessage(TPA_NOT_FOUND); return true; }
        if (!auth.isAuthenticated(target)) { player.sendMessage("§cEsse jogador ainda não está autenticado."); return true; }
        service.request(player, target, true);
        return true;
    }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player) || !player.isOnline() || !player.hasPermission(PERMISSION) || args.length != 1 || isVanished(player)) return List.of();
        final String prefix = args[0].toLowerCase(Locale.ROOT);
        return Bukkit.getOnlinePlayers().stream()
                .filter(target -> canTarget(player, target))
                .map(Player::getName)
                .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix))
                .sorted()
                .limit(20)
                .toList();
    }

    private boolean canTarget(Player requester, Player target) {
        if (target == null || !target.isOnline()) return false;
        if (requester.equals(target)) return true;
        return requester.hasPermission(VANISH_PERMISSION) || !isVanished(target);
    }

    private boolean isVanished(Player player) {
        try {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("EssentialsPlus");
            return plugin instanceof EssentialsPlus essentials && essentials.isVanished(player);
        } catch (LinkageError ignored) {
            return false;
        }
    }
}
