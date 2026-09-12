package com.donnie1337.essentialsplus.chat;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class TellCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(TellListener.message("messages.tell.player-only"));
            return true;
        }
        return TellListener.handleTellCommand(player, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player) || !player.hasPermission("essentialsplus.tell")) {
            return List.of();
        }
        if (args.length != 1) {
            return List.of();
        }

        String prefix = args[0].toLowerCase();
        List<String> suggestions = new ArrayList<>();
        for (Player online : player.getServer().getOnlinePlayers()) {
            if (online.getName().toLowerCase().startsWith(prefix)) {
                suggestions.add(online.getName());
            }
        }
        return suggestions;
    }
}
