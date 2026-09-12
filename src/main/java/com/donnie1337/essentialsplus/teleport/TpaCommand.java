package com.donnie1337.essentialsplus.teleport.command;

import com.donnie1337.essentialsplus.EssentialsPlus;
import com.donnie1337.essentialsplus.auth.AuthSystemBridge;
import com.donnie1337.essentialsplus.teleport.TeleportService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

public final class TpaCommand implements CommandExecutor, TabCompleter {
    private static final String VANISH_PERMISSION = "essentialsplus.vanish";
    private static final String TPA_NOT_FOUND = "§b§lᴛᴘᴀ §8• §cJogador não encontrado.";
    private static final String TPA_VANISHED = "§b§lᴛᴘᴀ §8• §cVocê não pode usar TPA enquanto estiver invisível.";

    private final TeleportService service;
    private final AuthSystemBridge auth = new AuthSystemBridge();
    public TpaCommand(TeleportService service) { this.service = service; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Este comando só pode ser usado por jogadores."); return true; }
        if (args.length != 1) { player.sendMessage(tpaUsage()); return true; }
        if (!player.hasPermission("essentialsplus.tpa")) { player.sendMessage("§e§lᴛᴘᴀ §8• §rComando não encontrado."); return true; }
        if (isRestrictedByVanish(player)) { player.sendMessage(TPA_VANISHED); return true; }

        final Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !canTarget(player, target)) { player.sendMessage(TPA_NOT_FOUND); return true; }
        if (!auth.isAuthenticated(target)) { player.sendMessage("§b§lᴛᴘᴀ §8• §cEsse jogador ainda não está autenticado."); return true; }
        service.request(player, target, false);
        return true;
    }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player) || !player.isOnline()) return List.of();
        if (args.length != 1) return List.of();
        if (!player.hasPermission("essentialsplus.tpa") || isRestrictedByVanish(player)) return List.of();
        return Bukkit.getOnlinePlayers().stream()
                .filter(target -> canTarget(player, target) && !target.equals(player))
                .map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                .sorted().toList();
    }

    private String tpaUsage() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("EssentialsPlus");
        String prefix = "&b&lᴛᴘᴀ &8• &r";
        String message = "Use /tpa <jogador>.";
        if (plugin instanceof EssentialsPlus essentials) {
            prefix = essentials.getConfig().getString("messages.tpa.prefix", prefix);
            message = essentials.getConfig().getString("messages.tpa.uso.tpa", message);
        }
        return ChatColor.translateAlternateColorCodes('&', prefix + message);
    }

    private boolean canTarget(Player requester, Player target) {
        if (target == null || !target.isOnline()) return false;
        if (requester.equals(target)) return false;
        return requester.hasPermission(VANISH_PERMISSION) || !isVanished(target);
    }

    private boolean isRestrictedByVanish(Player player) {
        return isVanished(player);
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
