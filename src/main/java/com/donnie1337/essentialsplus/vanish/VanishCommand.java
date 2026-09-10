package com.donnie1337.essentialsplus.vanish;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public final class VanishCommand implements CommandExecutor, TabCompleter {
    private static final String VANISH_PERMISSION = "essentialsplus.vanish";

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
        if (!player.hasPermission(VANISH_PERMISSION)) {
            player.sendMessage(ChatColor.YELLOW + "§lᴄʜᴀᴛ §8• §rComando não encontrado.");
            return true;
        }
        if (args.length != 0) {
            player.sendMessage(ChatColor.RED + "Uso: /v");
            return true;
        }

        service.toggle(player);
        boolean enabled = service.isVanished(player);

        if (enabled) {
            player.sendMessage(ChatColor.RED + "§lᴠᴀɴɪsʜ §8• §rVocê ficou invisível para outros jogadores.");
        } else {
            player.sendMessage(ChatColor.GREEN + "§lᴠᴀɴɪsʜ §8• §rVocê voltou a ficar visível para outros jogadores.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
