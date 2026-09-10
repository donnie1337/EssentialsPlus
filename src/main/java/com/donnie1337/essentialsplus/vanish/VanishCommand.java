package com.donnie1337.essentialsplus.vanish;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public final class VanishCommand implements CommandExecutor, TabCompleter {
    private final VanishService service;

    public VanishCommand(VanishService service) {
        this.service = service;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores.");
            return true;
        }
        if (!player.hasPermission("essentialsplus.vanish")) {
            player.sendMessage(ChatColor.RED + "Você não tem permissão para executar este comando.");
            return true;
        }
        if (args.length != 0) {
            player.sendMessage(ChatColor.RED + "Uso: /v");
            return true;
        }

        service.toggle(player);
        boolean enabled = service.isVanished(player);
        player.sendMessage(ChatColor.GRAY + "Vanish: " + (enabled ? ChatColor.GREEN + "ativado" : ChatColor.RED + "desativado") + ChatColor.GRAY + ".");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
